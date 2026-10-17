package ru.sewaiper.derivative.analyst.domain.tinvest.market;

import java.util.List;

public record MarketValuesRequest(List<String> instrumentId, List<MarketValueType> values) {
}
