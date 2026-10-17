package ru.sewaiper.derivative.analyst.service.analyzer;

import org.junit.jupiter.api.Test;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.num.DecimalNumFactory;
import ru.sewaiper.derivative.analyst.domain.market.MarketContext;
import ru.sewaiper.derivative.analyst.domain.market.enums.Timeframe;
import ru.sewaiper.derivative.analyst.domain.market.snapshot.TInvestCandleData;
import ru.sewaiper.derivative.analyst.domain.market.tech.LevelMetrics;
import ru.sewaiper.derivative.analyst.domain.market.tech.MetricName;
import ru.sewaiper.derivative.analyst.domain.tinvest.candles.Candle;
import ru.sewaiper.derivative.analyst.domain.tinvest.candles.GetCandlesResponse;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class LevelAnalyzerTest {
    private final LevelAnalyzer analyzer = new LevelAnalyzer();

    @Test
    void includesTheFirstBarOfThe50BarWindowAndExcludesOlderExtremes() {
        var series = bars(200);
        var context = new MarketContext(null, series);

        analyzer.analyze(context);

        var metrics = (LevelMetrics) context.getMetrics().get(MetricName.LEVEL);
        assertNotNull(metrics);
        assertEquals(150, metrics.rollingHigh().intValue());
        assertEquals(50, metrics.rollingLow().intValue());
        assertEquals(50, metrics.highAboveClosePts().intValue());
        assertEquals(50, metrics.closeAboveLowPts().intValue());
        assertEquals(50, metrics.highAboveClosePct().intValue());
        assertEquals(50, metrics.closeAboveLowPct().intValue());
        assertEquals(2, metrics.swingLeftBars());
        assertEquals(2, metrics.swingRightBars());
        assertEquals(50, metrics.window());
        assertEquals(200, metrics.completeCandlesCount());
        assertEquals(series.getLastBar().getEndTime(), metrics.lastCompleteCandleTimeUtc());
    }

    @Test
    void calculatesLevelsOnRealClosedCandles() throws IOException {
        GetCandlesResponse response;
        try (var stream = getClass().getResourceAsStream("/candles.json")) {
            assertNotNull(stream, "Missing /candles.json test fixture");
            response = JsonMapper.builder().build().readValue(stream, GetCandlesResponse.class);
        }
        var closed = response.candles().stream().filter(Candle::isComplete).toList();
        var series = new TInvestCandleData(Timeframe.H1,
                closed.subList(closed.size() - 200, closed.size())).toBarSeries();
        var context = new MarketContext(null, series);

        analyzer.analyze(context);

        var metrics = (LevelMetrics) context.getMetrics().get(MetricName.LEVEL);
        assertEquals(14731, metrics.rollingHigh().intValue());
        assertEquals(14324, metrics.rollingLow().intValue());
        assertEquals(321, metrics.highAboveClosePts().intValue());
        assertEquals(86, metrics.closeAboveLowPts().intValue());
        var swingHigh = metrics.confirmedSwingHigh().orElseThrow();
        assertEquals(14572, swingHigh.price().intValue());
        assertEquals(Instant.parse("2026-10-16T12:00:00Z"), swingHigh.extremumCandleEndUtc());
        assertEquals(Instant.parse("2026-10-16T14:00:00Z"), swingHigh.confirmationCandleEndUtc());
        var swingLow = metrics.confirmedSwingLow().orElseThrow();
        assertEquals(14336, swingLow.price().intValue());
        assertEquals(Instant.parse("2026-10-15T19:00:00Z"), swingLow.extremumCandleEndUtc());
        assertEquals(Instant.parse("2026-10-15T21:00:00Z"), swingLow.confirmationCandleEndUtc());
        assertEquals(Instant.parse("2026-10-16T19:00:00Z"), metrics.lastCompleteCandleTimeUtc());
    }

    @Test
    void doesNotPublishAnExtremumUntilTwoRightHandBarsAreClosed() {
        var before = new MarketContext(null, swingBars(200));
        var after = new MarketContext(null, swingBars(201));

        analyzer.analyze(before);
        analyzer.analyze(after);

        var beforeMetrics = (LevelMetrics) before.getMetrics().get(MetricName.LEVEL);
        var afterMetrics = (LevelMetrics) after.getMetrics().get(MetricName.LEVEL);
        assertEquals(130, beforeMetrics.rollingHigh().intValue());
        assertEquals(120, beforeMetrics.confirmedSwingHigh().orElseThrow().price().intValue());
        assertEquals(130, afterMetrics.confirmedSwingHigh().orElseThrow().price().intValue());
        assertEquals(after.getBarSeries().getLastBar().getEndTime(),
                afterMetrics.confirmedSwingHigh().orElseThrow().confirmationCandleEndUtc());
    }

    @Test
    void doesNotTreatAFlatPlateauAsAConfirmedSwing() {
        var context = new MarketContext(null, barsWithTiedHighs());

        analyzer.analyze(context);

        var metrics = (LevelMetrics) context.getMetrics().get(MetricName.LEVEL);
        assertTrue(metrics.confirmedSwingHigh().isEmpty());
    }

    @Test
    void skipsInsufficientHistoryWithoutPublishingMetrics() {
        var context = new MarketContext(null, bars(49));

        assertDoesNotThrow(() -> analyzer.analyze(context));
        assertFalse(context.getMetrics().containsKey(MetricName.LEVEL));
    }

    private BarSeries bars(int count) {
        BarSeries series = new BaseBarSeriesBuilder()
                .withNumFactory(DecimalNumFactory.getInstance())
                .build();
        Instant start = Instant.parse("2026-10-01T00:00:00Z");
        for (int i = 0; i < count; i++) {
            Instant begin = start.plus(Duration.ofHours(i));
            String high = i == 149 ? "500" : i == 150 ? "150" : "101";
            String low = i == 149 ? "1" : i == 150 ? "50" : "99";
            series.barBuilder()
                    .timePeriod(Duration.ofHours(1))
                    .beginTime(begin)
                    .endTime(begin.plus(Duration.ofHours(1)))
                    .openPrice("100")
                    .highPrice(high)
                    .lowPrice(low)
                    .closePrice("100")
                    .volume("10")
                    .add();
        }
        return series;
    }

    private BarSeries swingBars(int count) {
        BarSeries series = new BaseBarSeriesBuilder()
                .withNumFactory(DecimalNumFactory.getInstance())
                .build();
        Instant start = Instant.parse("2026-10-01T00:00:00Z");
        for (int i = 0; i < count; i++) {
            Instant begin = start.plus(Duration.ofHours(i));
            String high = i == 195 ? "120" : i == 198 ? "130" : "100";
            String low = i == 194 ? "70" : "90";
            series.barBuilder()
                    .timePeriod(Duration.ofHours(1))
                    .beginTime(begin)
                    .endTime(begin.plus(Duration.ofHours(1)))
                    .openPrice("95")
                    .highPrice(high)
                    .lowPrice(low)
                    .closePrice("95")
                    .volume("10")
                    .add();
        }
        return series;
    }

    private BarSeries barsWithTiedHighs() {
        BarSeries series = new BaseBarSeriesBuilder()
                .withNumFactory(DecimalNumFactory.getInstance())
                .build();
        Instant start = Instant.parse("2026-10-01T00:00:00Z");
        for (int i = 0; i < 200; i++) {
            Instant begin = start.plus(Duration.ofHours(i));
            String high = i == 195 || i == 196 ? "120" : "100";
            series.barBuilder()
                    .timePeriod(Duration.ofHours(1))
                    .beginTime(begin)
                    .endTime(begin.plus(Duration.ofHours(1)))
                    .openPrice("95")
                    .highPrice(high)
                    .lowPrice("90")
                    .closePrice("95")
                    .volume("10")
                    .add();
        }
        return series;
    }
}
