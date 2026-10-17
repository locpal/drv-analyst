package ru.sewaiper.derivative.analyst.dao;

import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ta4j.core.num.DecimalNumFactory;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.sqlite.SQLiteDataSource;
import tools.jackson.databind.json.JsonMapper;
import ru.sewaiper.derivative.analyst.domain.analysis.AnalysisStatus;
import ru.sewaiper.derivative.analyst.domain.analysis.MarketHypothesis;
import ru.sewaiper.derivative.analyst.domain.analysis.SkippableStep;
import ru.sewaiper.derivative.analyst.domain.llm.ChatCompletionRequest;
import ru.sewaiper.derivative.analyst.domain.llm.ChatCompletionResponse;
import ru.sewaiper.derivative.analyst.domain.market.InstrumentLocator;
import ru.sewaiper.derivative.analyst.domain.market.enums.AssetType;
import ru.sewaiper.derivative.analyst.domain.market.enums.Provider;
import ru.sewaiper.derivative.analyst.domain.market.enums.Timeframe;
import ru.sewaiper.derivative.analyst.domain.market.tech.AtrMetrics;
import ru.sewaiper.derivative.analyst.domain.market.tech.MetricName;
import ru.sewaiper.derivative.analyst.domain.schedule.TradingHorizon;

import java.nio.file.Path;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AnalysisRunDaoTest {
    @TempDir Path tempDir;

    @Test
    void persistsPreparationAndMarksInterruptedRunsAfterRestart() throws Exception {
        var dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + tempDir.resolve("runs.sqlite"));
        var liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setResourceLoader(new DefaultResourceLoader());
        liquibase.setChangeLog("classpath:migration/changelog.xml");
        liquibase.afterPropertiesSet();
        var dao = new AnalysisRunDao(new JdbcTemplate(dataSource));
        var stepDao = new AnalysisStepDao(new JdbcTemplate(dataSource));
        var forecastDao = new ForecastDao(new JdbcTemplate(dataSource));
        var locator = new InstrumentLocator("uid-1", AssetType.FUTURE, Provider.T_INVEST);
        var createdAt = Instant.parse("2026-10-16T10:00:00Z");
        var snapshotAt = Instant.parse("2026-10-16T10:01:00Z");
        var target = new TradingHorizon(1, LocalDate.parse("2026-10-19"),
                Instant.parse("2026-10-19T15:00:00Z"));
        UUID completedId = UUID.randomUUID();
        UUID interruptedId = UUID.randomUUID();
        UUID metricsOnlyId = UUID.randomUUID();
        dao.create(completedId, locator, Set.of(), createdAt);
        long fetchStep = stepDao.start(completedId, "FETCH_SNAPSHOT",
                Map.of("locator", locator, "timeframe", Timeframe.H1));
        stepDao.complete(fetchStep, Map.of("snapshotId", completedId, "snapshotAtUtc", snapshotAt));
        long failedStep = stepDao.start(completedId, "RESOLVE_HORIZONS", Map.of("snapshotId", completedId));
        stepDao.fail(failedStep, new IllegalStateException("schedule unavailable"));
        dao.markPreparing(completedId, createdAt.plusSeconds(1));
        var nums = DecimalNumFactory.getInstance();
        var atr = new AtrMetrics(nums.numOf("39.24084022700896"), nums.numOf("0.2723167260722343"),
                28, 200, snapshotAt);
        dao.markPrepared(completedId, snapshotAt, snapshotAt, "forts_futures", List.of(target),
                Map.of(), Map.of(Timeframe.H1, Map.of(MetricName.ATR, atr)), true);
        var request = new ChatCompletionRequest("tested-model",
                List.of(new ChatCompletionRequest.Message("user", "facts")), false,
                100, 0.0, null, null, null, null);
        forecastDao.saveRequest(completedId, request, snapshotAt.plusSeconds(1));
        var content = """
                {"hypotheses": [{"tradingDays": 1, "lowerBound": "100", "upperBound": "110"}]}
                """;
        var response = new ChatCompletionResponse("response-1", "chat.completion", 1L,
                "returned-model", null, List.of(new ChatCompletionResponse.Choice(0,
                new ChatCompletionResponse.Message("assistant", content, null, null, null),
                "stop", null, null)), new ChatCompletionResponse.Usage(11L, 22L, 33L,
                null, null, null, null, null, null));
        forecastDao.saveResponse(completedId, response, snapshotAt.plusSeconds(2));
        forecastDao.saveValidation(completedId, JsonMapper.builder().build().readTree(
                "{\"status\":\"VALID\",\"referencePrice\":\"100\"}"));
        forecastDao.saveHypotheses(completedId, List.of(
                new MarketHypothesis(1, "TERMINAL_PRICE_RANGE", new BigDecimal("100"),
                        new BigDecimal("110"), "reason", "alternative", List.of("risk")),
                new MarketHypothesis(5, "TERMINAL_PRICE_RANGE", new BigDecimal("90"),
                        new BigDecimal("120"), "reason", "alternative", List.of()),
                new MarketHypothesis(10, "TERMINAL_PRICE_RANGE", new BigDecimal("80"),
                        new BigDecimal("130"), "reason", "alternative", List.of())), snapshotAt.plusSeconds(3));
        dao.create(interruptedId, locator, Set.of(), createdAt);
        var skipped = Set.of(SkippableStep.MARKET_TICK, SkippableStep.ORDER_BOOK, SkippableStep.FORECAST);
        dao.create(metricsOnlyId, locator, skipped, createdAt);
        dao.markPreparing(metricsOnlyId, createdAt.plusSeconds(1));
        dao.markPrepared(metricsOnlyId, snapshotAt, snapshotAt, "forts_futures", List.of(target),
                Map.of(), Map.of(Timeframe.H1, Map.of(MetricName.ATR, atr)), false);
        stepDao.start(interruptedId, "FETCH_SNAPSHOT", Map.of("providerId", "uid-1"));
        stepDao.failInterruptedSteps(createdAt.plusSeconds(100));
        dao.failInterruptedRuns(createdAt.plusSeconds(100));

        var prepared = dao.find(completedId).orElseThrow();
        assertEquals(AnalysisStatus.FORECASTED, prepared.status());
        assertEquals(Set.of(), prepared.skip());
        assertEquals(snapshotAt, prepared.snapshotAtUtc());
        assertEquals(List.of(target), prepared.horizons());
        assertEquals("39.24084022700896", prepared.metrics().get("H1").get("ATR").get("atrPts").asText());
        assertEquals(28, prepared.metrics().get("H1").get("ATR").get("period").asInt());
        assertEquals(2, new JdbcTemplate(dataSource).queryForObject(
                "SELECT format_version FROM analysis_metrics WHERE analysis_id = ?", Integer.class,
                completedId.toString()));
        var exchange = forecastDao.find(completedId).orElseThrow();
        assertEquals("tested-model", exchange.request().get("model").asText());
        assertEquals("returned-model", exchange.model());
        assertEquals("stop", exchange.finishReason());
        assertEquals(33L, exchange.usage().get("total_tokens").asLong());
        assertEquals(content, exchange.content());
        assertEquals("VALID", exchange.validation().get("status").asText());
        assertEquals(3, exchange.hypotheses().size());
        assertThrows(IllegalStateException.class, () -> dao.markPrepared(completedId,
                snapshotAt.plusSeconds(60), snapshotAt, "forts_futures", List.of(target),
                Map.of(), Map.of(Timeframe.H1, Map.of(MetricName.ATR, atr)), true));
        assertEquals(1, new JdbcTemplate(dataSource).queryForObject(
                "SELECT COUNT(*) FROM analysis_metrics WHERE analysis_id = ?", Integer.class,
                completedId.toString()));
        assertEquals(AnalysisStatus.FAILED, dao.find(interruptedId).orElseThrow().status());
        assertEquals(AnalysisStatus.PREPARED, dao.find(metricsOnlyId).orElseThrow().status());
        assertEquals(skipped, dao.find(metricsOnlyId).orElseThrow().skip());
        assertNull(dao.find(interruptedId).orElseThrow().metrics());
        assertTrue(dao.find(UUID.randomUUID()).isEmpty());
        var completedStep = stepDao.findByAnalysisId(completedId).getFirst();
        assertEquals("COMPLETED", completedStep.status());
        assertEquals("uid-1", completedStep.input().get("locator").get("providerId").asText());
        assertEquals("H1", completedStep.input().get("timeframe").asText());
        assertEquals(completedId.toString(), completedStep.output().get("snapshotId").asText());
        assertEquals(snapshotAt.toString(), completedStep.output().get("snapshotAtUtc").asText());
        assertEquals("FAILED", stepDao.findByAnalysisId(completedId).get(1).status());
        assertEquals("IllegalStateException: schedule unavailable",
                stepDao.findByAnalysisId(completedId).get(1).errorMessage());
        assertEquals("FAILED", stepDao.findByAnalysisId(interruptedId).getFirst().status());
    }
}
