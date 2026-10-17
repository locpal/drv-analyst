package ru.sewaiper.derivative.analyst.domain.tinvest.futures;

import ru.sewaiper.derivative.analyst.domain.tinvest.enums.InstrumentIdType;

public record GetFutureRequest(String id, InstrumentIdType idType) {
}
