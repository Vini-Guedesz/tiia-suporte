package com.project.suporte.ai.service;

import com.project.suporte.ai.config.DiagnosticsProperties;
import com.project.suporte.ai.dto.IpGeolocationDTO;
import com.project.suporte.ai.dto.IpGeolocationResponseDTO;
import com.project.suporte.ai.exceptions.ApiException;
import com.project.suporte.ai.support.ExpiringCache;
import com.project.suporte.ai.support.TargetValidator;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.InetAddress;
import java.net.URI;
import java.time.Duration;

import org.springframework.http.HttpStatus;

@Service
public class IpGeolocationService {

    private final RestTemplate restTemplate;
    private final TargetValidator targetValidator;
    private final DiagnosticsProperties properties;
    private final ExpiringCache<String, IpGeolocationResponseDTO> cache;

    public IpGeolocationService(
            RestTemplate restTemplate,
            TargetValidator targetValidator,
            DiagnosticsProperties properties
    ) {
        this.restTemplate = restTemplate;
        this.targetValidator = targetValidator;
        this.properties = properties;
        this.cache = new ExpiringCache<>(Duration.ofSeconds(properties.getCache().getGeolocationTtlSeconds()));
    }

    public IpGeolocationResponseDTO geolocateIp(String ipAddress) {
        InetAddress publicAddress = targetValidator.resolvePublicAddress(ipAddress);
        String resolvedIp = publicAddress.getHostAddress();
        return cache.get(resolvedIp, () -> fetchGeolocation(resolvedIp));
    }

    private IpGeolocationResponseDTO fetchGeolocation(String resolvedIp) {
        URI url = UriComponentsBuilder.fromUriString(properties.getGeolocation().getBaseUrl())
                .pathSegment(resolvedIp)
                .build()
                .toUri();

        IpGeolocationDTO dto = restTemplate.getForObject(url, IpGeolocationDTO.class);
        if (dto == null) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "geolocation_unavailable", "O serviço de geolocalização não retornou dados.");
        }

        if (!"success".equalsIgnoreCase(dto.status())) {
            String message = dto.message() != null && !dto.message().isBlank()
                    ? dto.message()
                    : "Não foi possível obter a geolocalização do alvo informado.";
            throw new ApiException(HttpStatus.BAD_GATEWAY, "geolocation_failed", message);
        }

        return new IpGeolocationResponseDTO(
                dto.query(),
                dto.country(),
                dto.regionName(),
                dto.city(),
                dto.isp(),
                dto.org(),
                dto.timezone(),
                dto.lat(),
                dto.lon()
        );
    }
}
