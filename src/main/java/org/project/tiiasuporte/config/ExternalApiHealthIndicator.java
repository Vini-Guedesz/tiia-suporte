package org.project.tiiasuporte.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class ExternalApiHealthIndicator implements HealthIndicator {

    private final RestTemplate restTemplate;

    public ExternalApiHealthIndicator(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public Health health() {
        try {
            // Test if ip-api.com is responding
            String response = restTemplate.getForObject("http://ip-api.com/json/8.8.8.8?fields=status", String.class);

            if (response != null && response.contains("success")) {
                return Health.up()
                    .withDetail("ip-api.com", "Available")
                    .build();
            } else {
                return Health.down()
                    .withDetail("ip-api.com", "Unexpected response")
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("ip-api.com", "Unavailable")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
