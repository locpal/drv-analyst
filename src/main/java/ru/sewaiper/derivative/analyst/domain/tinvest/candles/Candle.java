package ru.sewaiper.derivative.analyst.domain.tinvest.candles;

import ru.sewaiper.derivative.analyst.domain.tinvest.Quotation;

import java.time.Instant;

public record Candle(Quotation open,
                     Quotation high,
                     Quotation low,
                     Quotation close,
                     String volume,
                     Instant time,
                     boolean isComplete,
                     CandleSource candleSource,
                     String volumeBuy,
                     String volumeSell) {
}
