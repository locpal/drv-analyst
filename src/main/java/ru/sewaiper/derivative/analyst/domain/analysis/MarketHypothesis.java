package ru.sewaiper.derivative.analyst.domain.analysis;

import java.math.BigDecimal;
import java.util.List;

public record MarketHypothesis(int tradingDays,
                               String type,
                               BigDecimal lowerBound,
                               BigDecimal upperBound,
                               String reasoning,
                               String alternativeScenario,
                               List<String> uncertaintyFactors) {
    public MarketHypothesis {
        uncertaintyFactors = List.copyOf(uncertaintyFactors);
    }
}
