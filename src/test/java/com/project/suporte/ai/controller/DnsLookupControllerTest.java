package com.project.suporte.ai.controller;

import com.project.suporte.ai.dto.DnsLookupResponseDTO;
import com.project.suporte.ai.service.DnsLookupService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DnsLookupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DnsLookupService dnsLookupService;

    @Test
    void shouldLookupDnsByQueryParam() throws Exception {
        when(dnsLookupService.lookup("openai.com"))
                .thenReturn(new DnsLookupResponseDTO("openai.com", List.of("104.18.33.45", "172.64.154.211")));

        mockMvc.perform(get("/api/v1/dns").param("hostname", "openai.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hostname").value("openai.com"))
                .andExpect(jsonPath("$.enderecos_ip[0]").value("104.18.33.45"));
    }
}
