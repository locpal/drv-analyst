package ru.sewaiper.derivative.analyst.domain.tinvest.bonds;

import ru.sewaiper.derivative.analyst.domain.tinvest.enums.InstrumentExchange;
import ru.sewaiper.derivative.analyst.domain.tinvest.enums.InstrumentStatus;

public record GetBondsRequest(InstrumentStatus instrumentStatus,
                              InstrumentExchange instrumentExchange) {
}
