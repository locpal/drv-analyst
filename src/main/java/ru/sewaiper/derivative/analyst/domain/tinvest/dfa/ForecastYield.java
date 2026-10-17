package ru.sewaiper.derivative.analyst.domain.tinvest.dfa;

import ru.sewaiper.derivative.analyst.domain.tinvest.Quotation;

public record ForecastYield(Quotation minValue,
                            Quotation maxValue) {
}
