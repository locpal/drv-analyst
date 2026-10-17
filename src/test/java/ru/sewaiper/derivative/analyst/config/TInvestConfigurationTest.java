package ru.sewaiper.derivative.analyst.config;

import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import feign.RetryableException;
import org.junit.jupiter.api.Test;
import ru.sewaiper.derivative.analyst.properties.TInvestProperties;

import javax.net.ssl.SSLHandshakeException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TInvestConfigurationTest {

    private final TInvestConfiguration configuration = new TInvestConfiguration();

    @Test
    void requiresTokenAndAddsBearerHeader() {
        TInvestProperties properties = new TInvestProperties();
        assertThrows(IllegalStateException.class, () -> configuration.tokenInterceptor(properties));

        properties.setToken(" test-token ");
        RequestTemplate template = new RequestTemplate();
        configuration.tokenInterceptor(properties).apply(template);
        assertEquals("Bearer test-token", template.headers().get("Authorization").iterator().next());
    }

    @Test
    void retriesServerErrorsAndTimeoutsOnlyThreeTimes() {
        Request request = Request.create(Request.HttpMethod.POST, "https://invest-public-api.tbank.ru/rest",
                Map.of(), null, StandardCharsets.UTF_8);
        Response response = Response.builder().status(503).reason("Unavailable").request(request).build();
        RetryableException serverError = assertInstanceOf(RetryableException.class,
                configuration.tInvestErrorDecoder().decode("schedule", response));
        var retryer = configuration.tInvestRetryer().clone();
        retryer.continueOrPropagate(serverError);
        retryer.continueOrPropagate(serverError);
        assertThrows(RetryableException.class, () -> retryer.continueOrPropagate(serverError));

        RetryableException timeout = new RetryableException(-1, "timeout", Request.HttpMethod.POST,
                new SocketTimeoutException(), (Long) null, request);
        configuration.tInvestRetryer().clone().continueOrPropagate(timeout);

        RetryableException tlsFailure = new RetryableException(-1, "TLS", Request.HttpMethod.POST,
                new SSLHandshakeException("untrusted"), (Long) null, request);
        assertThrows(RetryableException.class,
                () -> configuration.tInvestRetryer().clone().continueOrPropagate(tlsFailure));
    }

    @Test
    void loadsPinnedCertificate() {
        configuration.feignClient();
    }
}
