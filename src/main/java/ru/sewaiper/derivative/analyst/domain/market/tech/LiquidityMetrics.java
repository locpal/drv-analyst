package ru.sewaiper.derivative.analyst.domain.market.tech;

import org.ta4j.core.num.Num;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Facts calculated from one order book snapshot; depth and imbalance describe only its visible levels. */
public record LiquidityMetrics(Num bestBid,
                               Num bestAsk,
                               long bestBidQuantity,
                               long bestAskQuantity,
                               Num spreadPts,
                               Num spreadMidPct,
                               Optional<Num> spreadAtrPct,
                               int depthLevelsPerSide,
                               long bidDepthQuantity,
                               long askDepthQuantity,
                               Num depthImbalance,
                               Instant orderBookCapturedAtUtc) implements TechMetrics {
    public LiquidityMetrics {
        Objects.requireNonNull(bestBid, "bestBid");
        Objects.requireNonNull(bestAsk, "bestAsk");
        Objects.requireNonNull(spreadPts, "spreadPts");
        Objects.requireNonNull(spreadMidPct, "spreadMidPct");
        Objects.requireNonNull(spreadAtrPct, "spreadAtrPct");
        Objects.requireNonNull(depthImbalance, "depthImbalance");
        Objects.requireNonNull(orderBookCapturedAtUtc, "orderBookCapturedAtUtc");
    }

    @Override
    public MetricName type() {
        return MetricName.LIQUIDITY;
    }
}
