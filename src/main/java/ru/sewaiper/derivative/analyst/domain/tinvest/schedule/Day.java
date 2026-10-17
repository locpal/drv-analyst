package ru.sewaiper.derivative.analyst.domain.tinvest.schedule;

import java.time.Instant;
import java.util.List;

/**
 * @param date Дата.
 * @param isTradingDay Признак торгового дня на бирже.
 * @param startTime Время начала торгов по UTC.
 * @param endTime Время окончания торгов по UTC.
 * @param openingAuctionStartTime Время начала аукциона открытия по UTC.
 * @param closingAuctionEndTime Время окончания аукциона закрытия по UTC.
 * @param eveningOpeningAuctionStartTime Время начала аукциона открытия вечерней сессии по UTC.
 * @param eveningStartTime Время начала вечерней сессии по UTC.
 * @param eveningEndTime Время окончания вечерней сессии по UTC.
 * @param clearingStartTime Время начала основного клиринга по UTC.
 * @param clearingEndTime Время окончания основного клиринга по UTC.
 * @param premarketStartTime Время начала премаркета по UTC.
 * @param premarketEndTime Время окончания премаркета по UTC.
 * @param closingAuctionStartTime Время начала аукциона закрытия по UTC.
 * @param openingAuctionEndTime Время окончания аукциона открытия по UTC.
 * @param intervals Торговые интервалы.
 */
public record Day(Instant date,
                  boolean isTradingDay,
                  Instant startTime,
                  Instant endTime,
                  Instant openingAuctionStartTime,
                  Instant closingAuctionEndTime,
                  Instant eveningOpeningAuctionStartTime,
                  Instant eveningStartTime,
                  Instant eveningEndTime,
                  Instant clearingStartTime,
                  Instant clearingEndTime,
                  Instant premarketStartTime,
                  Instant premarketEndTime,
                  Instant closingAuctionStartTime,
                  Instant openingAuctionEndTime,
                  List<TradingInterval> intervals) {
}
