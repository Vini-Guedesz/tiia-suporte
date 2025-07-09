package org.project.tiiasuporte.config;

import org.apache.commons.net.whois.WhoisClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public WhoisClient whoisClient() {
        return new WhoisClient();
    }
}
