package ru.sewaiper.derivative.analyst.domain.market.tech;

import org.ta4j.core.num.Num;

import java.time.Instant;
import java.util.Objects;

/** RSI value and its change in RSI points over completed bars. */
public record RsiMetrics(Num value,
                         Num changePts,
                         int period,
                         int changeLookback,
                         int completeCandlesCount,
                         Instant lastCompleteCandleTimeUtc) implements TechMetrics {
    public RsiMetrics {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(changePts, "changePts");
        Objects.requireNonNull(lastCompleteCandleTimeUtc, "lastCompleteCandleTimeUtc");
    }

    @Override
    public MetricName type() {
        return MetricName.RSI;
    }
}
