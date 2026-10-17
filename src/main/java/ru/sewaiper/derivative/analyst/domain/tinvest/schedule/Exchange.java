package ru.sewaiper.derivative.analyst.domain.tinvest.schedule;

import java.util.List;

/**
 * @param exchange Наименование торговой площадки.
 * @param days Массив с торговыми и неторговыми днями.
 */
public record Exchange(String exchange, List<Day> days) {
}
