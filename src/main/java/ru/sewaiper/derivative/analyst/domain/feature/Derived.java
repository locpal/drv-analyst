package ru.sewaiper.derivative.analyst.domain.feature;

public class Derived {

    private Double changeFromClosePts;
    private Double changeFromClosePct;

    public Double getChangeFromClosePts() {
        return changeFromClosePts;
    }

    public void setChangeFromClosePts(Double changeFromClosePts) {
        this.changeFromClosePts = changeFromClosePts;
    }

    public Double getChangeFromClosePct() {
        return changeFromClosePct;
    }

    public void setChangeFromClosePct(Double changeFromClosePct) {
        this.changeFromClosePct = changeFromClosePct;
    }
}
