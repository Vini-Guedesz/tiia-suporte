package com.project.suporte.ai.controller;

import com.project.suporte.ai.dto.DnsLookupResponseDTO;
import com.project.suporte.ai.service.DnsLookupService;
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
@RequestMapping("/api/v1/dns")
@Tag(name = "DNS", description = "Endpoints para Consulta de DNS")
public class DnsLookupController {

    private final DnsLookupService dnsLookupService;

    public DnsLookupController(DnsLookupService dnsLookupService) {
        this.dnsLookupService = dnsLookupService;
    }

    @GetMapping
    @Operation(summary = "Consulta registros DNS", description = "Aceita hostname, IP ou URL via query param 'hostname'.")
    public ResponseEntity<DnsLookupResponseDTO> lookup(@Parameter(example = "openai.com") @RequestParam String hostname) {
        return ResponseEntity.ok(dnsLookupService.lookup(hostname));
    }

    @GetMapping("/{hostname}")
    @Operation(summary = "Consulta registros DNS para um hostname", description = "Rota legada mantida por compatibilidade. Prefira /api/v1/dns?hostname=host.")
    public ResponseEntity<DnsLookupResponseDTO> lookupLegacy(@PathVariable String hostname) {
        return ResponseEntity.ok(dnsLookupService.lookup(hostname));
    }
}
