package ru.sewaiper.derivative.analyst.domain.market.snapshot;

import ru.sewaiper.derivative.analyst.domain.market.TradingMechanics;
import ru.sewaiper.derivative.analyst.domain.market.enums.Timeframe;

import java.time.Instant;
import java.util.Map;

/**
 * Неизменяемый слепок рынка в конкретный момент времени. Служит единственным
 * источником сырых данных для всех анализаторов.
 *
 * @param ticker Внутренний тикер (например, "SBER" или "BRU4")
 * @param snapshotTimeUtc Точное время формирования слепка
 * @param candleData Использованные свечи провайдера по таймфреймам до преобразования в ta4j
 * @param orderBook Состояние стакана
 * @param currentTick Текущие значения (последняя цена, открытый интерес)
 */
public record InstrumentSnapshot(String ticker,
                                 Instant snapshotTimeUtc,
                                 InstrumentPassport passport,
                                 Map<Timeframe, TInvestCandleData> candleData,
                                 OrderBookSnapshot orderBook,
                                 MarketTick currentTick,
                                 TradingMechanics tradingMechanics) {
    public InstrumentSnapshot {
        candleData = candleData == null ? Map.of() : Map.copyOf(candleData);
        candleData.forEach((timeframe, data) -> {
            if (timeframe != data.timeframe()) {
                throw new IllegalArgumentException("Candle timeframe does not match its map key");
            }
        });
    }
}
