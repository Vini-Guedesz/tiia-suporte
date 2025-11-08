package org.project.tiiasuporte.config;

import org.apache.commons.net.whois.WhoisClient;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.async.TimeoutCallableProcessingInterceptor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;

@Configuration
public class AppConfig implements WebMvcConfigurer {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));

        return builder
                .requestFactory(() -> factory)
                .build();
    }

    @Bean
    public WhoisClient whoisClient() {
        WhoisClient client = new WhoisClient();
        // Define timeout para o WhoisClient (em milissegundos)
        client.setDefaultTimeout(10000); // 10 segundos
        return client;
    }

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        // Aumentar o timeout para 120 segundos (120000ms) para suportar traceroute longo
        configurer.setDefaultTimeout(120000);
        configurer.registerCallableInterceptors(timeoutInterceptor());
    }

    @Bean
    public TimeoutCallableProcessingInterceptor timeoutInterceptor() {
        return new TimeoutCallableProcessingInterceptor();
    }
}
