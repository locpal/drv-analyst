package ru.sewaiper.derivative.analyst.service.analyzer;

import ru.sewaiper.derivative.analyst.domain.market.MarketContext;

public interface IndicatorAnalyzer {

    void analyze(MarketContext context);
}
