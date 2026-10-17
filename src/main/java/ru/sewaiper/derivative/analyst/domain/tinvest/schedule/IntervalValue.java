package ru.sewaiper.derivative.analyst.domain.tinvest.schedule;

import java.time.Instant;

public record IntervalValue(Instant startTs, Instant endTs) {
}
