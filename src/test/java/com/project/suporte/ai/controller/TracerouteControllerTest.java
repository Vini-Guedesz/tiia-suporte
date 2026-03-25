package com.project.suporte.ai.controller;

import com.project.suporte.ai.service.TracerouteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TracerouteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TracerouteService tracerouteService;

    @Test
    void shouldStartTracerouteStream() throws Exception {
        doNothing().when(tracerouteService).executeTraceroute(any(), eq("1.1.1.1"));

        mockMvc.perform(get("/api/v1/traceroute").param("target", "1.1.1.1"))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted());
    }
}
