package ru.sewaiper.derivative.analyst.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.sewaiper.derivative.analyst.domain.analysis.LlmExchange;
import ru.sewaiper.derivative.analyst.domain.analysis.MarketHypothesis;
import ru.sewaiper.derivative.analyst.domain.llm.ChatCompletionRequest;
import ru.sewaiper.derivative.analyst.domain.llm.ChatCompletionResponse;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ForecastDao {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private final JdbcTemplate jdbc;

    public ForecastDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void saveRequest(UUID analysisId, ChatCompletionRequest request, Instant requestedAt) {
        jdbc.update("""
                INSERT INTO llm_exchanges (analysis_id, requested_at_utc, request_json)
                VALUES (?, ?, ?)
                """, analysisId.toString(), requestedAt.toString(), JSON.writeValueAsString(request));
    }

    public void saveResponse(UUID analysisId, ChatCompletionResponse response, Instant respondedAt) {
        ChatCompletionResponse.Choice choice = response.choices() == null || response.choices().isEmpty()
                ? null : response.choices().getFirst();
        if (jdbc.update("""
                UPDATE llm_exchanges SET responded_at_utc = ?, response_json = ?, model = ?,
                    finish_reason = ?, usage_json = ?, content = ?
                WHERE analysis_id = ? AND response_json IS NULL
                """, respondedAt.toString(), JSON.writeValueAsString(response), response.model(),
                choice == null ? null : choice.finishReason(),
                response.usage() == null ? null : JSON.writeValueAsString(response.usage()),
                choice == null || choice.message() == null ? null : choice.message().content(),
                analysisId.toString()) != 1) {
            throw new IllegalStateException("LLM response already saved or request missing");
        }
    }

    public void saveValidationError(UUID analysisId, String message) {
        jdbc.update("UPDATE llm_exchanges SET validation_error = ?, validation_json = ? WHERE analysis_id = ?",
                message, JSON.writeValueAsString(java.util.Map.of("status", "INVALID", "error", message)),
                analysisId.toString());
    }

    public void saveValidation(UUID analysisId, JsonNode validation) {
        if (jdbc.update("UPDATE llm_exchanges SET validation_json = ? WHERE analysis_id = ? AND response_json IS NOT NULL",
                JSON.writeValueAsString(validation), analysisId.toString()) != 1) {
            throw new IllegalStateException("LLM response missing for validation");
        }
    }

    @Transactional
    public void saveHypotheses(UUID analysisId, List<MarketHypothesis> hypotheses, Instant finishedAt) {
        if (hypotheses.size() != 3) {
            throw new IllegalArgumentException("Exactly three hypotheses are required");
        }
        for (MarketHypothesis hypothesis : hypotheses) {
            jdbc.update("""
                    INSERT INTO market_hypotheses (analysis_id, trading_days, type, lower_bound, upper_bound,
                        reasoning, alternative_scenario, uncertainty_factors_json)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """, analysisId.toString(), hypothesis.tradingDays(), hypothesis.type(),
                    hypothesis.lowerBound().toPlainString(), hypothesis.upperBound().toPlainString(),
                    hypothesis.reasoning(), hypothesis.alternativeScenario(),
                    JSON.writeValueAsString(hypothesis.uncertaintyFactors()));
        }
        if (jdbc.update("""
                UPDATE analysis_runs SET status = 'FORECASTED', finished_at_utc = ?
                WHERE id = ? AND status = 'FORECASTING'
                """, finishedAt.toString(), analysisId.toString()) != 1) {
            throw new IllegalStateException("Analysis is not ready for forecast completion");
        }
    }

    public Optional<LlmExchange> find(UUID analysisId) {
        var rows = jdbc.query("SELECT * FROM llm_exchanges WHERE analysis_id = ?", (rs, row) -> {
            var hypotheses = jdbc.query("""
                    SELECT * FROM market_hypotheses WHERE analysis_id = ? ORDER BY trading_days
                    """, (hypothesisRs, hypothesisRow) -> new MarketHypothesis(
                    hypothesisRs.getInt("trading_days"), hypothesisRs.getString("type"),
                    new BigDecimal(hypothesisRs.getString("lower_bound")),
                    new BigDecimal(hypothesisRs.getString("upper_bound")),
                    hypothesisRs.getString("reasoning"), hypothesisRs.getString("alternative_scenario"),
                    readStrings(hypothesisRs.getString("uncertainty_factors_json"))), analysisId.toString());
            return new LlmExchange(Instant.parse(rs.getString("requested_at_utc")),
                    read(rs.getString("request_json")), parseInstant(rs.getString("responded_at_utc")),
                    read(rs.getString("response_json")), rs.getString("model"),
                    rs.getString("finish_reason"), read(rs.getString("usage_json")),
                    rs.getString("content"), rs.getString("validation_error"),
                    read(rs.getString("validation_json")), hypotheses);
        }, analysisId.toString());
        return rows.stream().findFirst();
    }

    private List<String> readStrings(String json) {
        List<String> result = new ArrayList<>();
        for (JsonNode item : JSON.readTree(json)) {
            result.add(item.asText());
        }
        return List.copyOf(result);
    }

    private JsonNode read(String json) {
        return json == null ? null : JSON.readTree(json);
    }

    private Instant parseInstant(String value) {
        return value == null ? null : Instant.parse(value);
    }
}
