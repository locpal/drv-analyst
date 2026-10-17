package ru.sewaiper.derivative.analyst.domain.tinvest.market;

import ru.sewaiper.derivative.analyst.domain.tinvest.Quotation;

import java.time.Instant;
import java.util.List;

public record OrderBookResponse(String figi,
                                Integer depth,
                                List<OrderBookValue> bids,
                                List<OrderBookValue> asks,
                                Quotation lastPrice,
                                Quotation closePrice,
                                Quotation limitUp,
                                Quotation limitDown,
                                String instrumentUid,
                                String ticker,
                                String classCode,
                                Instant lastPriceTs,
                                Instant closePriceTs) {

}
