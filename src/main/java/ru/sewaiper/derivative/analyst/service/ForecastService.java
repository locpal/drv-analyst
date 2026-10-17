package ru.sewaiper.derivative.analyst.service;

import org.springframework.stereotype.Service;
import ru.sewaiper.derivative.analyst.clients.LlmClient;
import ru.sewaiper.derivative.analyst.dao.ForecastDao;
import ru.sewaiper.derivative.analyst.domain.analysis.AnalysisPreparation;
import ru.sewaiper.derivative.analyst.domain.analysis.LlmExchange;
import ru.sewaiper.derivative.analyst.domain.analysis.MarketHypothesis;
import ru.sewaiper.derivative.analyst.domain.llm.ChatCompletionRequest;
import ru.sewaiper.derivative.analyst.domain.llm.ChatCompletionResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ForecastService {
    private final LlmClient client;
    private final ForecastContract contract;
    private final ForecastDao dao;
    private final AnalysisStepJournal journal;

    public ForecastService(LlmClient client, ForecastContract contract, ForecastDao dao,
                           AnalysisStepJournal journal) {
        this.client = client;
        this.contract = contract;
        this.dao = dao;
        this.journal = journal;
    }

    public void forecast(UUID analysisId, AnalysisPreparation preparation) {
        ChatCompletionRequest request = contract.request(preparation);
        saveRequest(analysisId, request);
        ChatCompletionResponse response = requestForecast(analysisId, request);
        BigDecimal increment = BigDecimal.valueOf(preparation.marketContext().getSnapshot()
                .tradingMechanics().minPriceIncrement());
        var reference = contract.referencePrice(preparation);
        var validated = validateForecast(analysisId, response, reference, increment);
        saveForecast(analysisId, validated.hypotheses());
    }

    private void saveRequest(UUID analysisId, ChatCompletionRequest request) {
        journal.run(
                analysisId,
                "SAVE_LLM_REQUEST",
                Map.of("analysisId", analysisId),
                () -> dao.saveRequest(analysisId, request, Instant.now()),
                Map.of("model", request.model(), "requestId", analysisId)
        );
    }

    private ChatCompletionResponse requestForecast(UUID analysisId, ChatCompletionRequest request) {
        return journal.run(
                analysisId,
                "REQUEST_FORECAST",
                Map.of("requestId", analysisId, "model", request.model()),
                () -> {
                    var received = client.completion(request);
                    if (received == null) {
                        throw new IllegalStateException("LLM returned no response");
                    }
                    dao.saveResponse(analysisId, received, Instant.now());
                    return received;
                },
                received -> Map.of("exchangeId", analysisId)
        );
    }

    private ForecastContract.ValidationResult validateForecast(UUID analysisId, ChatCompletionResponse response,
                                                               ForecastContract.ReferencePrice reference,
                                                               BigDecimal increment) {
        return journal.run(
                analysisId,
                "VALIDATE_FORECAST",
                Map.of("exchangeId", analysisId),
                () -> {
                    try {
                        var result = contract.validate(response, reference, increment);
                        dao.saveValidation(analysisId, result.validation());
                        return result;
                    } catch (RuntimeException e) {
                        dao.saveValidationError(analysisId, e.getMessage());
                        throw e;
                    }
                },
                result -> Map.of(
                        "tradingDays", result.hypotheses().stream().map(MarketHypothesis::tradingDays).toList()
                )
        );
    }

    private void saveForecast(UUID analysisId, List<MarketHypothesis> hypotheses) {
        journal.run(
                analysisId,
                "SAVE_FORECAST",
                Map.of("exchangeId", analysisId),
                () -> dao.saveHypotheses(analysisId, hypotheses, Instant.now()),
                Map.of("hypothesisCount", hypotheses.size(), "status", "FORECASTED")
        );
    }

    public Optional<LlmExchange> find(UUID analysisId) {
        return dao.find(analysisId);
    }
}
