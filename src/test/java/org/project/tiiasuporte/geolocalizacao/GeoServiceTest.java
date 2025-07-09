package org.project.tiiasuporte.geolocalizacao;

import org.project.tiiasuporte.exceptions.ExternalServiceException;
import org.project.tiiasuporte.exceptions.InvalidIpAddressException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class GeoServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private GeoService geoService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(geoService, "geolocalizacaoApiUrl", "http://ip-api.com/json/");
    }

    @Test
    void testObterLocalizacao_Success() {
        String ip = "8.8.8.8";
        String expectedResponse = "{\"status\": \"success\"}";
        when(restTemplate.getForObject("http://ip-api.com/json/" + ip, String.class)).thenReturn(expectedResponse);

        String actualResponse = geoService.obterLocalizacao(ip);

        assertEquals(expectedResponse, actualResponse);
    }

    @Test
    void testObterLocalizacao_InvalidIp() {
        String ip = "invalid-ip";

        assertThrows(InvalidIpAddressException.class, () -> {
            geoService.obterLocalizacao(ip);
        });
    }

    @Test
    void testObterLocalizacao_ApiError() {
        String ip = "8.8.8.8";
        when(restTemplate.getForObject("http://ip-api.com/json/" + ip, String.class))
            .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        assertThrows(ExternalServiceException.class, () -> {
            geoService.obterLocalizacao(ip);
        });
    }
}
