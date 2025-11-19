package org.project.tiiasuporte.geolocalizacao;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.project.tiiasuporte.exceptions.ExternalServiceException;
import org.project.tiiasuporte.exceptions.InvalidIpAddressException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.project.tiiasuporte.util.ValidationUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class GeoService {

    private static final Logger logger = LoggerFactory.getLogger(GeoService.class);
    private static final int CONNECTION_TIMEOUT_MS = 3000;
    private static final int READ_TIMEOUT_MS = 5000;

    private final RestTemplate restTemplate;

    @Value("${geolocalizacao.api.url}")
    private String geolocalizacaoApiUrl;

    @Autowired
    public GeoService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Cacheable(value = "geolocalizacao", key = "#ip")
    @CircuitBreaker(name = "ipApi", fallbackMethod = "getGeoFallback")
    @RateLimiter(name = "ipApi")
    public String obterLocalizacao(String ip) {
        long startTime = System.currentTimeMillis();

        if (!ValidationUtils.isValidIpAddress(ip)) {
            logger.warn("Tentativa de obter geolocalização com IP inválido: {}", ip);
            throw new InvalidIpAddressException("Endereço IP inválido: " + ip);
        }

        String url = geolocalizacaoApiUrl + ip;
        try {
            String response = restTemplate.getForObject(url, String.class);
            long duration = System.currentTimeMillis() - startTime;
            logger.debug("Geolocalização para IP {} concluída em {}ms", ip, duration);
            return response;
        } catch (HttpClientErrorException e) {
            logger.error("Erro ao consultar a API de geolocalização para IP {}: {}", ip, e.getStatusCode(), e);
            throw new ExternalServiceException(String.format("Erro ao consultar a API de geolocalização: %s", e.getStatusCode()), e);
        } catch (Exception e) {
            logger.error("Erro inesperado ao obter geolocalização para IP {}: {}", ip, e.getMessage(), e);
            throw new ExternalServiceException(String.format("Erro inesperado ao obter geolocalização: %s", e.getMessage()), e);
        }
    }

    /**
     * Fallback method for circuit breaker
     * Returns cached data or error message when external API is unavailable
     */
    private String getGeoFallback(String ip, Exception ex) {
        logger.warn("Circuit breaker activated for IP {}: {}", ip, ex.getMessage());
        return String.format(
            "{\"status\":\"fail\",\"message\":\"Serviço de geolocalização temporariamente indisponível\",\"query\":\"%s\"}",
            ip
        );
    }
}
