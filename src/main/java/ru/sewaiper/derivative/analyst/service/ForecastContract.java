package ru.sewaiper.derivative.analyst.service;

import org.springframework.stereotype.Component;
import ru.sewaiper.derivative.analyst.dao.AnalysisMetricJson;
import ru.sewaiper.derivative.analyst.domain.analysis.AnalysisPreparation;
import ru.sewaiper.derivative.analyst.domain.analysis.MarketHypothesis;
import ru.sewaiper.derivative.analyst.domain.llm.ChatCompletionRequest;
import ru.sewaiper.derivative.analyst.domain.llm.ChatCompletionResponse;
import ru.sewaiper.derivative.analyst.domain.market.MarketContext;
import ru.sewaiper.derivative.analyst.domain.market.snapshot.InstrumentSnapshot;
import ru.sewaiper.derivative.analyst.domain.market.snapshot.TInvestCandleData;
import ru.sewaiper.derivative.analyst.domain.market.enums.Timeframe;
import ru.sewaiper.derivative.analyst.domain.market.tech.MetricName;
import ru.sewaiper.derivative.analyst.domain.schedule.TradingHorizon;
import ru.sewaiper.derivative.analyst.properties.LlmProperties;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ForecastContract {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Set<Integer> DAYS = Set.of(1, 5, 10);

    // Задаёт смысл прогноза: цена закрытия в целевой момент, а не касание уровня до него.
    private static final String SYSTEM_PROMPT = """
            When constructing forecast ranges, distinguish observed market characteristics from assumptions about future price behavior.
            Use the supplied indicators to assess the current price dynamics, momentum, volatility and market structure, but do not treat them as direct predictors of future prices.
            First, in timeframeAssessment, describe where H1 and D1 observations agree or conflict, and what each history can support. If either history is unavailable or too short, state that limitation without inventing a comparison.
            Then choose the three forecast ranges. Explain the qualitative reasons for each range's width and asymmetry. If ATR or another indicator is used as a scale reference, explicitly identify the choice as heuristic rather than statistically calibrated. Do not invent a precise formula or exact numerical derivation for heuristically chosen distances.
            Consider the H1 and D1 candle histories separately for each forecast horizon. Do not assume that a missing or short D1 history makes a horizon impossible, or that an H1 ATR directly measures a multi-day price movement.
            Use the single referencePrice supplied by Java for all three horizons. H1 and D1 closing prices describe their respective histories; never use either series close as a separate starting price.
            Return nonnegative lowerDistancePts and upperDistancePts from referencePrice for each horizon. Java calculates and rounds the final price bounds. Explain the qualitative reasons for the distance and asymmetry without calculating final prices in the explanation.
            Identify which timeframe and observed values inform each range. Longer horizons should reflect additional uncertainty unless the supplied evidence justifies otherwise.
            Do not invent statistical probabilities, historical frequencies, predictive accuracy or relationships that are not supported by the supplied data.
            When the evidence is weak or conflicting, express that uncertainty through the explanation and forecast range rather than manufacturing confidence.
            Before interpreting trend direction, verify the sign of each supplied EMA spread. A positive EMA spread means EMA20 is above EMA50, even if the current price is below both. Explicitly distinguish EMA ordering, price position, and indicator changes. Do not describe conflicting timeframe observations as agreement.
            """;

    // Ограничивает формат ответа у провайдера; ниже validate() независимо проверяет результат в Java.
    private static final JsonNode SCHEMA = JSON.readTree("""
            {
              "type": "object",
              "additionalProperties": false,
              "required": ["timeframeAssessment", "hypotheses"],
              "properties": {
                "timeframeAssessment": {"type": "string"},
                "hypotheses": {
                  "type": "array", "minItems": 3, "maxItems": 3,
                  "items": {
                    "type": "object", "additionalProperties": false,
                    "required": ["tradingDays", "type", "lowerDistancePts", "upperDistancePts",
                      "reasoning", "alternativeScenario", "uncertaintyFactors"],
                    "properties": {
                      "tradingDays": {"type": "integer", "enum": [1, 5, 10]},
                      "type": {"type": "string", "enum": ["TERMINAL_PRICE_RANGE"]},
                      "lowerDistancePts": {"type": "string"},
                      "upperDistancePts": {"type": "string"},
                      "reasoning": {"type": "string"},
                      "alternativeScenario": {"type": "string"},
                      "uncertaintyFactors": {"type": "array", "items": {"type": "string"}}
                    }
                  }
                }
              }
            }
            """);

    private final LlmProperties properties;

    public ForecastContract(LlmProperties properties) {
        this.properties = properties;
    }

    public ChatCompletionRequest request(AnalysisPreparation preparation) {
        // Не отправляем запрос, если модель или заранее рассчитанные целевые дни не определены.
        String model = properties.getModel();
        if (model == null || model.isBlank()) {
            throw new IllegalStateException("LLM model is missing (LLM_MODEL)");
        }
        if (preparation.horizons().size() != 3
                || !preparation.horizons().stream().map(TradingHorizon::tradingDays).collect(Collectors.toSet()).equals(DAYS)) {
            throw new IllegalArgumentException("Forecast requires horizons 1, 5 and 10");
        }
        var context = preparation.marketContext();
        var snapshot = context.getSnapshot();
        if (snapshot.tradingMechanics() == null || snapshot.tradingMechanics().minPriceIncrement() <= 0) {
            throw new IllegalArgumentException("Instrument price increment is unavailable");
        }
        if (snapshot.candleData().values().stream().allMatch(data -> data.candles().isEmpty())) {
            throw new IllegalArgumentException("Forecast requires closed candles");
        }
        // Пользовательское сообщение содержит только зафиксированные на момент анализа рыночные факты.
        var facts = prepareFacts(preparation);
        var messages = List.of(
                new ChatCompletionRequest.Message("system", SYSTEM_PROMPT),
                new ChatCompletionRequest.Message("user", JSON.writeValueAsString(facts)));
        // Имя схемы сохраняется в полном запросе и позволяет отличать версии контракта.
        var responseFormat = new ChatCompletionRequest.ResponseFormat(
                "json_schema",
                new ChatCompletionRequest.JsonSchema(
                        "centaur_terminal_price_range_v3", true, SCHEMA
                )
        );
        // Формируем запрос
        return new ChatCompletionRequest(
                model.strip(),
                messages,
                false,
                2048,
                0.0,
                null,
                null,
                responseFormat,
                null
        );
    }

    public record ReferencePrice(BigDecimal value, Timeframe timeframe, String candleStartUtc) {}

    public record ValidationResult(List<MarketHypothesis> hypotheses, JsonNode validation) {}

    public ReferencePrice referencePrice(AnalysisPreparation preparation) {
        var candleData = preparation.marketContext().getSnapshot().candleData();
        for (Timeframe timeframe : List.of(Timeframe.H1, Timeframe.D1)) {
            var data = candleData.get(timeframe);
            if (data != null && !data.candles().isEmpty()) {
                var candle = data.candles().getLast();
                return new ReferencePrice(candle.close().asBigDecimal(), timeframe, candle.time().toString());
            }
        }
        throw new IllegalArgumentException("Forecast requires closed candles");
    }

    public ValidationResult validate(ChatCompletionResponse response, ReferencePrice reference, BigDecimal increment) {
        // Сначала проверяем, что генерация завершилась и вернула единственный содержательный ответ.
        if (response == null || response.choices() == null || response.choices().size() != 1) {
            throw new IllegalArgumentException("LLM must return exactly one choice");
        }
        var choice = response.choices().getFirst();
        if (choice.error() != null || !"stop".equals(choice.finishReason()) || choice.message() == null
                || choice.message().content() == null || choice.message().content().isBlank()) {
            throw new IllegalArgumentException("LLM response has no complete JSON content");
        }
        JsonNode root;
        try {
            root = JSON.readTree(choice.message().content());
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("LLM content is not valid JSON", e);
        }
        JsonNode items = root == null ? null : root.get("hypotheses");
        if (root == null || !root.isObject() || root.size() != 2
                || items == null || !items.isArray() || items.size() != 3) {
            throw new IllegalArgumentException("LLM response must contain three hypotheses");
        }
        text(root, "timeframeAssessment");
        if (increment == null || increment.signum() <= 0 || reference == null
                || reference.value() == null || reference.value().signum() <= 0) {
            throw new IllegalArgumentException("Instrument price increment is unavailable");
        }
        // Все три диапазона строятся от одной зафиксированной цены. Округление наружу не сужает диапазон модели.
        List<MarketHypothesis> hypotheses = new ArrayList<>();
        List<Map<String, Object>> calculations = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        for (JsonNode item : items) {
            if (!item.isObject() || item.size() != 7) {
                throw new IllegalArgumentException("Hypothesis must be an object");
            }
            JsonNode daysNode = item.get("tradingDays");
            if (daysNode == null || !daysNode.isIntegralNumber()) {
                throw new IllegalArgumentException("Invalid tradingDays");
            }
            int days = daysNode.asInt();
            if (!daysNode.asString().equals(Integer.toString(days)) || !DAYS.contains(days) || !seen.add(days)) {
                throw new IllegalArgumentException("Duplicate or unexpected forecast horizon: " + days);
            }
            if (!"TERMINAL_PRICE_RANGE".equals(text(item, "type"))) {
                throw new IllegalArgumentException("Unsupported hypothesis type");
            }
            BigDecimal down = decimal(item, "lowerDistancePts");
            BigDecimal up = decimal(item, "upperDistancePts");
            if (down.signum() < 0 || up.signum() < 0 || (down.signum() == 0 && up.signum() == 0)) {
                throw new IllegalArgumentException("Invalid distances for horizon " + days);
            }
            BigDecimal rawLower = reference.value().subtract(down);
            BigDecimal rawUpper = reference.value().add(up);
            BigDecimal lower = rawLower.divide(increment, 0, RoundingMode.FLOOR).multiply(increment);
            BigDecimal upper = rawUpper.divide(increment, 0, RoundingMode.CEILING).multiply(increment);
            if (lower.signum() <= 0 || upper.compareTo(lower) <= 0) {
                throw new IllegalArgumentException("Invalid price range for horizon " + days);
            }
            calculations.add(Map.of("tradingDays", days,
                    "lowerDistancePts", down.toPlainString(), "upperDistancePts", up.toPlainString(),
                    "rawLowerBound", rawLower.toPlainString(), "rawUpperBound", rawUpper.toPlainString(),
                    "lowerBound", lower.toPlainString(), "upperBound", upper.toPlainString()));
            JsonNode factors = item.get("uncertaintyFactors");
            if (factors == null || !factors.isArray()) {
                throw new IllegalArgumentException("uncertaintyFactors must be an array");
            }
            List<String> uncertainties = new ArrayList<>();
            for (JsonNode factor : factors) {
                if (!factor.isString() || factor.asString().isBlank()) {
                    throw new IllegalArgumentException("Invalid uncertainty factor");
                }
                uncertainties.add(factor.asString());
            }
            hypotheses.add(new MarketHypothesis(days, "TERMINAL_PRICE_RANGE", lower, upper,
                    text(item, "reasoning"), text(item, "alternativeScenario"), uncertainties));
        }
        hypotheses.sort(java.util.Comparator.comparingInt(MarketHypothesis::tradingDays));
        return new ValidationResult(List.copyOf(hypotheses), JSON.valueToTree(Map.of(
                "status", "VALID", "contract", "centaur_terminal_price_range_v3",
                "referencePrice", reference.value().stripTrailingZeros().toPlainString(),
                "referenceTimeframe", reference.timeframe().name(),
                "referenceCandleStartUtc", reference.candleStartUtc(),
                "priceIncrement", increment.toPlainString(), "rounding", "OUTWARD",
                "calculations", calculations)));
    }

    private Map<String, Object> prepareFacts(AnalysisPreparation preparation) {
        MarketContext context = preparation.marketContext();
        InstrumentSnapshot snapshot = context.getSnapshot();
        // Таймфрейм указывается для каждой серии, а не для инструмента целиком.
        var instruments = Map.of(
                "ticker", snapshot.ticker(),
                "providerId", snapshot.passport().providerId(),
                "assetType", snapshot.passport().assetType().name(),
                "exchange", snapshot.passport().exchange()
        );
        // Целевые моменты определены Java до вызова модели; Gemini не выбирает их самостоятельно.
        var horizons = preparation.horizons()
                .stream()
                .map(h -> Map.of(
                        "tradingDays", h.tradingDays(),
                        "tradingDate", h.sessionDate().toString(),
                        "targetAtUtc", h.targetAtUtc().toString())
                ).toList();
        // Значения DecimalNum передаются строками без перехода через JSON-числа; шаг берётся из TradingMechanics.
        Map<String, Object> series = new LinkedHashMap<>();
        for (Timeframe timeframe : List.of(Timeframe.H1, Timeframe.D1)) {
            TInvestCandleData candles = snapshot.candleData().get(timeframe);
            Map<String, Object> facts = new LinkedHashMap<>();
            int count = candles == null ? 0 : candles.candles().size();
            facts.put("completeCandlesCount", count);
            if (count > 0) {
                var last = candles.candles().getLast();
                facts.put("lastClosedPrice", last.close().asBigDecimal().stripTrailingZeros().toPlainString());
                facts.put("lastCandleStartUtc", last.time().toString());
            }
            var indicators = context.getTimeframeMetrics().getOrDefault(timeframe, Map.of());
            facts.put("metrics", indicators.isEmpty() ? Map.of()
                    : AnalysisMetricJson.decode(AnalysisMetricJson.encode(indicators)));
            series.put(timeframe.name(), facts);
        }
        ReferencePrice reference = referencePrice(preparation);
        Map<String, String> metricReferences = context.getMetrics().containsKey(MetricName.LIQUIDITY)
                && context.getTimeframeMetrics().getOrDefault(Timeframe.H1, Map.of()).containsKey(MetricName.ATR)
                ? Map.of("LIQUIDITY.spreadAtrPct", "H1.ATR.atrPts") : Map.of();
        return Map.of(
                "instrument", instruments,
                "snapshotAtUtc", snapshot.snapshotTimeUtc().toString(),
                "referencePrice", Map.of("value", reference.value().stripTrailingZeros().toPlainString(),
                        "timeframe", reference.timeframe().name(),
                        "candleStartUtc", reference.candleStartUtc()),
                "priceIncrement", BigDecimal.valueOf(snapshot.tradingMechanics().minPriceIncrement()).toPlainString(),
                "series", series,
                "commonMetrics", context.getMetrics().isEmpty() ? Map.of()
                        : AnalysisMetricJson.decode(AnalysisMetricJson.encode(context.getMetrics())),
                "metricReferences", metricReferences,
                "skippedInputs", preparation.depth().skip().stream().map(Enum::name).sorted().toList(),
                "horizons", horizons
        );
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isString() || value.asString().isBlank()) {
            throw new IllegalArgumentException("Missing or blank " + field);
        }
        return value.asString();
    }

    private BigDecimal decimal(JsonNode node, String field) {
        try {
            return new BigDecimal(text(node, field));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid decimal " + field, e);
        }
    }
}
