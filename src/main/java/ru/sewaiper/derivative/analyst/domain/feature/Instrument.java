package ru.sewaiper.derivative.analyst.domain.feature;

import ru.sewaiper.derivative.analyst.domain.market.tech.AtrMetrics;
import ru.sewaiper.derivative.analyst.domain.market.tech.EmaMetrics;

import java.math.BigDecimal;
import java.time.Instant;

public class Instrument {

    private String uid;
    private String figi;
    private String ticker;
    private String classCode;
    private String name;

    private String realExchange;
    private String currency;

    private String assetType;
    private String basicAsset;
    private BigDecimal basicAssetSize;
    private String settlementType;

    private Integer lot;

    private BigDecimal tickSize;
    private BigDecimal tickValue;

    private Instant firstTradeDate;
    private Instant lastTradeDate;
    private Instant expirationDate;

    private Margins margins;

    private Availability availability;

    private Market market;

    private AtrMetrics atr;

    private EmaMetrics ema;

    private Liquidity liquidity;

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getFigi() {
        return figi;
    }

    public void setFigi(String figi) {
        this.figi = figi;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public String getClassCode() {
        return classCode;
    }

    public void setClassCode(String classCode) {
        this.classCode = classCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRealExchange() {
        return realExchange;
    }

    public void setRealExchange(String realExchange) {
        this.realExchange = realExchange;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getAssetType() {
        return assetType;
    }

    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }

    public String getBasicAsset() {
        return basicAsset;
    }

    public void setBasicAsset(String basicAsset) {
        this.basicAsset = basicAsset;
    }

    public BigDecimal getBasicAssetSize() {
        return basicAssetSize;
    }

    public void setBasicAssetSize(BigDecimal basicAssetSize) {
        this.basicAssetSize = basicAssetSize;
    }

    public String getSettlementType() {
        return settlementType;
    }

    public void setSettlementType(String settlementType) {
        this.settlementType = settlementType;
    }

    public Integer getLot() {
        return lot;
    }

    public void setLot(Integer lot) {
        this.lot = lot;
    }

    public BigDecimal getTickSize() {
        return tickSize;
    }

    public void setTickSize(BigDecimal tickSize) {
        this.tickSize = tickSize;
    }

    public BigDecimal getTickValue() {
        return tickValue;
    }

    public void setTickValue(BigDecimal tickValue) {
        this.tickValue = tickValue;
    }

    public Instant getFirstTradeDate() {
        return firstTradeDate;
    }

    public void setFirstTradeDate(Instant firstTradeDate) {
        this.firstTradeDate = firstTradeDate;
    }

    public Instant getLastTradeDate() {
        return lastTradeDate;
    }

    public void setLastTradeDate(Instant lastTradeDate) {
        this.lastTradeDate = lastTradeDate;
    }

    public Instant getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Instant expirationDate) {
        this.expirationDate = expirationDate;
    }

    public Margins getMargins() {
        return margins;
    }

    public void setMargins(Margins margins) {
        this.margins = margins;
    }

    public Availability getAvailability() {
        return availability;
    }

    public void setAvailability(Availability availability) {
        this.availability = availability;
    }

    public Market getMarket() {
        return market;
    }

    public void setMarket(Market market) {
        this.market = market;
    }

    public AtrMetrics getAtr() {
        return atr;
    }

    public void setAtr(AtrMetrics atr) {
        this.atr = atr;
    }

    public EmaMetrics getEma() {
        return ema;
    }

    public void setEma(EmaMetrics ema) {
        this.ema = ema;
    }

    public Liquidity getLiquidity() {
        return liquidity;
    }

    public void setLiquidity(Liquidity liquidity) {
        this.liquidity = liquidity;
    }
}
