package org.project.tiiasuporte.traceroute;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@RestController
@RequestMapping("/api/traceroute")
@Tag(name = "Diagnóstico de Rede", description = "Endpoints para ferramentas de diagnóstico de rede")
public class TracerouteController {

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
    public String traceroute(
        @Parameter(
            description = "O host (domínio ou IP) para o qual o traceroute será executado.",
            example = "google.com"
        ) 
        @PathVariable String host) {
        StringBuilder result = new StringBuilder();
        String command;

        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            command = "tracert " + host;
        } else {
            command = "traceroute " + host;
        }

        try {
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append("\n");
            }

            process.waitFor();
        } catch (IOException | InterruptedException e) {
            result.append("Erro ao executar o traceroute: ").append(e.getMessage());
        }

        return result.toString();
    }
}
