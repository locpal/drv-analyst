package ru.sewaiper.derivative.analyst.domain.market.snapshot;

import java.math.BigDecimal;
import java.time.Instant;

/** Values returned by the provider; timeUtc is the last-price timestamp, not fetch time. */
public record MarketTick(Instant timeUtc,
                         BigDecimal lastPrice,
                         BigDecimal closePrice,
                         Long openInterest,
                         Instant closePriceTimeUtc,
                         Instant openInterestTimeUtc) {
}
