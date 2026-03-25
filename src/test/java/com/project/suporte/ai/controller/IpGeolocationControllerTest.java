package com.project.suporte.ai.controller;

import com.project.suporte.ai.dto.IpGeolocationResponseDTO;
import com.project.suporte.ai.service.IpGeolocationService;
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
class IpGeolocationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IpGeolocationService ipGeolocationService;

    @Test
    void shouldGeolocateByQueryParam() throws Exception {
        when(ipGeolocationService.geolocateIp("8.8.8.8"))
                .thenReturn(new IpGeolocationResponseDTO(
                        "8.8.8.8",
                        "United States",
                        "California",
                        "Mountain View",
                        "Google LLC",
                        "Google LLC",
                        "America/Los_Angeles",
                        37.4056,
                        -122.0775
                ));

        mockMvc.perform(get("/api/v1/geolocation").param("target", "8.8.8.8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ip").value("8.8.8.8"))
                .andExpect(jsonPath("$.fuso_horario").value("America/Los_Angeles"));
    }
}
