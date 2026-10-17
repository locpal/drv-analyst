package ru.sewaiper.derivative.analyst.service.analyzer;

import org.junit.jupiter.api.Test;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.num.DecimalNumFactory;
import ru.sewaiper.derivative.analyst.domain.market.MarketContext;
import ru.sewaiper.derivative.analyst.domain.market.snapshot.InstrumentSnapshot;
import ru.sewaiper.derivative.analyst.domain.market.snapshot.OrderBookSnapshot;
import ru.sewaiper.derivative.analyst.domain.market.snapshot.PriceLevel;
import ru.sewaiper.derivative.analyst.domain.market.tech.AtrMetrics;
import ru.sewaiper.derivative.analyst.domain.market.tech.LiquidityMetrics;
import ru.sewaiper.derivative.analyst.domain.market.tech.MetricName;
import ru.sewaiper.derivative.analyst.domain.tinvest.Quotation;
import ru.sewaiper.derivative.analyst.domain.tinvest.market.OrderBookValue;
import ru.sewaiper.derivative.analyst.mapper.TInvestMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LiquidityAnalyzerTest {
    private static final Instant CAPTURED_AT = Instant.parse("2026-10-16T11:00:00Z");
    private final LiquidityAnalyzer analyzer = new LiquidityAnalyzer();

    @Test
    void calculatesSpreadAndSymmetricDepthFromAnUnsortedBook() {
        var context = context(
                List.of(level("99.8", 20), level("99.9", 10)),
                List.of(level("100.2", 5), level("100.1", 15)), "2");

        analyzer.analyze(context);

        var metrics = (LiquidityMetrics) context.getMetrics().get(MetricName.LIQUIDITY);
        assertNotNull(metrics);
        assertEquals(99.9, metrics.bestBid().doubleValue(), 1e-12);
        assertEquals(100.1, metrics.bestAsk().doubleValue(), 1e-12);
        assertEquals(10, metrics.bestBidQuantity());
        assertEquals(15, metrics.bestAskQuantity());
        assertEquals(0.2, metrics.spreadPts().doubleValue(), 1e-12);
        assertEquals(0.2, metrics.spreadMidPct().doubleValue(), 1e-12);
        assertEquals(10.0, metrics.spreadAtrPct().orElseThrow().doubleValue(), 1e-12);
        assertEquals(2, metrics.depthLevelsPerSide());
        assertEquals(30, metrics.bidDepthQuantity());
        assertEquals(20, metrics.askDepthQuantity());
        assertEquals(0.2, metrics.depthImbalance().doubleValue(), 1e-12);
        assertEquals(CAPTURED_AT, metrics.orderBookCapturedAtUtc());
    }

    @Test
    void preservesNineDecimalPlacesFromTheProviderAndLeavesZeroAtrRatioUndefined() {
        var mapped = TInvestMapper.mapPriceLevel(new OrderBookValue(
                new Quotation(null, "100", 123_456_789), "7"));
        assertEquals(new BigDecimal("100.123456789"), mapped.price());
        var context = context(List.of(level("100.123456789", 7)),
                List.of(level("100.123456790", 8)), "0");

        analyzer.analyze(context);

        var metrics = (LiquidityMetrics) context.getMetrics().get(MetricName.LIQUIDITY);
        assertEquals(0, metrics.spreadPts().bigDecimalValue()
                .compareTo(new BigDecimal("0.000000001")));
        assertTrue(metrics.spreadAtrPct().isEmpty());
    }

    @Test
    void rejectsMissingOrCrossedBookWithoutPublishingMetrics() {
        var missing = context(List.of(), List.of(level("101", 1)), "2");
        var crossed = context(List.of(level("101", 1)), List.of(level("100", 1)), "2");

        assertThrows(IllegalStateException.class, () -> analyzer.analyze(missing));
        assertThrows(IllegalStateException.class, () -> analyzer.analyze(crossed));
        assertFalse(missing.getMetrics().containsKey(MetricName.LIQUIDITY));
        assertFalse(crossed.getMetrics().containsKey(MetricName.LIQUIDITY));
    }

    private MarketContext context(List<PriceLevel> bids, List<PriceLevel> asks, String atr) {
        var book = new OrderBookSnapshot(CAPTURED_AT, bids, asks);
        var snapshot = new InstrumentSnapshot("TEST", CAPTURED_AT, null, null, book, null, null);
        var series = new BaseBarSeriesBuilder().withNumFactory(DecimalNumFactory.getInstance()).build();
        var context = new MarketContext(snapshot, series);
        context.addMetric(new AtrMetrics(series.numFactory().numOf(atr), series.numFactory().zero(),
                28, 200, CAPTURED_AT));
        return context;
    }

    private static PriceLevel level(String price, long quantity) {
        return new PriceLevel(new BigDecimal(price), quantity);
    }
}
