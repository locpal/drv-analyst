package ru.sewaiper.derivative.analyst.domain.tinvest.shares;

import ru.sewaiper.derivative.analyst.domain.tinvest.enums.InstrumentExchange;
import ru.sewaiper.derivative.analyst.domain.tinvest.enums.InstrumentStatus;

public record GetSharesRequest(InstrumentStatus instrumentStatus,
                               InstrumentExchange instrumentExchange) {
}
