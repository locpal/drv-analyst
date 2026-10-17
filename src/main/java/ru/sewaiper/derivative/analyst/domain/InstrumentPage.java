package ru.sewaiper.derivative.analyst.domain;

import ru.sewaiper.derivative.analyst.domain.market.snapshot.InstrumentPassport;

import java.util.List;

public record InstrumentPage(List<InstrumentPassport> items,
                             int page,
                             int size,
                             long totalElements,
                             int totalPages) {
}
