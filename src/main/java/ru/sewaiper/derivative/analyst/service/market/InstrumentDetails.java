package ru.sewaiper.derivative.analyst.service.market;

import ru.sewaiper.derivative.analyst.domain.market.TradingMechanics;
import ru.sewaiper.derivative.analyst.domain.market.snapshot.InstrumentPassport;

public record InstrumentDetails(InstrumentPassport passport, TradingMechanics mechanics) {
}
