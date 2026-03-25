package com.project.suporte.ai.controller;

import com.project.suporte.ai.dto.IpGeolocationResponseDTO;
import com.project.suporte.ai.service.IpGeolocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/geolocation")
@Tag(name = "Geolocalização", description = "Endpoints para Geolocalização de IP")
public class IpGeolocationController {

    private final IpGeolocationService ipGeolocationService;

    public IpGeolocationController(IpGeolocationService ipGeolocationService) {
        this.ipGeolocationService = ipGeolocationService;
    }

    @GetMapping
    @Operation(summary = "Geolocaliza um alvo público", description = "Aceita IP público, hostname ou URL via query param 'target' e resolve a geolocalização do IP público correspondente.")
    public ResponseEntity<IpGeolocationResponseDTO> geolocateIp(@Parameter(example = "8.8.8.8") @RequestParam String target) {
        return ResponseEntity.ok(ipGeolocationService.geolocateIp(target));
    }

    @GetMapping("/{ipAddress}")
    @Operation(summary = "Geolocaliza um endereço IP", description = "Rota legada mantida por compatibilidade. Prefira /api/v1/geolocation?target=ip-ou-host.")
    public ResponseEntity<IpGeolocationResponseDTO> geolocateIpLegacy(@PathVariable String ipAddress) {
        return ResponseEntity.ok(ipGeolocationService.geolocateIp(ipAddress));
    }
}
