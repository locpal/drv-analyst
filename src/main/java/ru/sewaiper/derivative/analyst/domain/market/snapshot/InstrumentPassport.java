package ru.sewaiper.derivative.analyst.domain.market.snapshot;

import ru.sewaiper.derivative.analyst.domain.market.enums.AssetType;
import ru.sewaiper.derivative.analyst.domain.market.enums.Provider;

public record InstrumentPassport(String ticker,
                                 String name,
                                 AssetType assetType,
                                 Provider provider,
                                 String providerId,
                                 String figi,
                                 String exchange) {
}
