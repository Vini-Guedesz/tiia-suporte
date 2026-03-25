package com.project.suporte.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.suporte.ai.dto.PortScanRequestDTO;
import com.project.suporte.ai.dto.PortScanResponseDTO;
import com.project.suporte.ai.exceptions.ApiException;
import com.project.suporte.ai.service.PortScanService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PortScanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PortScanService portScanService;

    @Test
    void shouldScanPorts() throws Exception {
        PortScanRequestDTO request = new PortScanRequestDTO("scanme.nmap.org", List.of(80, 443), 800);
        when(portScanService.scanPorts(request))
                .thenReturn(new PortScanResponseDTO("scanme.nmap.org", List.of(80, 443)));

        mockMvc.perform(post("/api/v1/portscan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.host").value("scanme.nmap.org"))
                .andExpect(jsonPath("$.portas_abertas[0]").value(80));
    }

    @Test
    void shouldRejectRestrictedTarget() throws Exception {
        PortScanRequestDTO request = new PortScanRequestDTO("localhost", List.of(80, 443), 800);
        when(portScanService.scanPorts(request))
                .thenThrow(new ApiException(HttpStatus.FORBIDDEN, "restricted_target", "Port scan em localhost ou redes privadas não é permitido."));

        mockMvc.perform(post("/api/v1/portscan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("restricted_target"));
    }

    @Test
    void shouldValidateRequestBody() throws Exception {
        PortScanRequestDTO request = new PortScanRequestDTO("", List.of(), 10);

        mockMvc.perform(post("/api/v1/portscan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_error"));
    }
}
