package ru.sewaiper.derivative.analyst.domain.tinvest.schedule;

import java.time.Instant;

public record TradingSchedulesRequest(String exchange, Instant from, Instant to) {
}
