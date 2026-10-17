package ru.sewaiper.derivative.analyst.service.analyzer;

import org.junit.jupiter.api.Test;
import ru.sewaiper.derivative.analyst.domain.market.MarketContext;
import ru.sewaiper.derivative.analyst.domain.market.enums.Timeframe;
import ru.sewaiper.derivative.analyst.domain.market.snapshot.TInvestCandleData;
import ru.sewaiper.derivative.analyst.domain.market.tech.AtrMetrics;
import ru.sewaiper.derivative.analyst.domain.market.tech.MetricName;
import ru.sewaiper.derivative.analyst.domain.tinvest.candles.Candle;
import ru.sewaiper.derivative.analyst.domain.tinvest.candles.GetCandlesResponse;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AtrAnalyzerRealCandlesTest {
    private static final int CANDLE_COUNT = 200;

    @Test
    void selectsLast200ClosedCandlesAndPreservesThemThroughSerialization() throws IOException {
        var response = loadCandles();
        var closed = response.candles().stream().filter(Candle::isComplete).toList();
        var data = last200Closed(response);
        var restored = TInvestCandleData.restore(data.serialize());
        var series = restored.toBarSeries();

        assertEquals(478, response.candles().size());
        assertEquals(477, closed.size());
        assertFalse(response.candles().getLast().isComplete());
        assertEquals(data, restored);
        assertEquals(CANDLE_COUNT, series.getBarCount());
        assertEquals(Instant.parse("2026-10-02T06:00:00Z"), series.getFirstBar().getBeginTime());
        assertEquals(Instant.parse("2026-10-16T19:00:00Z"), series.getLastBar().getEndTime());
        assertEquals("14311.000000000", series.getFirstBar().getClosePrice().toString());
        assertEquals("14410.000000000", series.getLastBar().getClosePrice().toString());
        assertEquals("132", series.getLastBar().getVolume().toString());
    }

    @Test
    void calculatesAtrFromRealHourlyCandles() throws IOException {
        var series = last200Closed(loadCandles()).toBarSeries();
        var context = new MarketContext(null, series);

        new AtrAnalyzer().analyze(context);

        var metrics = (AtrMetrics) context.getMetrics().get(MetricName.ATR);
        assertNotNull(metrics);
        assertEquals(28, metrics.period());
        assertEquals(CANDLE_COUNT, metrics.completeCandlesCount());
        assertEquals(Instant.parse("2026-10-16T19:00:00Z"), metrics.lastCompleteCandleTimeUtc());
        assertEquals(39.24084022700896, metrics.atrPts().doubleValue(), 1e-9);
        assertEquals(0.2723167260722343, metrics.atrPct().doubleValue(), 1e-11);
    }

    @Test
    void rejectsTheUnfinishedLastCandleIfItIsPassedToBarSeries() throws IOException {
        var candles = loadCandles().candles();
        var data = new TInvestCandleData(Timeframe.H1,
                candles.subList(candles.size() - CANDLE_COUNT, candles.size()));

        assertThrows(IllegalArgumentException.class, data::toBarSeries);
    }

    private static TInvestCandleData last200Closed(GetCandlesResponse response) {
        List<Candle> closed = response.candles().stream().filter(Candle::isComplete).toList();
        return new TInvestCandleData(Timeframe.H1,
                closed.subList(closed.size() - CANDLE_COUNT, closed.size()));
    }

    private static GetCandlesResponse loadCandles() throws IOException {
        try (var stream = AtrAnalyzerRealCandlesTest.class.getResourceAsStream("/candles.json")) {
            assertNotNull(stream, "Missing /candles.json test fixture");
            return JsonMapper.builder().build().readValue(stream, GetCandlesResponse.class);
        }
    }
}
