package ru.sewaiper.derivative.analyst.domain.analysis;

import tools.jackson.databind.JsonNode;

import java.time.Instant;

public record AnalysisStep(long id, String name, String status, Instant startedAtUtc,
                           Instant finishedAtUtc, JsonNode input, JsonNode output, String errorMessage) {
}
