package ru.sewaiper.derivative.analyst.domain.feature;

import java.util.List;

public class Availability {

    private boolean apiTradeAvailable;
    private boolean buyAvailable;
    private boolean sellAvailable;
    private boolean shortEnabled;
    private String tradingStatus;
    private List<String> requiredTests;
    private boolean forQualInvestorOnly;

    public boolean isApiTradeAvailable() {
        return apiTradeAvailable;
    }

    public void setApiTradeAvailable(boolean apiTradeAvailable) {
        this.apiTradeAvailable = apiTradeAvailable;
    }

    public boolean isBuyAvailable() {
        return buyAvailable;
    }

    public void setBuyAvailable(boolean buyAvailable) {
        this.buyAvailable = buyAvailable;
    }

    public boolean isSellAvailable() {
        return sellAvailable;
    }

    public void setSellAvailable(boolean sellAvailable) {
        this.sellAvailable = sellAvailable;
    }

    public boolean isShortEnabled() {
        return shortEnabled;
    }

    public void setShortEnabled(boolean shortEnabled) {
        this.shortEnabled = shortEnabled;
    }

    public String getTradingStatus() {
        return tradingStatus;
    }

    public void setTradingStatus(String tradingStatus) {
        this.tradingStatus = tradingStatus;
    }

    public List<String> getRequiredTests() {
        return requiredTests;
    }

    public void setRequiredTests(List<String> requiredTests) {
        this.requiredTests = requiredTests;
    }

    public boolean isForQualInvestorOnly() {
        return forQualInvestorOnly;
    }

    public void setForQualInvestorOnly(boolean forQualInvestorOnly) {
        this.forQualInvestorOnly = forQualInvestorOnly;
    }
}
