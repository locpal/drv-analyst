package ru.sewaiper.derivative.analyst.service.market;

import org.jspecify.annotations.NonNull;
import ru.sewaiper.derivative.analyst.domain.market.InstrumentLocator;
import ru.sewaiper.derivative.analyst.domain.market.SnapshotDepth;
import ru.sewaiper.derivative.analyst.domain.market.snapshot.InstrumentSnapshot;

public interface MarketDataProvider {

    @NonNull
    InstrumentSnapshot getSnapshot(@NonNull InstrumentLocator locator, @NonNull SnapshotDepth depth);
}
