package ru.sewaiper.derivative.analyst.service.analyzer;

import org.junit.jupiter.api.Test;
import ru.sewaiper.derivative.analyst.domain.market.MarketContext;
import ru.sewaiper.derivative.analyst.domain.market.enums.Timeframe;
import ru.sewaiper.derivative.analyst.domain.market.snapshot.TInvestCandleData;
import ru.sewaiper.derivative.analyst.domain.market.tech.EmaMetrics;
import ru.sewaiper.derivative.analyst.domain.market.tech.MetricName;
import ru.sewaiper.derivative.analyst.domain.tinvest.candles.Candle;
import ru.sewaiper.derivative.analyst.domain.tinvest.candles.GetCandlesResponse;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class EmaAnalyzerRealCandlesTest {
    @Test
    void calculatesEmaFromTheSame200ClosedHourlyCandlesAsAtr() throws IOException {
        GetCandlesResponse response;
        try (var stream = getClass().getResourceAsStream("/candles.json")) {
            assertNotNull(stream, "Missing /candles.json test fixture");
            response = JsonMapper.builder().build().readValue(stream, GetCandlesResponse.class);
        }
        var closed = response.candles().stream().filter(Candle::isComplete).toList();
        var series = new TInvestCandleData(Timeframe.H1,
                closed.subList(closed.size() - 200, closed.size())).toBarSeries();
        var context = new MarketContext(null, series);

        new EmaAnalyzer().analyze(context);

        var metrics = (EmaMetrics) context.getMetrics().get(MetricName.EMA);
        assertNotNull(metrics);
        assertEquals(14470.85906725826, metrics.ema20().doubleValue(), 1e-6);
        assertEquals(14495.232279860136, metrics.ema50().doubleValue(), 1e-6);
        assertTrue(metrics.spreadPts().isNegative());
        assertTrue(metrics.closeToEma20Pts().isNegative());
        assertEquals(Instant.parse("2026-10-16T19:00:00Z"), metrics.lastCompleteCandleTimeUtc());
    }
}
