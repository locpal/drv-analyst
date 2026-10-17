package ru.sewaiper.derivative.analyst.service.market;

import ru.sewaiper.derivative.analyst.domain.market.enums.AssetType;

public interface InstrumentDetailsLoader {
    AssetType assetType();

    InstrumentDetails load(String uid);
}
