package ru.sewaiper.derivative.analyst.domain.schedule;

import java.time.Instant;
import java.time.LocalDate;

public record TradingHorizon(int tradingDays, LocalDate sessionDate, Instant targetAtUtc) {
}
