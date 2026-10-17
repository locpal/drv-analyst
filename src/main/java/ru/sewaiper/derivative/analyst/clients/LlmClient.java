package ru.sewaiper.derivative.analyst.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.sewaiper.derivative.analyst.config.LlmClientConfiguration;
import ru.sewaiper.derivative.analyst.domain.llm.ChatCompletionRequest;
import ru.sewaiper.derivative.analyst.domain.llm.ChatCompletionResponse;

@FeignClient(
        name = "llm-client",
        url = "${clients.llm.url}",
        configuration = LlmClientConfiguration.class
)
public interface LlmClient {

    @PostMapping(path = "/v1/chat/completions")
    ChatCompletionResponse completion(@RequestBody ChatCompletionRequest request);
}
