package org.project.tiiasuporte.traceroute;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/traceroute")
@Tag(name = "Diagnóstico de Rede", description = "Endpoints para ferramentas de diagnóstico de rede")
public class TracerouteController {

    private final TracerouteService tracerouteService;

    @Autowired
    public TracerouteController(TracerouteService tracerouteService) {
        this.tracerouteService = tracerouteService;
    }

    @Operation(
        summary = "Executa um traceroute para um host",
        description = "Executa o comando traceroute (ou tracert no Windows) do sistema operacional para um determinado host (domínio ou IP) e retorna a saída como texto.",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Saída do comando traceroute.",
                content = @Content(
                    mediaType = "text/plain",
                    schema = @Schema(implementation = String.class),
                    examples = @ExampleObject(value = "Rastreando a rota para google.com [142.250.218.142]\n...\n  1    <1 ms    <1 ms    <1 ms  192.168.1.1\n  2     1 ms     1 ms     1 ms  [...gateway...]\n...\nRastreamento concluído.")
                )
            )
        }
    )
    @GetMapping("/{host}")
    public CompletableFuture<String> traceroute(
        @Parameter(
            description = "O host (domínio ou IP) para o qual o traceroute será executado.",
            example = "google.com"
        ) 
        @PathVariable String host,
        @Parameter(
            description = "Número máximo de saltos para o traceroute.",
            example = "30"
        )
        @RequestParam(defaultValue = "${traceroute.default.maxHops}") int maxHops,
        @Parameter(
            description = "Tempo limite por salto em milissegundos.",
            example = "4000"
        )
        @RequestParam(defaultValue = "${traceroute.default.timeout}") int timeout) {
        return tracerouteService.rawTraceroute(host, maxHops, timeout);
    }

    @Operation(
        summary = "Executa um traceroute com geolocalização para um host",
        description = "Executa o comando traceroute e, para cada salto, tenta obter informações de geolocalização (país, cidade, lat/lon) e retorna uma lista de objetos TracerouteHop.",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Lista de saltos do traceroute com dados de geolocalização.",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = TracerouteHop.class, type = "array"),
                    examples = @ExampleObject(value = "[\n  {\n    \"hopNumber\": 1,\n    \"ipAddress\": \"192.168.1.1\",\n    \"hostname\": \"router.local\",\n    \"country\": \"Brazil\",\n    \"city\": \"Sao Paulo\",\n    \"lat\": -23.5505,\n    \"lon\": -46.6333\n  },\n  {\n    \"hopNumber\": 2,\n    \"ipAddress\": \"10.0.0.1\",\n    \"hostname\": \"isp-gateway.local\",\n    \"country\": \"Brazil\",\n    \"city\": \"Sao Paulo\",\n    \"lat\": -23.5505,\n    \"lon\": -46.6333\n  }\n]")
                )
            )
        }
    )
    @RateLimiter(name = "traceroute")
    @GetMapping("/geo/{host}")
    public CompletableFuture<List<TracerouteHop>> tracerouteWithGeo(
        @Parameter(
            description = "O host (domínio ou IP) para o qual o traceroute com geolocalização será executado.",
            example = "google.com"
        ) 
        @PathVariable String host,
        @Parameter(
            description = "Número máximo de saltos para o traceroute.",
            example = "30"
        )
        @RequestParam(defaultValue = "${traceroute.default.maxHops}") int maxHops,
        @Parameter(
            description = "Tempo limite por salto em milissegundos.",
            example = "4000"
        )
        @RequestParam(defaultValue = "${traceroute.default.timeout}") int timeout) {
        return tracerouteService.traceroute(host, maxHops, timeout);
    }
}
