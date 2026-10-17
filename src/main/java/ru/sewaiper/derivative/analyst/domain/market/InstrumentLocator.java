package ru.sewaiper.derivative.analyst.domain.market;

import ru.sewaiper.derivative.analyst.domain.market.enums.AssetType;
import ru.sewaiper.derivative.analyst.domain.market.enums.Provider;

/**
 *
 * @param providerId Уникальный идентификатор инструмента у провайдера (UID T-Invest)
 * @param type Тип инструмента
 * @param provider Провайдер данных
 */
public record InstrumentLocator(String providerId,
                                AssetType type,
                                Provider provider) {
}
