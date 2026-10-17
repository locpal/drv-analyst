package ru.sewaiper.derivative.analyst.domain.market.tech;

import org.ta4j.core.num.Num;

import java.time.Instant;
import java.util.Objects;

/** EMA values and their mathematical relationships on the last closed bar. */
public record EmaMetrics(Num ema20,
                         Num ema50,
                         Num spreadPts,
                         Num spreadPct,
                         Num closeToEma20Pts,
                         Num closeToEma20Pct,
                         Num ema20ChangePts,
                         Num ema20ChangePct,
                         int fastPeriod,
                         int slowPeriod,
                         int changeLookback,
                         int completeCandlesCount,
                         Instant lastCompleteCandleTimeUtc) implements TechMetrics {
    public EmaMetrics {
        Objects.requireNonNull(ema20, "ema20");
        Objects.requireNonNull(ema50, "ema50");
        Objects.requireNonNull(spreadPts, "spreadPts");
        Objects.requireNonNull(spreadPct, "spreadPct");
        Objects.requireNonNull(closeToEma20Pts, "closeToEma20Pts");
        Objects.requireNonNull(closeToEma20Pct, "closeToEma20Pct");
        Objects.requireNonNull(ema20ChangePts, "ema20ChangePts");
        Objects.requireNonNull(ema20ChangePct, "ema20ChangePct");
        Objects.requireNonNull(lastCompleteCandleTimeUtc, "lastCompleteCandleTimeUtc");
    }

    @Override
    public MetricName type() {
        return MetricName.EMA;
    }
}
