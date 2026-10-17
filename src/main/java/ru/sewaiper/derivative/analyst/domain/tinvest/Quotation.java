package ru.sewaiper.derivative.analyst.domain.tinvest;

import java.math.BigDecimal;

public record Quotation(String currency, String units, Integer nano) {

    public long asNano() {
        long u = Long.parseLong(units);
        return u * 1_000_000_000L + nano;
    }

    public BigDecimal asBigDecimal() {
        return BigDecimal.valueOf(Long.parseLong(units))
                .add(BigDecimal.valueOf(nano, 9));
    }

    public double asDouble() {
        return Long.parseLong(units) + nano / 1e9;
    }
}
