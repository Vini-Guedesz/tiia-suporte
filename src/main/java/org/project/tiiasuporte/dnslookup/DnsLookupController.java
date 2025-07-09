package org.project.tiiasuporte.dnslookup;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/dnslookup")
@Tag(name = "DNS Lookup", description = "Endpoints para realizar consultas DNS detalhadas")
public class DnsLookupController {

    private final DnsLookupService dnsLookupService;

    @Autowired
    public DnsLookupController(DnsLookupService dnsLookupService) {
        this.dnsLookupService = dnsLookupService;
    }

    @Operation(
        summary = "Realiza um DNS lookup detalhado para um host",
        description = "Retorna informações detalhadas de DNS para um determinado host, incluindo registros A, AAAA, MX, NS, TXT e CNAME.",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Informações de DNS retornadas com sucesso.",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Map.class),
                    examples = @ExampleObject(value = "{\"A\":[\"172.217.160.142\"],\"MX\":[\"10 alt1.aspmx.l.google.com.\",\"20 alt2.aspmx.l.google.com.\"],\"NS\":[\"ns1.google.com.\",\"ns2.google.com.\"],\"TXT\":[\"v=spf1 include:_spf.google.com ~all\"]}")
                )
            )
        }
    )
    @GetMapping("/{host}")
    public CompletableFuture<Map<String, Object>> dnsLookup(@PathVariable String host) {
        return dnsLookupService.dnsLookup(host);
    }
}
