package ru.sewaiper.derivative.analyst.domain.tinvest.market;

import java.util.List;

public record MarketValuesResponse(List<MarketValuePosition> instruments) {
}
