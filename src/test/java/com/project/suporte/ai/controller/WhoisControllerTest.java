package com.project.suporte.ai.controller;

import com.project.suporte.ai.dto.WhoisResponseDTO;
import com.project.suporte.ai.service.WhoisService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WhoisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WhoisService whoisService;

    @Test
    void shouldLookupWhoisByQueryParam() throws Exception {
        when(whoisService.lookup("openai.com"))
                .thenReturn(new WhoisResponseDTO(
                        "openai.com",
                        "MarkMonitor Inc.",
                        "2015-04-24T00:00:00Z",
                        "2030-04-24T00:00:00Z",
                        "ns1.example.com, ns2.example.com",
                        "clientTransferProhibited"
                ));

        mockMvc.perform(get("/api/v1/whois").param("domain", "openai.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dominio").value("openai.com"))
                .andExpect(jsonPath("$.registrador").value("MarkMonitor Inc."));
    }
}
