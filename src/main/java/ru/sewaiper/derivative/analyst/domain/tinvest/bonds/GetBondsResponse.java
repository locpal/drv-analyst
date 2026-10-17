package ru.sewaiper.derivative.analyst.domain.tinvest.bonds;

import java.util.List;

public record GetBondsResponse(List<Bond> instruments) {
}
