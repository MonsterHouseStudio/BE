package com.monsterhouse.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class RestClientConfig {
    private final LineProperties lineProperties;
    @Bean(name = "lineRestClient")
    public RestClient lineRestClient(){
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(lineProperties.connectTimeoutMs()));
        factory.setReadTimeout(Duration.ofMillis(lineProperties.readTimeoutMs()));
        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }
}
