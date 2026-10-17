package ru.sewaiper.derivative.analyst.domain.tinvest.futures;

import java.util.List;

public record GetFuturesResponse(List<Future> instruments) {
}
