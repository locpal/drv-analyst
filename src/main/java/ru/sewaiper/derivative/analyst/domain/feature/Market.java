package ru.sewaiper.derivative.analyst.domain.feature;

import java.time.Instant;

public class Market {

    private Instant asOfUtc;
    private Double lastPrice;
    private Double closePrice;
    private Instant closeTimeUtc;
    private Double eveningSessionPrice;
    private Instant eveningTimeUtc;
    private Integer openInterest;
    private Instant openInterestTimeUtc;

    public Instant getAsOfUtc() {
        return asOfUtc;
    }

    public void setAsOfUtc(Instant asOfUtc) {
        this.asOfUtc = asOfUtc;
    }

    public Double getLastPrice() {
        return lastPrice;
    }

    public void setLastPrice(Double lastPrice) {
        this.lastPrice = lastPrice;
    }

    public Double getClosePrice() {
        return closePrice;
    }

    public void setClosePrice(Double closePrice) {
        this.closePrice = closePrice;
    }

    public Instant getCloseTimeUtc() {
        return closeTimeUtc;
    }

    public void setCloseTimeUtc(Instant closeTimeUtc) {
        this.closeTimeUtc = closeTimeUtc;
    }

    public Double getEveningSessionPrice() {
        return eveningSessionPrice;
    }

    public void setEveningSessionPrice(Double eveningSessionPrice) {
        this.eveningSessionPrice = eveningSessionPrice;
    }

    public Instant getEveningTimeUtc() {
        return eveningTimeUtc;
    }

    public void setEveningTimeUtc(Instant eveningTimeUtc) {
        this.eveningTimeUtc = eveningTimeUtc;
    }

    public Integer getOpenInterest() {
        return openInterest;
    }

    public void setOpenInterest(Integer openInterest) {
        this.openInterest = openInterest;
    }

    public Instant getOpenInterestTimeUtc() {
        return openInterestTimeUtc;
    }

    public void setOpenInterestTimeUtc(Instant openInterestTimeUtc) {
        this.openInterestTimeUtc = openInterestTimeUtc;
    }
}
