package ru.sewaiper.derivative.analyst.service;


import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import ru.sewaiper.derivative.analyst.dao.MarketSnapshotDao;
import ru.sewaiper.derivative.analyst.dao.AnalysisMetricJson;
import ru.sewaiper.derivative.analyst.domain.analysis.AnalysisPreparation;
import ru.sewaiper.derivative.analyst.domain.market.InstrumentLocator;
import ru.sewaiper.derivative.analyst.domain.market.MarketContext;
import ru.sewaiper.derivative.analyst.domain.market.SnapshotDepth;
import ru.sewaiper.derivative.analyst.domain.market.snapshot.InstrumentSnapshot;
import ru.sewaiper.derivative.analyst.domain.schedule.TradingHorizon;
import ru.sewaiper.derivative.analyst.service.analyzer.IndicatorAnalysisService;
import ru.sewaiper.derivative.analyst.service.market.MarketDataProvider;
import ru.sewaiper.derivative.analyst.service.schedule.TradingHorizonService;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class AnalysisCoordinator {

    private final MarketDataProvider marketDataProvider;
    private final IndicatorAnalysisService indicatorAnalysisService;
    private final TradingHorizonService tradingHorizonService;
    private final MarketSnapshotDao marketSnapshotDao;
    private final AnalysisStepJournal journal;

    public AnalysisCoordinator(MarketDataProvider marketDataProvider,
                               IndicatorAnalysisService indicatorAnalysisService,
                               TradingHorizonService tradingHorizonService,
                               MarketSnapshotDao marketSnapshotDao,
                               AnalysisStepJournal journal) {
        this.marketDataProvider = Objects.requireNonNull(marketDataProvider, "marketDataProvider");
        this.indicatorAnalysisService = Objects.requireNonNull(indicatorAnalysisService, "indicatorAnalysisService");
        this.tradingHorizonService = Objects.requireNonNull(tradingHorizonService, "tradingHorizonService");
        this.marketSnapshotDao = Objects.requireNonNull(marketSnapshotDao, "marketSnapshotDao");
        this.journal = Objects.requireNonNull(journal, "journal");
    }

    public AnalysisPreparation prepare(@NonNull UUID analysisId,
                                       @NonNull InstrumentLocator locator,
                                       @NonNull SnapshotDepth depth) {
        InstrumentSnapshot snapshot = fetchSnapshot(analysisId, locator, depth);
        saveSnapshot(analysisId, snapshot);
        MarketContext context =
                calculateIndicators(analysisId, snapshot, depth);
        List<TradingHorizon> horizons = resolveHorizons(analysisId, snapshot);
        return new AnalysisPreparation(locator, depth, context, horizons);
    }

    private InstrumentSnapshot fetchSnapshot(UUID analysisId, InstrumentLocator locator, SnapshotDepth depth) {
        return journal.run(
                analysisId,
                "FETCH_SNAPSHOT",
                Map.of("locator", locator, "depth", depth),
                () -> marketDataProvider.getSnapshot(locator, depth),
                this::snapshotSummary
        );
    }

    private Map<String, Object> snapshotSummary(InstrumentSnapshot snapshot) {
        return Map.of(
                "ticker", snapshot.ticker(),
                "snapshotAtUtc", snapshot.snapshotTimeUtc(),
                "providerId", snapshot.passport().providerId()
        );
    }

    private void saveSnapshot(UUID analysisId, InstrumentSnapshot snapshot) {
        journal.run(
                analysisId,
                "SAVE_SNAPSHOT",
                Map.of("snapshotAtUtc", snapshot.snapshotTimeUtc()),
                () -> marketSnapshotDao.save(analysisId, snapshot),
                Map.of("snapshotId", analysisId)
        );
    }

    private MarketContext calculateIndicators(UUID analysisId, InstrumentSnapshot snapshot, SnapshotDepth depth) {
        return journal.run(
                analysisId,
                "CALCULATE_INDICATORS",
                Map.of("snapshotId", analysisId, "timeframes", List.of("H1", "D1")),
                () -> analyzeSnapshot(snapshot, depth),
                this::metricSummary
        );
    }

    private MarketContext analyzeSnapshot(InstrumentSnapshot snapshot, SnapshotDepth depth) {
        MarketContext context = indicatorAnalysisService.analyze(snapshot, depth.skip());
        if (context.getSnapshot() != snapshot) {
            throw new IllegalStateException("indicatorAnalysisService returned a different snapshot");
        }
        return context;
    }

    private Map<String, Object> metricSummary(MarketContext context) {
        Map<String, Object> metrics = new java.util.LinkedHashMap<>();
        if (!context.getMetrics().isEmpty()) {
            metrics.put("COMMON", AnalysisMetricJson.decode(AnalysisMetricJson.encode(context.getMetrics())));
        }
        context.getTimeframeMetrics().forEach((timeframe, values) -> {
            if (!values.isEmpty()) {
                metrics.put(timeframe.name(), AnalysisMetricJson.decode(AnalysisMetricJson.encode(values)));
            }
        });
        return Map.of("metrics", metrics);
    }

    private List<TradingHorizon> resolveHorizons(UUID analysisId, InstrumentSnapshot snapshot) {
        return journal.run(
                analysisId,
                "RESOLVE_HORIZONS",
                Map.of("snapshotId", analysisId),
                () -> calculateHorizons(snapshot),
                this::horizonSummary
        );
    }

    private List<TradingHorizon> calculateHorizons(InstrumentSnapshot snapshot) {
        Objects.requireNonNull(snapshot.snapshotTimeUtc(), "snapshotTimeUtc");
        String exchange = Objects.requireNonNull(snapshot.passport(), "snapshot passport").exchange();
        if (exchange == null || exchange.isBlank()) {
            throw new IllegalStateException("Instrument exchange is unavailable");
        }
        return tradingHorizonService.resolve(exchange, snapshot.snapshotTimeUtc());
    }

    private Map<String, Object> horizonSummary(List<TradingHorizon> horizons) {
        return Map.of("horizons", horizons.stream().map(horizon -> Map.of(
                        "tradingDays", horizon.tradingDays(),
                        "sessionDate", horizon.sessionDate().toString(),
                        "targetAtUtc", horizon.targetAtUtc().toString())).toList());
    }
}
