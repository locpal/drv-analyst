package ru.sewaiper.derivative.analyst.domain.tinvest.market;

import ru.sewaiper.derivative.analyst.domain.tinvest.Quotation;

public record OrderBookValue(Quotation price, String quantity) {
}
