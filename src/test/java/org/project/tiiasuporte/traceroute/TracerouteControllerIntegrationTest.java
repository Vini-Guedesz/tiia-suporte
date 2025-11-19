package org.project.tiiasuporte.traceroute;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import static org.mockito.ArgumentMatchers.any;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.web.client.RestTemplate;
import java.util.function.Supplier;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

@SpringBootTest
@AutoConfigureMockMvc
public class TracerouteControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TracerouteService tracerouteService;

    @Test
    public void tracerouteShouldReturnSuccess() throws Exception {
        String host = "google.com";
        String mockResponse = "Rastreando a rota para google.com";

        when(tracerouteService.rawTraceroute(anyString(), anyInt(), anyInt()))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        MvcResult mvcResult = mockMvc.perform(get("/api/v1/traceroute/{host}", host))
                .andExpect(status().isOk())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().string(mockResponse));
    }
}
