package ru.sewaiper.derivative.analyst.domain.tinvest.market;

import java.util.List;

public record MarketValuePosition(String instrumentUid,
                                  String ticker,
                                  String classCode,
                                  List<MarketValue> values) {
}
