package ru.sewaiper.derivative.analyst.domain.tinvest.dfa;

import ru.sewaiper.derivative.analyst.domain.tinvest.MoneyValue;
import ru.sewaiper.derivative.analyst.domain.tinvest.Quotation;

import java.time.Instant;
import java.util.List;

public record Dfa(String uid,
                  String ticker,
                  String name,
                  String positionUid,
                  Quotation minPriceIncrement,
                  Integer lot,
                  MoneyValue nominal,
                  String currency, // Поле "Валюта" из спецификации
                  Instant maturityDate,
                  Boolean shortEnabledFlag,
                  Boolean apiTradeAvailableFlag,
                  Boolean buyAvailableFlag,
                  Boolean sellAvailableFlag,
                  Boolean limitOrderAvailableFlag,
                  Boolean marketOrderAvailableFlag,
                  Boolean bestpriceOrderAvailableFlag,
                  Boolean forIisFlag,
                  Boolean forQualInvestorFlag,
                  String type,
                  List<BasicAsset> basicAssets,
                  ForecastYield forecastYield,
                  Quotation yieldToMaturity,
                  Quotation couponValue,
                  Integer couponPaymentFrequency,
                  Instant couponPaymentDate,
                  Quotation aciValue) {
}
