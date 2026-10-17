package ru.sewaiper.derivative.analyst.domain.tinvest.bonds;

import ru.sewaiper.derivative.analyst.domain.tinvest.MoneyValue;
import ru.sewaiper.derivative.analyst.domain.tinvest.Quotation;
import ru.sewaiper.derivative.analyst.domain.tinvest.enums.RealExchange;
import ru.sewaiper.derivative.analyst.domain.tinvest.enums.RiskLevel;
import ru.sewaiper.derivative.analyst.domain.tinvest.enums.TradingStatus;

import java.time.Instant;
import java.util.List;

public record Bond(String figi,
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
                   Integer couponQuantityPerYear,
                   Instant maturityDate,
                   MoneyValue nominal,
                   MoneyValue initialNominal,
                   Instant stateRegDate,
                   Instant placementDate,
                   MoneyValue placementPrice,
                   MoneyValue aciValue,
                   String countryOfRisk,
                   String countryOfRiskName,
                   String sector,
                   String issueKind, // documentary или non_documentary
                   Long issueSize,
                   Long issueSizePlan,
                   TradingStatus tradingStatus,
                   Boolean otcFlag,
                   Boolean buyAvailableFlag,
                   Boolean sellAvailableFlag,
                   Boolean floatingCouponFlag,
                   Boolean perpetualFlag,
                   Boolean amortizationFlag,
                   Quotation minPriceIncrement,
                   Boolean apiTradeAvailableFlag,
                   String uid,
                   RealExchange realExchange,
                   String positionUid,
                   String assetUid,
                   List<String> requiredTests, // Array типов тестов
                   Boolean forIisFlag,
                   Boolean forQualInvestorFlag,
                   Boolean weekendFlag,
                   Boolean blockedTcaFlag,
                   Boolean subordinatedFlag,
                   Boolean liquidityFlag,
                   Instant first1minCandleDate,
                   Instant first1dayCandleDate,
                   RiskLevel riskLevel,
                   BrandData brand,
                   BondType bondType,
                   Instant callDate,
                   Quotation dlongClient,
                   Quotation dshortClient) {
}
