package ru.sewaiper.derivative.analyst.domain.analysis;

import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.List;

public record LlmExchange(Instant requestedAtUtc,
                          JsonNode request,
                          Instant respondedAtUtc,
                          JsonNode response,
                          String model,
                          String finishReason,
                          JsonNode usage,
                          String content,
                          String validationError,
                          JsonNode validation,
                          List<MarketHypothesis> hypotheses) {
}
