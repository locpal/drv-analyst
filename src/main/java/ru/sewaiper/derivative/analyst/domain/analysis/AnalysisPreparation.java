package ru.sewaiper.derivative.analyst.domain.analysis;

import ru.sewaiper.derivative.analyst.domain.market.InstrumentLocator;
import ru.sewaiper.derivative.analyst.domain.market.MarketContext;
import ru.sewaiper.derivative.analyst.domain.market.SnapshotDepth;
import ru.sewaiper.derivative.analyst.domain.schedule.TradingHorizon;

import java.util.List;
import java.util.Objects;

/** Inputs assembled for an analysis; no Gemini hypothesis has been created yet. */
public record AnalysisPreparation(InstrumentLocator locator,
                                  SnapshotDepth depth,
                                  MarketContext marketContext,
                                  List<TradingHorizon> horizons) {

    public AnalysisPreparation {
        Objects.requireNonNull(locator, "locator");
        Objects.requireNonNull(depth, "depth");
        Objects.requireNonNull(marketContext, "marketContext");
        horizons = List.copyOf(horizons);
    }
}
