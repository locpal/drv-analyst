package ru.sewaiper.derivative.analyst.service.analyzer;

import org.junit.jupiter.api.Test;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.num.DecimalNumFactory;
import ru.sewaiper.derivative.analyst.domain.market.MarketContext;
import ru.sewaiper.derivative.analyst.domain.market.enums.Timeframe;
import ru.sewaiper.derivative.analyst.domain.market.snapshot.TInvestCandleData;
import ru.sewaiper.derivative.analyst.domain.market.tech.MetricName;
import ru.sewaiper.derivative.analyst.domain.market.tech.RsiMetrics;
import ru.sewaiper.derivative.analyst.domain.tinvest.candles.Candle;
import ru.sewaiper.derivative.analyst.domain.tinvest.candles.GetCandlesResponse;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.function.IntUnaryOperator;

import static org.junit.jupiter.api.Assertions.*;

class RsiAnalyzerTest {
    private final RsiAnalyzer analyzer = new RsiAnalyzer();

    @Test
    void handlesOnlyGainsOnlyLossesAndFlatPrices() {
        var rising = analyze(bars(200, i -> 100 + i));
        var falling = analyze(bars(200, i -> 300 - i));
        var flat = analyze(bars(200, i -> 200));

        assertEquals(100.0, rising.value().doubleValue(), 1e-12);
        assertEquals(0.0, falling.value().doubleValue(), 1e-12);
        assertEquals(0.0, flat.value().doubleValue(), 1e-12);
        assertTrue(rising.changePts().isZero());
        assertEquals(14, rising.period());
        assertEquals(5, rising.changeLookback());
        assertEquals(200, rising.completeCandlesCount());
    }

    @Test
    void calculatesRsiAndFiveBarChangeOnRealClosedCandles() throws IOException {
        GetCandlesResponse response;
        try (var stream = getClass().getResourceAsStream("/candles.json")) {
            assertNotNull(stream, "Missing /candles.json test fixture");
            response = JsonMapper.builder().build().readValue(stream, GetCandlesResponse.class);
        }
        var closed = response.candles().stream().filter(Candle::isComplete).toList();
        var series = new TInvestCandleData(Timeframe.H1,
                closed.subList(closed.size() - 200, closed.size())).toBarSeries();

        var metrics = analyze(series);

        assertEquals(40.4989776750219, metrics.value().doubleValue(), 1e-6);
        assertEquals(-11.894398728305525, metrics.changePts().doubleValue(), 1e-6);
        assertEquals(Instant.parse("2026-10-16T19:00:00Z"), metrics.lastCompleteCandleTimeUtc());
    }

    @Test
    void skipsInsufficientHistory() {
        var context = new MarketContext(null, bars(19, i -> 100 + i));

        assertDoesNotThrow(() -> analyzer.analyze(context));
        assertFalse(context.getMetrics().containsKey(MetricName.RSI));
    }

    private RsiMetrics analyze(BarSeries series) {
        var context = new MarketContext(null, series);
        analyzer.analyze(context);
        return (RsiMetrics) context.getMetrics().get(MetricName.RSI);
    }

    private BarSeries bars(int count, IntUnaryOperator closeAt) {
        BarSeries series = new BaseBarSeriesBuilder()
                .withNumFactory(DecimalNumFactory.getInstance())
                .build();
        Instant start = Instant.parse("2026-10-01T00:00:00Z");
        for (int i = 0; i < count; i++) {
            Instant begin = start.plus(Duration.ofHours(i));
            String close = Integer.toString(closeAt.applyAsInt(i));
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
