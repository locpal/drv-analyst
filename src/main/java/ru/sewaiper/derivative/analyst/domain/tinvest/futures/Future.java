package ru.sewaiper.derivative.analyst.domain.tinvest.futures;

import ru.sewaiper.derivative.analyst.domain.tinvest.Brand;
import ru.sewaiper.derivative.analyst.domain.tinvest.MoneyValue;
import ru.sewaiper.derivative.analyst.domain.tinvest.Quotation;
import ru.sewaiper.derivative.analyst.domain.tinvest.enums.RealExchange;
import ru.sewaiper.derivative.analyst.domain.tinvest.enums.TradingStatus;

import java.time.Instant;
import java.util.List;

public record Future(String figi,
                     String ticker,
                     String classCode,
                     Integer lot,
                     String currency,
                     Quotation dlong,
                     Quotation dshort,
                     Quotation dlongMin,
                     Quotation dshortMin,
                     boolean shortEnabledFlag,
                     String name,
                     String exchange,
                     Instant firstTradeDate,
                     Instant lastTradeDate,
                     String futuresType,
                     String assetType,
                     String basicAsset,
                     Quotation basicAssetSize,
                     String countryOfRisk,
                     String countryOfRiskName,
                     String sector,
                     Instant expirationDate,
                     TradingStatus tradingStatus,
                     boolean otcFlag,
                     boolean buyAvailableFlag,
                     boolean sellAvailableFlag,
                     Quotation minPriceIncrement,
                     boolean apiTradeAvailableFlag,
                     String uid,
                     RealExchange realExchange,
                     String positionUid,
                     String basicAssetPositionUid,
                     List<String> requiredTests,
                     boolean forIisFlag,
                     boolean forQualInvestorFlag,
                     boolean weekendFlag,
                     boolean blockedTcaFlag,
                     Instant first1minCandleDate,
                     Instant first1dayCandleDate,
                     MoneyValue initialMarginOnBuy,
                     MoneyValue initialMarginOnSell,
                     Quotation minPriceIncrementAmount,
                     Brand brand,
                     Quotation dlongClient,
                     Quotation dshortClient) {
}
