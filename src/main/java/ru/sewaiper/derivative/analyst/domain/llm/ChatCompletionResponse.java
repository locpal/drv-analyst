package ru.sewaiper.derivative.analyst.domain.llm;


import tools.jackson.databind.JsonNode;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ChatCompletionResponse(String id,
                                     String object,
                                     Long created,
                                     String model,
                                     String systemFingerprint,
                                     List<Choice> choices,
                                     Usage usage) {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Choice(Integer index,
                         Message message,
                         String finishReason,
                         String nativeFinishReason,
                         ApiError error) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Message(String role,
                          String content,
                          String reasoning,
                          List<JsonNode> reasoningDetails,
                          List<ToolCall> toolCalls) {
    }

    public record ToolCall(String id,
                           String type,
                           FunctionCall function) {
    }

    public record FunctionCall(String name,
                               String arguments) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Usage(Long promptTokens,
                        Long completionTokens,
                        Long totalTokens,
                        BigDecimal cost,
                        BigDecimal costRub,
                        BigDecimal balance,
                        PromptTokensDetails promptTokensDetails,
                        CompletionTokensDetails completionTokensDetails,
                        ServerToolUse serverToolUse) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record PromptTokensDetails(Long cachedTokens,
                                      Long cacheWriteTokens) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record CompletionTokensDetails(Long reasoningTokens) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ServerToolUse(Integer webSearchRequests) {
    }

    public record ApiError(Integer code,
                           String message,
                           Map<String, JsonNode> metadata) {
    }
}

