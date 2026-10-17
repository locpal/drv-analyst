package ru.sewaiper.derivative.analyst.domain.tinvest.shares;

import java.util.List;

public record GetSharesResponse(List<Share> instruments) {
}
