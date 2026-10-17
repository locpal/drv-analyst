package ru.sewaiper.derivative.analyst.domain.tinvest.instrument;

import ru.sewaiper.derivative.analyst.domain.tinvest.Quotation;
import ru.sewaiper.derivative.analyst.domain.tinvest.enums.RealExchange;
import ru.sewaiper.derivative.analyst.domain.tinvest.enums.TradingStatus;

import java.time.Instant;
import java.util.List;

public record Instrument(String uid,
                         String figi,
                         String ticker,
                         String classCode,
                         String isin,
                         String positionUid,
                         String assetUid,
                         String name,
                         int lot,
                         String currency,
                         Quotation minPriceIncrement,
                         String exchange,
                         RealExchange realExchange,
                         String instrumentType,
                         String instrumentKind,
                         String countryOfRisk,
                         String countryOfRiskName,
                         TradingStatus tradingStatus,
                         boolean apiTradeAvailableFlag,
                         boolean buyAvailableFlag,
                         boolean sellAvailableFlag,
                         boolean shortEnabledFlag,
                         boolean otcFlag,
                         boolean forIisFlag,
                         boolean forQualInvestorFlag,
                         boolean weekendFlag,
                         boolean blockedTcaFlag,
                         List<String> requiredTests,
                         Quotation klong,
                         Quotation kshort,
                         Quotation dlong,
                         Quotation dshort,
                         Quotation dlongMin,
                         Quotation dshortMin,
                         Quotation dlongClient,
                         Quotation dshortClient,
                         Instant first1minCandleDate,
                         Instant first1dayCandleDate) {

}