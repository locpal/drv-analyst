package ru.sewaiper.derivative.analyst.domain.market.snapshot;

import java.time.Instant;
import java.util.List;

public record OrderBookSnapshot(Instant timeUtc, List<PriceLevel> bids, List<PriceLevel> asks) {
}
