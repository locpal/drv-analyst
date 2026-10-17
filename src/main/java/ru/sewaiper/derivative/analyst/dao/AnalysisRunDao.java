package ru.sewaiper.derivative.analyst.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.sewaiper.derivative.analyst.domain.analysis.AnalysisRun;
import ru.sewaiper.derivative.analyst.domain.analysis.AnalysisStatus;
import ru.sewaiper.derivative.analyst.domain.analysis.SkippableStep;
import ru.sewaiper.derivative.analyst.domain.market.InstrumentLocator;
import ru.sewaiper.derivative.analyst.domain.market.enums.AssetType;
import ru.sewaiper.derivative.analyst.domain.market.enums.Provider;
import ru.sewaiper.derivative.analyst.domain.market.enums.Timeframe;
import ru.sewaiper.derivative.analyst.domain.market.tech.MetricName;
import ru.sewaiper.derivative.analyst.domain.market.tech.TechMetrics;
import ru.sewaiper.derivative.analyst.domain.schedule.TradingHorizon;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Set;
import java.util.EnumSet;
import tools.jackson.databind.json.JsonMapper;

@Repository
public class AnalysisRunDao {
    private static final int METRIC_FORMAT_VERSION = 2;
    private static final String CALCULATION_VERSION = "centaur-v1/ta4j-0.22.6";
    private static final String METHODOLOGY_VERSION = "h1-d1-v1";
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private final JdbcTemplate jdbc;

    public AnalysisRunDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void create(UUID id, InstrumentLocator locator, Set<SkippableStep> skip, Instant createdAt) {
        jdbc.update("""
                INSERT INTO analysis_runs (id, provider, asset_type, provider_id, methodology_version, skip_json,
                    status, created_at_utc)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, id.toString(), locator.provider().name(), locator.type().name(), locator.providerId(),
                METHODOLOGY_VERSION, JSON.writeValueAsString(skip.stream().map(Enum::name).sorted().toList()),
                AnalysisStatus.CREATED.name(), createdAt.toString());
    }

    public void markPreparing(UUID id, Instant startedAt) {
        updateStatus("UPDATE analysis_runs SET status = ?, started_at_utc = ? WHERE id = ? AND status = 'CREATED'",
                AnalysisStatus.PREPARING.name(), startedAt.toString(), id.toString());
    }

    @Transactional
    public void markPrepared(UUID id, Instant finishedAt, Instant snapshotAt, String exchange,
                             List<TradingHorizon> horizons, Map<MetricName, TechMetrics> commonMetrics,
                             Map<Timeframe, Map<MetricName, TechMetrics>> timeframeMetrics,
                             boolean forecastPlanned) {
        updateStatus("""
                UPDATE analysis_runs SET status = ?, finished_at_utc = ?, snapshot_at_utc = ?, exchange = ?
                WHERE id = ? AND status = 'PREPARING'
                """, forecastPlanned ? AnalysisStatus.FORECASTING.name() : AnalysisStatus.PREPARED.name(),
                forecastPlanned ? null : finishedAt.toString(), snapshotAt.toString(), exchange,
                id.toString());
        for (TradingHorizon horizon : horizons) {
            jdbc.update("""
                    INSERT INTO analysis_run_horizons (analysis_id, trading_days, trading_date, target_at_utc)
                    VALUES (?, ?, ?, ?)
                    """, id.toString(), horizon.tradingDays(), horizon.sessionDate().toString(),
                    horizon.targetAtUtc().toString());
        }
        saveMetrics(id, "COMMON", commonMetrics, finishedAt);
        timeframeMetrics.forEach((timeframe, metrics) -> saveMetrics(id, timeframe.name(), metrics, finishedAt));
    }

    private void saveMetrics(UUID id, String scope, Map<MetricName, TechMetrics> metrics, Instant calculatedAt) {
        if (metrics.isEmpty()) {
            return;
        }
        jdbc.update("""
                INSERT INTO analysis_metrics (analysis_id, scope, format_version, calculation_version,
                    calculated_at_utc, metrics_json)
                VALUES (?, ?, ?, ?, ?, ?)
                """, id.toString(), scope, METRIC_FORMAT_VERSION, CALCULATION_VERSION,
                calculatedAt.toString(), AnalysisMetricJson.encode(metrics));
    }

    public void markFailed(UUID id, Instant finishedAt, String message) {
        updateStatus("""
                UPDATE analysis_runs SET status = ?, finished_at_utc = ?, error_message = ?
                WHERE id = ? AND status IN ('CREATED', 'PREPARING', 'PREPARED', 'FORECASTING')
                """, AnalysisStatus.FAILED.name(), finishedAt.toString(), message, id.toString());
    }

    public void failInterruptedRuns(Instant finishedAt) {
        jdbc.update("""
                UPDATE analysis_runs SET status = 'FAILED', finished_at_utc = ?,
                    error_message = 'Analysis interrupted by application restart'
                WHERE status IN ('CREATED', 'PREPARING', 'FORECASTING')
                """, finishedAt.toString());
    }

    public Optional<AnalysisRun> find(UUID id) {
        List<AnalysisRun> runs = jdbc.query("SELECT * FROM analysis_runs WHERE id = ?", this::mapRun, id.toString());
        return runs.stream().findFirst().map(run -> new AnalysisRun(run.id(), run.locator(), run.methodologyVersion(),
                run.skip(), run.status(), run.createdAtUtc(), run.startedAtUtc(), run.finishedAtUtc(), run.snapshotAtUtc(),
                run.exchange(), jdbc.query("""
                        SELECT trading_days, trading_date, target_at_utc FROM analysis_run_horizons
                        WHERE analysis_id = ? ORDER BY trading_days
                        """, (rs, row) -> new TradingHorizon(rs.getInt("trading_days"),
                        LocalDate.parse(rs.getString("trading_date")),
                        Instant.parse(rs.getString("target_at_utc"))), id.toString()),
                readMetrics(id), run.errorMessage()));
    }

    private tools.jackson.databind.JsonNode readMetrics(UUID id) {
        Map<String, tools.jackson.databind.JsonNode> byScope = new java.util.LinkedHashMap<>();
        jdbc.query("SELECT scope, metrics_json FROM analysis_metrics WHERE analysis_id = ? ORDER BY scope",
                        (rs, row) -> Map.entry(rs.getString("scope"),
                                AnalysisMetricJson.decode(rs.getString("metrics_json"))), id.toString())
                .forEach(entry -> byScope.put(entry.getKey(), entry.getValue()));
        return byScope.isEmpty() ? null : JSON.readTree(JSON.writeValueAsString(byScope));
    }

    private AnalysisRun mapRun(ResultSet rs, int row) throws SQLException {
        return new AnalysisRun(UUID.fromString(rs.getString("id")),
                new InstrumentLocator(rs.getString("provider_id"),
                        AssetType.valueOf(rs.getString("asset_type")), Provider.valueOf(rs.getString("provider"))),
                rs.getString("methodology_version"), readSkip(rs.getString("skip_json")),
                AnalysisStatus.valueOf(rs.getString("status")),
                Instant.parse(rs.getString("created_at_utc")), parseInstant(rs.getString("started_at_utc")),
                parseInstant(rs.getString("finished_at_utc")), parseInstant(rs.getString("snapshot_at_utc")),
                rs.getString("exchange"), List.of(), null, rs.getString("error_message"));
    }

    private Instant parseInstant(String value) {
        return value == null ? null : Instant.parse(value);
    }

    private Set<SkippableStep> readSkip(String json) {
        EnumSet<SkippableStep> skip = EnumSet.noneOf(SkippableStep.class);
        for (var name : JSON.readTree(json)) {
            skip.add(SkippableStep.valueOf(name.asString()));
        }
        return Set.copyOf(skip);
    }

    private void updateStatus(String sql, Object... args) {
        if (jdbc.update(sql, args) != 1) {
            throw new IllegalStateException("Analysis run is missing or has an unexpected status");
        }
    }
}
