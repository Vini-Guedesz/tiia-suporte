package com.project.suporte.ai.controller;

import com.project.suporte.ai.dto.PortScanRequestDTO;
import com.project.suporte.ai.dto.PortScanResponseDTO;
import com.project.suporte.ai.service.PortScanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/portscan")
@Tag(name = "Varredura de Portas", description = "Endpoints para Varredura de Portas de Rede")
public class PortScanController {

    private final PortScanService portScanService;

    public PortScanController(PortScanService portScanService) {
        this.portScanService = portScanService;
    }

    @PostMapping
    @Operation(summary = "Verifica portas em um determinado host", description = "Aceita host público e até 64 portas por requisição. O sistema rejeita localhost e redes privadas para reduzir risco operacional.")
    public ResponseEntity<PortScanResponseDTO> scanPorts(@Valid @RequestBody PortScanRequestDTO request) {
        return ResponseEntity.ok(portScanService.scanPorts(request));
    }
}
