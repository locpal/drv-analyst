package ru.sewaiper.derivative.analyst.service.analyzer;

import org.junit.jupiter.api.Test;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.num.DecimalNumFactory;
import ru.sewaiper.derivative.analyst.domain.market.MarketContext;
import ru.sewaiper.derivative.analyst.domain.market.tech.EmaMetrics;
import ru.sewaiper.derivative.analyst.domain.market.tech.MetricName;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class EmaAnalyzerTest {
    private final EmaAnalyzer analyzer = new EmaAnalyzer();

    @Test
    void calculatesBothPeriodsAndDerivedValuesFromTheLastClosedBar() {
        var series = bars(200);
        var context = new MarketContext(null, series);

        analyzer.analyze(context);

        var metrics = (EmaMetrics) context.getMetrics().get(MetricName.EMA);
        assertNotNull(metrics);
        double ema20 = 200.0 + 10.0 * 2.0 / 21.0;
        double ema50 = 200.0 + 10.0 * 2.0 / 51.0;
        assertEquals(ema20, metrics.ema20().doubleValue(), 1e-10);
        assertEquals(ema50, metrics.ema50().doubleValue(), 1e-10);
        assertEquals(ema20 - ema50, metrics.spreadPts().doubleValue(), 1e-10);
        assertEquals((ema20 - ema50) / ema50 * 100, metrics.spreadPct().doubleValue(), 1e-10);
        assertEquals(210 - ema20, metrics.closeToEma20Pts().doubleValue(), 1e-10);
        assertEquals((210 - ema20) / ema20 * 100, metrics.closeToEma20Pct().doubleValue(), 1e-10);
        assertEquals(ema20 - 200, metrics.ema20ChangePts().doubleValue(), 1e-10);
        assertEquals((ema20 - 200) / 200 * 100, metrics.ema20ChangePct().doubleValue(), 1e-10);
        assertEquals(20, metrics.fastPeriod());
        assertEquals(50, metrics.slowPeriod());
        assertEquals(5, metrics.changeLookback());
        assertEquals(200, metrics.completeCandlesCount());
        assertEquals(series.getLastBar().getEndTime(), metrics.lastCompleteCandleTimeUtc());
    }

    @Test
    void skipsInsufficientHistoryWithoutPublishingMetrics() {
        var context = new MarketContext(null, bars(49));

        assertDoesNotThrow(() -> analyzer.analyze(context));
        assertFalse(context.getMetrics().containsKey(MetricName.EMA));
    }

    private BarSeries bars(int count) {
        BarSeries series = new BaseBarSeriesBuilder()
                .withNumFactory(DecimalNumFactory.getInstance())
                .build();
        Instant start = Instant.parse("2026-10-01T00:00:00Z");
        for (int i = 0; i < count; i++) {
            Instant begin = start.plus(Duration.ofHours(i));
            String close = i == 199 ? "210" : "200";
            series.barBuilder()
                    .timePeriod(Duration.ofHours(1))
                    .beginTime(begin)
                    .endTime(begin.plus(Duration.ofHours(1)))
                    .openPrice(close)
                    .highPrice(close)
                    .lowPrice(close)
                    .closePrice(close)
                    .volume("10")
                    .add();
        }
        return series;
    }
}
