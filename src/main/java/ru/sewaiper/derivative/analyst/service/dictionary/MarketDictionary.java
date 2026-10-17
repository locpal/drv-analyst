package ru.sewaiper.derivative.analyst.service.dictionary;

import ru.sewaiper.derivative.analyst.domain.InstrumentPage;
import ru.sewaiper.derivative.analyst.domain.market.enums.AssetType;
import ru.sewaiper.derivative.analyst.domain.market.enums.Provider;
import ru.sewaiper.derivative.analyst.domain.market.snapshot.InstrumentPassport;

import java.util.Set;

public interface MarketDictionary {

    Set<AssetType> supportedAssetTypes();

    void sync(AssetType assetType);

    InstrumentPassport getPassport(Provider provider, AssetType assetType, String providerId);

    InstrumentPage search(AssetType assetType, String query, int page, int size);
}
