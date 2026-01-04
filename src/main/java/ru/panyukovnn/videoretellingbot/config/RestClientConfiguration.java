package ru.panyukovnn.videoretellingbot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class RestClientConfiguration {

    @Value("${retelling.rest-timeout-ms:60000}")
    private Integer restTimeoutMs;

    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(restTimeoutMs))
            .build();
    }

    @Bean
    public RestClient.Builder restClientBuilder(HttpClient httpClient) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(restTimeoutMs);

        return RestClient.builder().requestFactory(requestFactory);
    }
}