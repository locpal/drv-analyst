package ru.sewaiper.derivative.analyst.domain.tinvest;

import java.math.BigDecimal;

public record MoneyValue(String currency, String units, Integer nano) {

    public BigDecimal asBigDecimal() {
        return BigDecimal.valueOf(Long.parseLong(units))
                .add(BigDecimal.valueOf(nano, 9));
    }

    public Double asDouble() {
        return asBigDecimal().doubleValue();
    }
}
