package ru.sewaiper.derivative.analyst.config;

import feign.Client;
import feign.RequestInterceptor;
import feign.RetryableException;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import ru.sewaiper.derivative.analyst.properties.TInvestProperties;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateFactory;

// Registered by TInvestClient only; do not component-scan this into every Feign client.
public class TInvestConfiguration {

    @Bean
    public RequestInterceptor tokenInterceptor(TInvestProperties properties) {
        String token = properties.getToken();
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("T-Invest token is missing (T_INVEST_TOKEN)");
        }
        String bearerToken = "Bearer " + token.strip();
        return template -> template.header(HttpHeaders.AUTHORIZATION, bearerToken);
    }

    @Bean
    public Client feignClient() {
        try {
            X509Certificate certificate;
            try (InputStream input = new ClassPathResource("tbank.ru.pem").getInputStream()) {
                certificate = (X509Certificate) CertificateFactory.getInstance("X.509")
                        .generateCertificate(input);
            }
            certificate.checkValidity();
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);
            trustStore.setCertificateEntry("tbank.ru", certificate);
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
            return new Client.Default(sslContext.getSocketFactory(), HttpsURLConnection.getDefaultHostnameVerifier());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize T-Invest TLS trust store", e);
        }
    }

    @Bean
    public ErrorDecoder tInvestErrorDecoder() {
        ErrorDecoder fallback = new ErrorDecoder.Default();
        return (methodKey, response) -> {
            if (response.status() >= 500 && response.status() < 600) {
                return new RetryableException(response.status(), "T-Invest HTTP " + response.status(),
                        response.request().httpMethod(), (Long) null, response.request());
            }
            return fallback.decode(methodKey, response);
        };
    }

    @Bean
    public Retryer tInvestRetryer() {
        return new TransientFailureRetryer();
    }

    private static final class TransientFailureRetryer implements Retryer {
        private final Retryer delegate = new Retryer.Default(200, 1000, 3);

        @Override
        public void continueOrPropagate(RetryableException exception) {
            if (exception.status() < 500 && !(exception.getCause() instanceof SocketTimeoutException)) {
                throw exception;
            }
            delegate.continueOrPropagate(exception);
        }

        @Override
        public Retryer clone() {
            return new TransientFailureRetryer();
        }
    }
}
