package com.project.suporte.ai.controller;

import com.project.suporte.ai.service.PingMonitorService;
import com.project.suporte.ai.service.PingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PingService pingService;

    @MockitoBean
    private PingMonitorService pingMonitorService;

    @Test
    void shouldStartPingStream() throws Exception {
        doNothing().when(pingService).executePing(any(), eq("8.8.8.8"), eq(4));

        mockMvc.perform(get("/api/v1/ping").param("target", "8.8.8.8").param("count", "4"))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted());
    }

    @Test
    void shouldRejectInvalidPingCount() throws Exception {
        mockMvc.perform(get("/api/v1/ping").param("target", "8.8.8.8").param("count", "20"))
                .andExpect(status().isBadRequest())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value("invalid_count"));
    }

    @Test
    void shouldReturnSseErrorForInvalidPingCount() throws Exception {
        mockMvc.perform(get("/api/v1/ping")
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .param("target", "8.8.8.8")
                        .param("count", "20"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(containsString("event: error")))
                .andExpect(content().string(containsString("\"code\":\"invalid_count\"")))
                .andExpect(content().string(containsString("\"finished\":true")));
    }

    @Test
    void shouldStartMonitoringStream() throws Exception {
        doNothing().when(pingMonitorService).monitor(any(), eq("cliente.exemplo.com.br"), eq(5000), eq(2000));

        mockMvc.perform(get("/api/v1/ping/monitor")
                        .param("target", "cliente.exemplo.com.br")
                        .param("intervalMs", "5000")
                        .param("timeoutMs", "2000"))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted());
    }

    @Test
    void shouldRejectInvalidMonitorInterval() throws Exception {
        mockMvc.perform(get("/api/v1/ping/monitor")
                        .param("target", "cliente.exemplo.com.br")
                        .param("intervalMs", "250")
                        .param("timeoutMs", "2000"))
                .andExpect(status().isBadRequest())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value("invalid_interval"));
    }

    @Test
    void shouldIgnoreFaviconRequest() throws Exception {
        mockMvc.perform(get("/favicon.ico"))
                .andExpect(status().isNoContent());
    }
}
