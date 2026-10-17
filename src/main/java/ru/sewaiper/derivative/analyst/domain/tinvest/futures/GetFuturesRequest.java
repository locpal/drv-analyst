package ru.sewaiper.derivative.analyst.domain.tinvest.futures;

import ru.sewaiper.derivative.analyst.domain.tinvest.enums.InstrumentExchange;
import ru.sewaiper.derivative.analyst.domain.tinvest.enums.InstrumentStatus;

public record GetFuturesRequest(InstrumentStatus instrumentStatus,
                                InstrumentExchange instrumentExchange) {
}
