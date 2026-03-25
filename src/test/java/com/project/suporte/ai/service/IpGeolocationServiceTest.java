package com.project.suporte.ai.service;

import com.project.suporte.ai.config.DiagnosticsProperties;
import com.project.suporte.ai.dto.IpGeolocationDTO;
import com.project.suporte.ai.dto.IpGeolocationResponseDTO;
import com.project.suporte.ai.exceptions.ApiException;
import com.project.suporte.ai.support.TargetValidator;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.net.InetAddress;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IpGeolocationServiceTest {

    @Test
    void shouldGeolocateAndCacheResolvedPublicIp() throws Exception {
        RestTemplate restTemplate = mock(RestTemplate.class);
        TargetValidator validator = mock(TargetValidator.class);
        DiagnosticsProperties properties = new DiagnosticsProperties();
        IpGeolocationService service = new IpGeolocationService(restTemplate, validator, properties);

        when(validator.resolvePublicAddress("openai.com")).thenReturn(InetAddress.getByName("8.8.8.8"));
        when(restTemplate.getForObject(any(URI.class), eq(IpGeolocationDTO.class)))
                .thenReturn(new IpGeolocationDTO(
                        "8.8.8.8",
                        "success",
                        null,
                        "United States",
                        "US",
                        "CA",
                        "California",
                        "Mountain View",
                        "94043",
                        37.4056,
                        -122.0775,
                        "America/Los_Angeles",
                        "Google LLC",
                        "Google LLC",
                        "AS15169"
                ));

        IpGeolocationResponseDTO first = service.geolocateIp("openai.com");
        IpGeolocationResponseDTO second = service.geolocateIp("openai.com");

        assertEquals("8.8.8.8", first.query());
        assertEquals("America/Los_Angeles", first.timezone());
        assertEquals(first, second);
        verify(restTemplate, times(1)).getForObject(any(URI.class), eq(IpGeolocationDTO.class));
    }

    @Test
    void shouldFailWhenProviderReturnsErrorStatus() throws Exception {
        RestTemplate restTemplate = mock(RestTemplate.class);
        TargetValidator validator = mock(TargetValidator.class);
        IpGeolocationService service = new IpGeolocationService(restTemplate, validator, new DiagnosticsProperties());

        when(validator.resolvePublicAddress("8.8.8.8")).thenReturn(InetAddress.getByName("8.8.8.8"));
        when(restTemplate.getForObject(any(URI.class), eq(IpGeolocationDTO.class)))
                .thenReturn(new IpGeolocationDTO(
                        "8.8.8.8",
                        "fail",
                        "invalid query",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        0,
                        0,
                        null,
                        null,
                        null,
                        null
                ));

        assertThrows(ApiException.class, () -> service.geolocateIp("8.8.8.8"));
    }
}
