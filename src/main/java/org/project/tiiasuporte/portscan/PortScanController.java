package org.project.tiiasuporte.portscan;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/portscan")
@Tag(name = "Port Scan", description = "Endpoints para realizar varredura de portas")
public class PortScanController {

    private final PortScanService portScanService;

    @Autowired
    public PortScanController(PortScanService portScanService) {
        this.portScanService = portScanService;
    }

    @Operation(
        summary = "Realiza um scan de portas em um host",
        description = "Verifica quais portas de uma lista especificada estão abertas em um determinado host.",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Lista de portas abertas.",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Integer.class, type = "array"),
                    examples = @ExampleObject(value = "[80, 443, 22]")
                )
            )
        }
    )
    @GetMapping("/{host}")
    public CompletableFuture<ResponseEntity<List<Integer>>> scanPorts(
        @Parameter(
            description = "O host (domínio ou IP) para o qual o scan de portas será executado.",
            example = "google.com"
        )
        @PathVariable String host,
        @Parameter(
            description = "Lista de portas a serem verificadas, separadas por vírgula.",
            example = "80,443,22"
        )
        @RequestParam List<Integer> ports,
        @Parameter(
            description = "Tempo limite de conexão por porta em milissegundos.",
            example = "1000"
        )
        @RequestParam(defaultValue = "1000") int timeout) {

        return portScanService.scanPorts(host, ports, timeout)
                .thenApply(ResponseEntity::ok);
    }
}
