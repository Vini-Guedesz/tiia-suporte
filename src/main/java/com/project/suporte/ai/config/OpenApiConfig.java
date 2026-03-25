package com.project.suporte.ai.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Suporte AI API")
                        .version("1.0")
                        .description("API de apoio a operações de suporte técnico, com ferramentas de DNS, geolocalização, whois, ping, traceroute e port scan controlado.")
                        .license(new License().name("Uso interno")));
    }
}
