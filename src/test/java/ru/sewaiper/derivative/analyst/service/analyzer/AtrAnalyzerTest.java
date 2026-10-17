package ru.sewaiper.derivative.analyst.service.analyzer;

import org.junit.jupiter.api.Test;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.num.DecimalNumFactory;
import ru.sewaiper.derivative.analyst.domain.market.MarketContext;
import ru.sewaiper.derivative.analyst.domain.market.tech.AtrMetrics;
import ru.sewaiper.derivative.analyst.domain.market.tech.MetricName;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class AtrAnalyzerTest {
    private final AtrAnalyzer analyzer = new AtrAnalyzer();

    @Test
    void calculatesWilderAtrAndPercentFromTheLastClosedBar() {
        BarSeries series = bars(200, false);
        var context = new MarketContext(null, series);

        analyzer.analyze(context);

        var metrics = (AtrMetrics) context.getMetrics().get(MetricName.ATR);
        assertNotNull(metrics);
        assertEquals(MetricName.ATR, metrics.type());
        assertEquals(28, metrics.period());
        assertEquals(200, metrics.completeCandlesCount());
        assertEquals(series.getLastBar().getEndTime(), metrics.lastCompleteCandleTimeUtc());
        // 199 bars have TR=2; the last has TR=4. MMA(28) advances by 2/28.
        assertEquals(2.0 + 2.0 / 28.0, metrics.atrPts().doubleValue(), 1e-12);
        assertEquals((2.0 + 2.0 / 28.0) / 200.0 * 100.0,
                metrics.atrPct().doubleValue(), 1e-12);
    }

    @Test
    void acceptsZeroAtrForAFlatMarket() {
        var context = new MarketContext(null, bars(200, true));

        analyzer.analyze(context);

        var metrics = (AtrMetrics) context.getMetrics().get(MetricName.ATR);
        assertTrue(metrics.atrPts().isZero());
        assertTrue(metrics.atrPct().isZero());
    }

    @Test
    void includesGapFromThePreviousCloseInTrueRange() {
        var context = new MarketContext(null, bars(200, false, true));

        analyzer.analyze(context);

        var metrics = (AtrMetrics) context.getMetrics().get(MetricName.ATR);
        // The last high-low is 1, but high minus previous close is 10.
        assertEquals(2.0 + 8.0 / 28.0, metrics.atrPts().doubleValue(), 1e-12);
        assertEquals((2.0 + 8.0 / 28.0) / 209.0 * 100.0,
                metrics.atrPct().doubleValue(), 1e-12);
    }

    @Test
    void skipsInsufficientHistory() {
        var context = new MarketContext(null, bars(27, false));
        assertDoesNotThrow(() -> analyzer.analyze(context));
        assertFalse(context.getMetrics().containsKey(MetricName.ATR));
    }

    private BarSeries bars(int count, boolean flat) {
        return bars(count, flat, false);
    }

    private BarSeries bars(int count, boolean flat, boolean gapAtEnd) {
        BarSeries series = new BaseBarSeriesBuilder()
                .withNumFactory(DecimalNumFactory.getInstance())
                .build();
        Instant start = Instant.parse("2026-10-01T00:00:00Z");
        for (int i = 0; i < count; i++) {
            Instant begin = start.plus(Duration.ofHours(i));
            boolean lastGap = gapAtEnd && i == 199;
            String high = lastGap ? "210" : flat ? "200" : i == 199 ? "204" : "201";
            String low = lastGap ? "209" : flat ? "200" : i == 199 ? "200" : "199";
            String close = lastGap ? "209" : "200";
            series.barBuilder()
                    .timePeriod(Duration.ofHours(1))
                    .beginTime(begin)
                    .endTime(begin.plus(Duration.ofHours(1)))
                    .openPrice(close)
                    .highPrice(high)
                    .lowPrice(low)
                    .closePrice(close)
                    .volume("10")
                    .add();
        }
        return series;
    }
}
