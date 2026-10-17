package ru.sewaiper.derivative.analyst.domain.analysis;

/** Optional parts of an analysis; snapshot persistence and candle calculations always run. */
public enum SkippableStep {
    MARKET_TICK,
    ORDER_BOOK,
    FORECAST
}
