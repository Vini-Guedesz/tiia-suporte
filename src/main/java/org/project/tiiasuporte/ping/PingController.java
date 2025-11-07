package org.project.tiiasuporte.ping;

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

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/ping")
@Tag(name = "Ping", description = "Endpoints para teste de conectividade de rede")
public class PingController {

    private final PingService pingService;

    @Autowired
    public PingController(PingService pingService) {
        this.pingService = pingService;
    }

    @Operation(
        summary = "Executa um teste de ping para um host",
        description = "Envia 4 pacotes ICMP para o host especificado e retorna estatísticas de conectividade incluindo tempo de resposta e perda de pacotes.",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Ping executado com sucesso. Retorna a saída completa do comando ping.",
                content = @Content(
                    mediaType = "text/plain",
                    schema = @Schema(implementation = String.class),
                    examples = @ExampleObject(value = "PING google.com (142.250.218.142): 56 data bytes\n64 bytes from 142.250.218.142: icmp_seq=0 ttl=117 time=10.5 ms\n64 bytes from 142.250.218.142: icmp_seq=1 ttl=117 time=9.8 ms\n64 bytes from 142.250.218.142: icmp_seq=2 ttl=117 time=10.1 ms\n64 bytes from 142.250.218.142: icmp_seq=3 ttl=117 time=10.3 ms\n\n--- google.com ping statistics ---\n4 packets transmitted, 4 packets received, 0.0% packet loss\nround-trip min/avg/max/stddev = 9.8/10.2/10.5/0.3 ms")
                )
            ),
            @ApiResponse(
                responseCode = "400",
                description = "Host inválido fornecido",
                content = @Content(
                    mediaType = "application/json"
                )
            )
        }
    )
    @GetMapping("/{host}")
    public CompletableFuture<ResponseEntity<String>> pingHost(
        @Parameter(
            description = "O host (domínio ou endereço IP) para o qual o ping será executado. Aceita IPv4, IPv6 e nomes de domínio.",
            example = "google.com",
            required = true
        )
        @PathVariable String host) {
        return pingService.ping(host)
                .thenApply(ResponseEntity::ok);
    }
}
