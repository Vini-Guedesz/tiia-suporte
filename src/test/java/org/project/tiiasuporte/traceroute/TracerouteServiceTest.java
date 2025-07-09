package org.project.tiiasuporte.traceroute;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.tiiasuporte.geolocalizacao.GeoService;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TracerouteServiceTest {

    @Mock
    private GeoService geoService;

    @InjectMocks
    private TracerouteService tracerouteService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(tracerouteService, "tracerouteCommandWindows", "tracert");
        ReflectionTestUtils.setField(tracerouteService, "tracerouteCommandLinux", "traceroute");
    }

    @Test
    void testTraceroute_Success() throws ExecutionException, InterruptedException {
        String host = "google.com";

        // Mock GeoService response
        when(geoService.obterLocalizacao(anyString())).thenReturn("{\"status\":\"success\", \"country\":\"US\", \"city\":\"Mountain View\", \"lat\":37.3861, \"lon\":-122.0829}");

        CompletableFuture<List<TracerouteHop>> future = tracerouteService.traceroute(host, 30, 5000);
        List<TracerouteHop> hops = future.get();

        assertNotNull(hops);
        assertFalse(hops.isEmpty());
        // Basic check for hop number and IP address
        assertTrue(hops.get(0).getHopNumber() > 0);
        assertTrue(hops.get(0).getIpAddress().matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$"));
    }

    @Test
    void testTraceroute_InvalidHost() throws ExecutionException, InterruptedException {
        String host = "invalid-host@";

        CompletableFuture<List<TracerouteHop>> future = tracerouteService.traceroute(host, 30, 5000);
        List<TracerouteHop> hops = future.get();

        assertNotNull(hops);
        assertEquals(1, hops.size());
        assertEquals("Host inválido.", hops.get(0).getHostname());
    }
}