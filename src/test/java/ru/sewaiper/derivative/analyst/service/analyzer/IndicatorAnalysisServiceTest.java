package ru.sewaiper.derivative.analyst.service.analyzer;

import org.junit.jupiter.api.Test;
import ru.sewaiper.derivative.analyst.domain.analysis.SkippableStep;
import ru.sewaiper.derivative.analyst.domain.market.snapshot.InstrumentSnapshot;
import ru.sewaiper.derivative.analyst.domain.market.snapshot.TInvestCandleData;
import ru.sewaiper.derivative.analyst.domain.market.enums.Timeframe;
import ru.sewaiper.derivative.analyst.domain.market.tech.MetricName;
import ru.sewaiper.derivative.analyst.domain.market.tech.TechMetrics;
import ru.sewaiper.derivative.analyst.domain.tinvest.Quotation;
import ru.sewaiper.derivative.analyst.domain.tinvest.candles.Candle;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class IndicatorAnalysisServiceTest {

    @Test
    void runsTechnicalAnalyzersWithinEachTimeframe() {
        InstrumentSnapshot snapshot = new InstrumentSnapshot("TEST", Instant.parse("2026-10-13T12:00:00Z"),
                null, Map.of(Timeframe.H1, candles(Timeframe.H1)), null, null, null);
        var firstMetric = new TestMetrics(MetricName.EMA);
        var secondMetric = new TestMetrics(MetricName.ATR);

        IndicatorAnalyzer first = context -> {
            assertSame(snapshot, context.getSnapshot());
            context.addMetric(firstMetric);
        };
        IndicatorAnalyzer second = context -> {
            assertSame(firstMetric, context.getMetrics().get(MetricName.EMA));
            context.addMetric(secondMetric);
        };

        var result = new IndicatorAnalysisService(List.of(first, second)).analyze(snapshot);

        assertSame(snapshot, result.getSnapshot());
        assertEquals(2, result.getTimeframeMetrics().get(Timeframe.H1).size());
        assertSame(secondMetric, result.getTimeframeMetrics().get(Timeframe.H1).get(MetricName.ATR));
        assertThrows(UnsupportedOperationException.class,
                () -> result.getTimeframeMetrics().get(Timeframe.H1).remove(MetricName.EMA));
    }

    @Test
    void rejectsMissingSnapshotAndPropagatesAnalyzerFailure() {
        var service = new IndicatorAnalysisService(List.of(context -> {
            throw new IllegalStateException("bad input");
        }));
        assertThrows(NullPointerException.class, () -> service.analyze(null));
        var snapshot = new InstrumentSnapshot("TEST", Instant.EPOCH,
                null, Map.of(Timeframe.H1, candles(Timeframe.H1)), null, null, null);
        assertThrows(IllegalStateException.class, () -> service.analyze(snapshot));
    }

    @Test
    void skipsOnlyAnalyzersWhoseInputsWereOmitted() {
        var snapshot = new InstrumentSnapshot("TEST", Instant.EPOCH,
                null, Map.of(Timeframe.H1, candles(Timeframe.H1)), null, null, null);
        var market = mock(MarketAnalyzer.class);
        var liquidity = mock(LiquidityAnalyzer.class);
        var metric = new TestMetrics(MetricName.ATR);
        IndicatorAnalyzer candleAnalyzer = context -> context.addMetric(metric);

        var context = new IndicatorAnalysisService(List.of(candleAnalyzer, market, liquidity))
                .analyze(snapshot, Set.of(SkippableStep.MARKET_TICK, SkippableStep.ORDER_BOOK));

        assertSame(metric, context.getTimeframeMetrics().get(Timeframe.H1).get(MetricName.ATR));
        verifyNoInteractions(market, liquidity);
    }

    @Test
    void keepsIndicatorsSeparateAndRejectsOnlyWhenBothHistoriesAreEmpty() {
        var price = new Quotation(null, "100", 0);
        var daily = new java.util.ArrayList<Candle>();
        for (int i = 60; i >= 1; i--) {
            daily.add(new Candle(price, price, price, price, "1",
                    Instant.parse("2026-10-13T00:00:00Z").minusSeconds(i * 86400L),
                    true, null, null, null));
        }
        var snapshot = new InstrumentSnapshot("TEST", Instant.parse("2026-10-13T12:00:00Z"), null,
                Map.of(Timeframe.H1, candles(Timeframe.H1),
                        Timeframe.D1, new TInvestCandleData(Timeframe.D1, daily)),
                null, null, null);
        var result = new IndicatorAnalysisService(List.of(new AtrAnalyzer(), new EmaAnalyzer())).analyze(snapshot);
        assertTrue(result.getTimeframeMetrics().get(Timeframe.H1).isEmpty());
        assertTrue(result.getTimeframeMetrics().get(Timeframe.D1).containsKey(MetricName.EMA));
        assertTrue(result.getTimeframeMetrics().get(Timeframe.D1).containsKey(MetricName.ATR));

        var empty = new InstrumentSnapshot("TEST", Instant.EPOCH, null,
                Map.of(Timeframe.H1, new TInvestCandleData(Timeframe.H1, List.of()),
                        Timeframe.D1, new TInvestCandleData(Timeframe.D1, List.of())),
                null, null, null);
        assertThrows(IllegalStateException.class,
                () -> new IndicatorAnalysisService(List.of(new AtrAnalyzer())).analyze(empty));
    }

    private TInvestCandleData candles(Timeframe timeframe) {
        var price = new Quotation(null, "100", 0);
        var candle = new Candle(price, price, price, price, "1",
                Instant.parse("2026-10-13T10:00:00Z"), true, null, null, null);
        return new TInvestCandleData(timeframe, List.of(candle));
    }

    private record TestMetrics(MetricName type) implements TechMetrics {
    }
}
