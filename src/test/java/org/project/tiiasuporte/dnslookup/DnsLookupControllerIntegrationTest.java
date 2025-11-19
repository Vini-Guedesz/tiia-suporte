package org.project.tiiasuporte.dnslookup;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.mockito.ArgumentMatchers.any;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.web.client.RestTemplate;
import java.util.function.Supplier;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class DnsLookupControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DnsLookupService dnsLookupService;

    @Test
    void dnsLookup_ValidHost_ReturnsDnsInfo() throws Exception {
        Map<String, Object> mockDnsInfo = new HashMap<>();
        mockDnsInfo.put("A", Collections.singletonList("192.0.2.1"));
        mockDnsInfo.put("MX", Collections.singletonList("10 mail.example.com"));

        when(dnsLookupService.dnsLookup(anyString()))
                .thenReturn(CompletableFuture.completedFuture(mockDnsInfo));

        MvcResult mvcResult = mockMvc.perform(get("/api/v1/dnslookup/{host}", "example.com"))
                .andExpect(status().isOk())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(jsonPath("$.A[0]").value("192.0.2.1"))
                .andExpect(jsonPath("$.MX[0]").value("10 mail.example.com"));
    }

    @Test
    void dnsLookup_InvalidHost_ReturnsError() throws Exception {
        Map<String, Object> errorMap = new HashMap<>();
        errorMap.put("error", "Nome de host inválido.");

        when(dnsLookupService.dnsLookup(anyString()))
                .thenReturn(CompletableFuture.completedFuture(errorMap));

        MvcResult mvcResult = mockMvc.perform(get("/api/v1/dnslookup/{host}", "invalid_host"))
                .andExpect(status().isOk())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(jsonPath("$.error").value("Nome de host inválido."));
    }

    @Test
    void dnsLookup_ServiceThrowsException_ReturnsError() throws Exception {
        Map<String, Object> errorMap = new HashMap<>();
        errorMap.put("error", "Erro ao realizar DNS lookup: Test Exception");

        when(dnsLookupService.dnsLookup(anyString()))
                .thenReturn(CompletableFuture.completedFuture(errorMap));

        MvcResult mvcResult = mockMvc.perform(get("/api/v1/dnslookup/{host}", "example.com"))
                .andExpect(status().isOk())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(jsonPath("$.error").value("Erro ao realizar DNS lookup: Test Exception"));
    }
}
