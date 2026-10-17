package ru.sewaiper.derivative.analyst.domain.llm;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ChatCompletionRequest(String model,
                                    List<Message> messages,
                                    Boolean stream,
                                    Integer maxTokens,
                                    Double temperature,
                                    Double topP,
                                    Integer seed,
                                    ResponseFormat responseFormat,
                                    Reasoning reasoning) {
    public record Message(String role,
                          String content) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ResponseFormat(String type,
                                 JsonSchema jsonSchema) {
    }

    public record JsonSchema(String name,
                             Boolean strict,
                             JsonNode schema) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Reasoning(String effort,
                            Integer maxTokens,
                            Boolean exclude,
                            Boolean enabled) {
    }
}