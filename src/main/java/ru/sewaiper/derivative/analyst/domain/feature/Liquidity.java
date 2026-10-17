package ru.sewaiper.derivative.analyst.domain.feature;

import java.time.Instant;

public class Liquidity {

    private Instant asOfUtc;
    private boolean marketOpen;
    private String reason;

    private Double bestBid;
    private Double bestAsk;
    private Double bestBidQty;
    private Double bestAskQty;

    private Double spreadPts;
    private Double spreadMoney;

    private Integer topN;
    private Double topDepthBidQty;
    private Double topDepthAskQty;
    private String imbalance;

    public Instant getAsOfUtc() {
        return asOfUtc;
    }

    public void setAsOfUtc(Instant asOfUtc) {
        this.asOfUtc = asOfUtc;
    }

    public boolean isMarketOpen() {
        return marketOpen;
    }

    public void setMarketOpen(boolean marketOpen) {
        this.marketOpen = marketOpen;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Double getBestBid() {
        return bestBid;
    }

    public void setBestBid(Double bestBid) {
        this.bestBid = bestBid;
    }

    public Double getBestAsk() {
        return bestAsk;
    }

    public void setBestAsk(Double bestAsk) {
        this.bestAsk = bestAsk;
    }

    public Double getBestBidQty() {
        return bestBidQty;
    }

    public void setBestBidQty(Double bestBidQty) {
        this.bestBidQty = bestBidQty;
    }

    public Double getBestAskQty() {
        return bestAskQty;
    }

    public void setBestAskQty(Double bestAskQty) {
        this.bestAskQty = bestAskQty;
    }

    public Double getSpreadPts() {
        return spreadPts;
    }

    public void setSpreadPts(Double spreadPts) {
        this.spreadPts = spreadPts;
    }

    public Double getSpreadMoney() {
        return spreadMoney;
    }

    public void setSpreadMoney(Double spreadMoney) {
        this.spreadMoney = spreadMoney;
    }

    public Integer getTopN() {
        return topN;
    }

    public void setTopN(Integer topN) {
        this.topN = topN;
    }

    public Double getTopDepthBidQty() {
        return topDepthBidQty;
    }

    public void setTopDepthBidQty(Double topDepthBidQty) {
        this.topDepthBidQty = topDepthBidQty;
    }

    public Double getTopDepthAskQty() {
        return topDepthAskQty;
    }

    public void setTopDepthAskQty(Double topDepthAskQty) {
        this.topDepthAskQty = topDepthAskQty;
    }

    public String getImbalance() {
        return imbalance;
    }

    public void setImbalance(String imbalance) {
        this.imbalance = imbalance;
    }
}
