package org.project.tiiasuporte.traceroute;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import static org.mockito.ArgumentMatchers.any;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.web.client.RestTemplate;
import java.util.function.Supplier;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

@SpringBootTest
@AutoConfigureMockMvc
public class TracerouteGeoControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TracerouteService tracerouteService;

    @Test
    public void tracerouteWithGeoShouldReturnHops() throws Exception {
        String host = "google.com";
        TracerouteHop hop1 = new TracerouteHop(1, "192.168.1.1", "router.local");
        TracerouteHop hop2 = new TracerouteHop(2, "10.0.0.1", "isp-gateway.local");
        List<TracerouteHop> mockHops = Arrays.asList(hop1, hop2);

        when(tracerouteService.traceroute(anyString(), anyInt(), anyInt()))
                .thenReturn(CompletableFuture.completedFuture(mockHops));

        MvcResult mvcResult = mockMvc.perform(get("/api/v1/traceroute/geo/{host}", host))
                .andExpect(status().isOk())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].hopNumber").value(1))
                .andExpect(jsonPath("$[0].ipAddress").value("192.168.1.1"))
                .andExpect(jsonPath("$[1].hopNumber").value(2))
                .andExpect(jsonPath("$[1].ipAddress").value("10.0.0.1"));
    }
}
