package ru.sewaiper.derivative.analyst.domain.tinvest.schedule;

/**
 * @param type Название интервала.
 */
public record TradingInterval(String type, IntervalValue interval) {
}
