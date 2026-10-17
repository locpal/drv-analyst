package ru.sewaiper.derivative.analyst.service.analyzer;

import org.junit.jupiter.api.Test;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.num.DecimalNumFactory;
import ru.sewaiper.derivative.analyst.domain.market.MarketContext;
import ru.sewaiper.derivative.analyst.domain.market.snapshot.InstrumentSnapshot;
import ru.sewaiper.derivative.analyst.domain.market.snapshot.MarketTick;
import ru.sewaiper.derivative.analyst.domain.market.tech.MarketMetrics;
import ru.sewaiper.derivative.analyst.domain.market.tech.MetricName;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class MarketAnalyzerTest {
    private static final Instant LAST_AT = Instant.parse("2026-10-16T17:01:00Z");
    private static final Instant CLOSE_AT = Instant.parse("2026-10-15T20:00:00Z");
    private final MarketAnalyzer analyzer = new MarketAnalyzer();

    @Test
    void calculatesExactProviderPriceDifferenceAndPreservesTimestamps() {
        var context = context(new MarketTick(LAST_AT,
                new BigDecimal("100.123456789"), new BigDecimal("100.123456780"),
                123_456L, CLOSE_AT, LAST_AT));

        analyzer.analyze(context);

        var metrics = (MarketMetrics) context.getMetrics().get(MetricName.MARKET);
        assertNotNull(metrics);
        assertEquals(0, metrics.lastVsClosePts().bigDecimalValue()
                .compareTo(new BigDecimal("0.000000009")));
        assertEquals(0, metrics.lastVsClosePct().bigDecimalValue()
                .compareTo(new BigDecimal("0.000000009")
                        .divide(new BigDecimal("100.123456780"), java.math.MathContext.DECIMAL64)
                        .multiply(new BigDecimal("100"))));
        assertEquals(LAST_AT, metrics.lastPriceTimeUtc());
        assertEquals(CLOSE_AT, metrics.providerCloseTimeUtc().orElseThrow());
        assertEquals(123_456L, metrics.openInterest().orElseThrow());
        assertEquals(LAST_AT, metrics.openInterestTimeUtc().orElseThrow());
    }

    @Test
    void keepsOpenInterestAbsentWhenProviderDoesNotSupplyIt() {
        var context = context(new MarketTick(LAST_AT,
                new BigDecimal("99"), new BigDecimal("100"), null, CLOSE_AT, null));

        analyzer.analyze(context);

        var metrics = (MarketMetrics) context.getMetrics().get(MetricName.MARKET);
        assertEquals(-1, metrics.lastVsClosePts().intValue());
        assertEquals(-1, metrics.lastVsClosePct().intValue());
        assertTrue(metrics.openInterest().isEmpty());
        assertTrue(metrics.openInterestTimeUtc().isEmpty());
    }

    @Test
    void rejectsMissingOrZeroReferencePrice() {
        var missing = context(new MarketTick(LAST_AT, null,
                new BigDecimal("100"), null, CLOSE_AT, null));
        var zeroClose = context(new MarketTick(LAST_AT,
                new BigDecimal("100"), BigDecimal.ZERO, null, CLOSE_AT, null));

        assertThrows(IllegalStateException.class, () -> analyzer.analyze(missing));
        assertThrows(IllegalStateException.class, () -> analyzer.analyze(zeroClose));
        assertFalse(missing.getMetrics().containsKey(MetricName.MARKET));
        assertFalse(zeroClose.getMetrics().containsKey(MetricName.MARKET));
    }

    private MarketContext context(MarketTick tick) {
        var snapshot = new InstrumentSnapshot("TEST", LAST_AT, null, null, null, tick, null);
        var series = new BaseBarSeriesBuilder().withNumFactory(DecimalNumFactory.getInstance()).build();
        return new MarketContext(snapshot, series);
    }
}
