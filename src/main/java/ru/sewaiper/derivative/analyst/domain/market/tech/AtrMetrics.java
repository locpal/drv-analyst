package ru.sewaiper.derivative.analyst.domain.market.tech;

import org.jspecify.annotations.NonNull;
import org.ta4j.core.num.Num;

import java.time.Instant;
import java.util.Objects;

/** ATR(28) and its share of the last closed candle's price. */
public record AtrMetrics(Num atrPts,
                         Num atrPct,
                         int period,
                         int completeCandlesCount,
                         Instant lastCompleteCandleTimeUtc) implements TechMetrics {
    public AtrMetrics {
        Objects.requireNonNull(atrPts, "atrPts");
        Objects.requireNonNull(atrPct, "atrPct");
        Objects.requireNonNull(lastCompleteCandleTimeUtc, "lastCompleteCandleTimeUtc");
    }

    @Override
    public @NonNull MetricName type() {
        return MetricName.ATR;
    }
}
