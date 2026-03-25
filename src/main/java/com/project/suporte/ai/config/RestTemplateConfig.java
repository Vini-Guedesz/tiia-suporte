package com.project.suporte.ai.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder, DiagnosticsProperties properties) {
        return builder
                .setConnectTimeout(Duration.ofMillis(properties.getGeolocation().getConnectTimeoutMs()))
                .setReadTimeout(Duration.ofMillis(properties.getGeolocation().getReadTimeoutMs()))
                .build();
    }
}
