package ru.sewaiper.derivative.analyst.domain.tinvest.market;

import ru.sewaiper.derivative.analyst.domain.tinvest.Quotation;

import java.time.Instant;

public record MarketValue(MarketValueType type,
                          Quotation value,
                          Instant time) {
}
