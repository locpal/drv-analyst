package ru.sewaiper.derivative.analyst.domain.market.snapshot;

import java.math.BigDecimal;

public record PriceLevel(BigDecimal price, long quantity) {
}
