package ru.sewaiper.derivative.analyst.domain.tinvest.candles;

import java.util.List;

public record GetCandlesResponse(List<Candle> candles) {
}
