package ru.sewaiper.derivative.analyst.domain.market.tech;

import org.ta4j.core.num.Num;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Rolling extrema and independently confirmed pivots on completed bars. */
public record LevelMetrics(Num rollingHigh,
                           Num rollingLow,
                           Num highAboveClosePts,
                           Num highAboveClosePct,
                           Num closeAboveLowPts,
                           Num closeAboveLowPct,
                           Optional<ConfirmedSwing> confirmedSwingHigh,
                           Optional<ConfirmedSwing> confirmedSwingLow,
                           int swingLeftBars,
                           int swingRightBars,
                           int window,
                           int completeCandlesCount,
                           Instant lastCompleteCandleTimeUtc) implements TechMetrics {
    public LevelMetrics {
        Objects.requireNonNull(rollingHigh, "rollingHigh");
        Objects.requireNonNull(rollingLow, "rollingLow");
        Objects.requireNonNull(highAboveClosePts, "highAboveClosePts");
        Objects.requireNonNull(highAboveClosePct, "highAboveClosePct");
        Objects.requireNonNull(closeAboveLowPts, "closeAboveLowPts");
        Objects.requireNonNull(closeAboveLowPct, "closeAboveLowPct");
        Objects.requireNonNull(confirmedSwingHigh, "confirmedSwingHigh");
        Objects.requireNonNull(confirmedSwingLow, "confirmedSwingLow");
        Objects.requireNonNull(lastCompleteCandleTimeUtc, "lastCompleteCandleTimeUtc");
    }

    @Override
    public MetricName type() {
        return MetricName.LEVEL;
    }
}
