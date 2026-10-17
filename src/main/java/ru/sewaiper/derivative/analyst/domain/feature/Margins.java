package ru.sewaiper.derivative.analyst.domain.feature;

import java.math.BigDecimal;

public class Margins {

    private BigDecimal initialMarginBuy;
    private BigDecimal initialMarginSell;

    public BigDecimal getInitialMarginBuy() {
        return initialMarginBuy;
    }

    public void setInitialMarginBuy(BigDecimal initialMarginBuy) {
        this.initialMarginBuy = initialMarginBuy;
    }

    public BigDecimal getInitialMarginSell() {
        return initialMarginSell;
    }

    public void setInitialMarginSell(BigDecimal initialMarginSell) {
        this.initialMarginSell = initialMarginSell;
    }
}
