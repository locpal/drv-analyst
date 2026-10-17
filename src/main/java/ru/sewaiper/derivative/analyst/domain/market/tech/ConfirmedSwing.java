package ru.sewaiper.derivative.analyst.domain.market.tech;

import org.ta4j.core.num.Num;

import java.time.Instant;
import java.util.Objects;

/** A pivot becomes available only after the required right-hand candles have closed. */
public record ConfirmedSwing(Num price,
                             Instant extremumCandleEndUtc,
                             Instant confirmationCandleEndUtc) {
    public ConfirmedSwing {
        Objects.requireNonNull(price, "price");
        Objects.requireNonNull(extremumCandleEndUtc, "extremumCandleEndUtc");
        Objects.requireNonNull(confirmationCandleEndUtc, "confirmationCandleEndUtc");
        if (!confirmationCandleEndUtc.isAfter(extremumCandleEndUtc)) {
            throw new IllegalArgumentException("Swing confirmation must follow the extremum candle");
        }
    }
}
