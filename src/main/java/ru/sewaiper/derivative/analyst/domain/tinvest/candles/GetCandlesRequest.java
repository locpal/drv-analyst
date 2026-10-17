package ru.sewaiper.derivative.analyst.domain.tinvest.candles;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

public record GetCandlesRequest(@JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
                                Instant from,
                                @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
                                Instant to,
                                CandleInterval interval,
                                String instrumentId,
                                CandleSource candleSourceType,
                                Integer limit) {
}
