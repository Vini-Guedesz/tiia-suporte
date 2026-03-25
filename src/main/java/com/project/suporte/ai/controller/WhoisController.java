package com.project.suporte.ai.controller;

import com.project.suporte.ai.dto.WhoisResponseDTO;
import com.project.suporte.ai.service.WhoisService;
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
@RequestMapping("/api/v1/whois")
@Tag(name = "Whois", description = "Endpoints para Consulta Whois")
public class WhoisController {

    private final WhoisService whoisService;

    public WhoisController(WhoisService whoisService) {
        this.whoisService = whoisService;
    }

    @GetMapping
    @Operation(summary = "Consulta informações Whois", description = "Aceita domínio via query param 'domain'.")
    public ResponseEntity<WhoisResponseDTO> lookup(@Parameter(example = "openai.com") @RequestParam String domain) {
        return ResponseEntity.ok(whoisService.lookup(domain));
    }

    @GetMapping("/{domain}")
    @Operation(summary = "Consulta informações Whois para um domínio", description = "Rota legada mantida por compatibilidade. Prefira /api/v1/whois?domain=dominio.")
    public ResponseEntity<WhoisResponseDTO> lookupLegacy(@PathVariable String domain) {
        return ResponseEntity.ok(whoisService.lookup(domain));
    }
}
