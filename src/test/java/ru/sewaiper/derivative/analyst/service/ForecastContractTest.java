package ru.sewaiper.derivative.analyst.service;

import org.junit.jupiter.api.Test;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.num.DecimalNumFactory;
import ru.sewaiper.derivative.analyst.domain.analysis.AnalysisPreparation;
import ru.sewaiper.derivative.analyst.domain.llm.ChatCompletionResponse;
import ru.sewaiper.derivative.analyst.domain.market.InstrumentLocator;
import ru.sewaiper.derivative.analyst.domain.market.MarketContext;
import ru.sewaiper.derivative.analyst.domain.market.SnapshotDepth;
import ru.sewaiper.derivative.analyst.domain.market.TradingMechanics;
import ru.sewaiper.derivative.analyst.domain.market.enums.AssetType;
import ru.sewaiper.derivative.analyst.domain.market.enums.Provider;
import ru.sewaiper.derivative.analyst.domain.market.enums.Timeframe;
import ru.sewaiper.derivative.analyst.domain.market.snapshot.InstrumentPassport;
import ru.sewaiper.derivative.analyst.domain.market.snapshot.InstrumentSnapshot;
import ru.sewaiper.derivative.analyst.domain.market.snapshot.TInvestCandleData;
import ru.sewaiper.derivative.analyst.domain.tinvest.Quotation;
import ru.sewaiper.derivative.analyst.domain.tinvest.candles.Candle;
import ru.sewaiper.derivative.analyst.domain.market.tech.AtrMetrics;
import ru.sewaiper.derivative.analyst.domain.schedule.TradingHorizon;
import ru.sewaiper.derivative.analyst.properties.LlmProperties;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ForecastContractTest {
    private final LlmProperties properties = new LlmProperties();
    private final ForecastContract contract = new ForecastContract(properties);

    @Test
    void requestContainsExactFactsTargetsAndStructuredSchema() {
        properties.setModel("tested-model");
        var request = contract.request(preparation());
        var json = JsonMapper.builder().build().readTree(JsonMapper.builder().build().writeValueAsString(request));

        assertEquals("tested-model", request.model());
        assertEquals("json_schema", json.get("response_format").get("type").asText());
        assertEquals("centaur_terminal_price_range_v3", json.get("response_format").get("json_schema").get("name").asText());
        assertTrue(json.get("response_format").get("json_schema").get("strict").asBoolean());
        assertEquals("timeframeAssessment", json.get("response_format").get("json_schema")
                .get("schema").get("required").get(0).asText());
        var facts = JsonMapper.builder().build().readTree(request.messages().get(1).content());
        assertEquals("100.25", facts.get("referencePrice").get("value").asText());
        assertEquals("0.25", facts.get("priceIncrement").asText());
        assertEquals("3.50", facts.get("series").get("H1").get("metrics").get("ATR").get("atrPts").asText());
        assertEquals(0, facts.get("series").get("D1").get("completeCandlesCount").asInt());
        assertEquals(3, facts.get("horizons").size());
    }

    @Test
    void forecastsWithOnlyDailyCandlesAndMarksHourlyHistoryUnavailable() {
        properties.setModel("tested-model");
        var base = preparation();
        var source = base.marketContext().getSnapshot();
        var price = new Quotation(null, "101", 0);
        var candle = new Candle(price, price, price, price, "1",
                source.snapshotTimeUtc().minusSeconds(86400), true, null, null, null);
        var dailySnapshot = new InstrumentSnapshot(source.ticker(), source.snapshotTimeUtc(),
                source.passport(), Map.of(Timeframe.D1, new TInvestCandleData(Timeframe.D1, List.of(candle))),
                null, null, source.tradingMechanics());
        var dailyPreparation = new AnalysisPreparation(base.locator(), base.depth(),
                new MarketContext(dailySnapshot, new TInvestCandleData(Timeframe.D1, List.of(candle)).toBarSeries()),
                base.horizons());

        var request = contract.request(dailyPreparation);
        var facts = JsonMapper.builder().build().readTree(request.messages().get(1).content());
        assertEquals("D1", facts.get("referencePrice").get("timeframe").asText());
        assertEquals(0, facts.get("series").get("H1").get("completeCandlesCount").asInt());
        assertEquals(1, facts.get("series").get("D1").get("completeCandlesCount").asInt());
    }

    @Test
    void keepsDifferentDailyCloseAsContextWhileUsingOneHourlyReference() {
        properties.setModel("tested-model");
        var base = preparation();
        var source = base.marketContext().getSnapshot();
        var dailyPrice = new Quotation(null, "90", 0);
        var dailyCandle = new Candle(dailyPrice, dailyPrice, dailyPrice, dailyPrice, "1",
                source.snapshotTimeUtc().minusSeconds(86400), true, null, null, null);
        var snapshot = new InstrumentSnapshot(source.ticker(), source.snapshotTimeUtc(), source.passport(),
                Map.of(Timeframe.H1, source.candleData().get(Timeframe.H1),
                        Timeframe.D1, new TInvestCandleData(Timeframe.D1, List.of(dailyCandle))),
                null, null, source.tradingMechanics());
        var preparation = new AnalysisPreparation(base.locator(), base.depth(),
                new MarketContext(snapshot, source.candleData().get(Timeframe.H1).toBarSeries()), base.horizons());
        var facts = JsonMapper.builder().build().readTree(contract.request(preparation).messages().get(1).content());
        assertEquals("100.25", facts.get("referencePrice").get("value").asText());
        assertEquals("90", facts.get("series").get("D1").get("lastClosedPrice").asText());
        assertEquals(Timeframe.H1, contract.referencePrice(preparation).timeframe());
    }

    @Test
    void validatesRangesAgainstHorizonsAndPriceIncrement() {
        var valid = response("""
                {"timeframeAssessment": "H1 is weak; D1 is unavailable, so agreement cannot be assessed.", "hypotheses": [
                  {"tradingDays": 5, "type": "TERMINAL_PRICE_RANGE", "lowerDistancePts": "5.01", "upperDistancePts": "9.74", "reasoning": "EMA and ATR", "alternativeScenario": "range", "uncertaintyFactors": ["volatility"]},
                  {"tradingDays": 1, "type": "TERMINAL_PRICE_RANGE", "lowerDistancePts": "1.01", "upperDistancePts": "2.01", "reasoning": "EMA", "alternativeScenario": "fall", "uncertaintyFactors": []},
                  {"tradingDays": 10, "type": "TERMINAL_PRICE_RANGE", "lowerDistancePts": "10.01", "upperDistancePts": "19.74", "reasoning": "ATR", "alternativeScenario": "rise", "uncertaintyFactors": []}
                ]}
                """);
        var result = contract.validate(valid, contract.referencePrice(preparation()), new BigDecimal("0.25"));
        var hypotheses = result.hypotheses();
        assertEquals(List.of(1, 5, 10), hypotheses.stream().map(h -> h.tradingDays()).toList());
        assertEquals(new BigDecimal("99.00"), hypotheses.getFirst().lowerBound());
        assertEquals(new BigDecimal("102.50"), hypotheses.getFirst().upperBound());
        assertEquals("100.25", result.validation().get("referencePrice").asText());
        assertEquals("OUTWARD", result.validation().get("rounding").asText());

        assertThrows(IllegalArgumentException.class, () -> contract.validate(response(valid.choices().getFirst()
                .message().content().replace("5.01", "-5.01")), contract.referencePrice(preparation()), new BigDecimal("0.25")));
        assertThrows(IllegalArgumentException.class, () -> contract.validate(response(valid.choices().getFirst()
                .message().content().replace("\"tradingDays\": 5", "\"tradingDays\": 1")),
                contract.referencePrice(preparation()), new BigDecimal("0.25")));
        assertThrows(IllegalArgumentException.class, () -> contract.validate(response("not json"),
                contract.referencePrice(preparation()), new BigDecimal("0.25")));
        assertThrows(IllegalArgumentException.class, () -> contract.validate(response(valid.choices().getFirst()
                .message().content().replace("H1 is weak; D1 is unavailable, so agreement cannot be assessed.", " ")),
                contract.referencePrice(preparation()), new BigDecimal("0.25")));
    }

    private AnalysisPreparation preparation() {
        var at = Instant.parse("2026-10-17T12:00:00Z");
        var passport = new InstrumentPassport("TEST", "Test", AssetType.FUTURE,
                Provider.T_INVEST, "uid-test", "figi-test", "forts_futures");
        var mechanics = new TradingMechanics(1, 0.25, "RUB", true, true, false,
                null, null, 1.0, null, null);
        var price = new Quotation(null, "100", 250_000_000);
        var candle = new Candle(price, price, price, price, "1", at.minusSeconds(7200),
                true, null, null, null);
        var snapshot = new InstrumentSnapshot("TEST", at, passport,
                Map.of(Timeframe.H1, new TInvestCandleData(Timeframe.H1, List.of(candle))),
                null, null, mechanics);
        BarSeries bars = mock(BarSeries.class);
        Bar last = mock(Bar.class);
        when(bars.getBarCount()).thenReturn(200);
        when(bars.getLastBar()).thenReturn(last);
        var nums = DecimalNumFactory.getInstance();
        when(last.getClosePrice()).thenReturn(nums.numOf("100.25"));
        var context = new MarketContext(snapshot, bars);
        context.addTimeframeMetrics(Timeframe.H1, Map.of(ru.sewaiper.derivative.analyst.domain.market.tech.MetricName.ATR,
                new AtrMetrics(nums.numOf("3.50"), nums.numOf("3.49"), 28, 200, at)));
        var horizons = List.of(
                new TradingHorizon(1, LocalDate.parse("2026-10-19"), at.plusSeconds(86400)),
                new TradingHorizon(5, LocalDate.parse("2026-10-23"), at.plusSeconds(432000)),
                new TradingHorizon(10, LocalDate.parse("2026-10-30"), at.plusSeconds(864000)));
        return new AnalysisPreparation(new InstrumentLocator("uid-test", AssetType.FUTURE, Provider.T_INVEST),
                new SnapshotDepth(10), context, horizons);
    }

    private ChatCompletionResponse response(String content) {
        return new ChatCompletionResponse("response", "chat.completion", 1L, "tested-model", null,
                List.of(new ChatCompletionResponse.Choice(0,
                        new ChatCompletionResponse.Message("assistant", content, null, null, null),
                        "stop", null, null)), null);
    }
}
