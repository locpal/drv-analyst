package ru.sewaiper.derivative.analyst.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import ru.sewaiper.derivative.analyst.properties.LlmProperties;

public class LlmClientConfiguration {

    @Bean
    public RequestInterceptor tokenInterceptor(LlmProperties properties) {
        String token = properties.getToken();
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("LLM API token is missing (LLM_TOKEN)");
        }
        String bearerToken = "Bearer " + token.strip();
        return template -> template.header(HttpHeaders.AUTHORIZATION, bearerToken);
    }
}
