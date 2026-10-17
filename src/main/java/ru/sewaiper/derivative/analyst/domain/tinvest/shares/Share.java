package ru.sewaiper.derivative.analyst.domain.tinvest.shares;

import ru.sewaiper.derivative.analyst.domain.tinvest.Brand;
import ru.sewaiper.derivative.analyst.domain.tinvest.MoneyValue;
import ru.sewaiper.derivative.analyst.domain.tinvest.Quotation;
import ru.sewaiper.derivative.analyst.domain.tinvest.enums.InstrumentExchange;
import ru.sewaiper.derivative.analyst.domain.tinvest.enums.RealExchange;
import ru.sewaiper.derivative.analyst.domain.tinvest.enums.TradingStatus;

import java.time.Instant;
import java.util.List;

public record Share(String figi,
                    String ticker,
                    String classCode,
                    String isin,
                    Integer lot,
                    String currency,
                    Quotation klong,
                    Quotation kshort,
                    Quotation dlong,
                    Quotation dshort,
                    Quotation dlongMin,
                    Quotation dshortMin,
                    Boolean shortEnabledFlag,
                    String name,
                    String exchange,
                    Instant ipoDate,
                    Long issueSize,
                    String countryOfRisk,
                    String countryOfRiskName,
                    String sector,
                    Long issueSizePlan,
                    MoneyValue nominal,
                    TradingStatus tradingStatus,
                    Boolean otcFlag,
                    Boolean buyAvailableFlag,
                    Boolean sellAvailableFlag,
                    Boolean divYieldFlag,
                    ShareType shareType,
                    Quotation minPriceIncrement,
                    Boolean apiTradeAvailableFlag,
                    String uid,
                    RealExchange realExchange,
                    String positionUid,
                    String assetUid,
                    InstrumentExchange instrumentExchange,
                    List<String> requiredTests,
                    Boolean forIisFlag,
                    Boolean forQualInvestorFlag,
                    Boolean weekendFlag,
                    Boolean blockedTcaFlag,
                    Boolean liquidityFlag,
                    Instant first1minCandleDate,
                    Instant first1dayCandleDate,
                    Brand brand,
                    Quotation dlongClient,
                    Quotation dshortClient) {
}
