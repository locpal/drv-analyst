package ru.sewaiper.derivative.analyst.domain.tinvest.instrument;

import ru.sewaiper.derivative.analyst.domain.tinvest.enums.InstrumentIdType;

public record GetInstrumentRequest(String id, InstrumentIdType idType) {
}
