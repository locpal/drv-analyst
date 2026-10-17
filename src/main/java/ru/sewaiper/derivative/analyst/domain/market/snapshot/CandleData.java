package ru.sewaiper.derivative.analyst.domain.market.snapshot;

import org.ta4j.core.BarSeries;

public interface CandleData {
    String serialize();

    BarSeries toBarSeries();
}
