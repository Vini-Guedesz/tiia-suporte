package org.project.tiiasuporte.ping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;

@Service
public class PingService {

    private static final Logger logger = LoggerFactory.getLogger(PingService.class);

    @Value("${ping.command.windows}")
    private String pingCommandWindows;

    @Value("${ping.command.linux}")
    private String pingCommandLinux;

    @Async
    public CompletableFuture<String> ping(String host) {
        StringBuilder result = new StringBuilder();

        String os = System.getProperty("os.name").toLowerCase();
        ProcessBuilder processBuilder;

        if (os.contains("win")) {
            // Windows: ping -n 4 host
            processBuilder = new ProcessBuilder(pingCommandWindows, "-n", "4", host);
        } else {
            // Linux/Unix: ping -c 4 host
            processBuilder = new ProcessBuilder(pingCommandLinux, "-c", "4", host);
        }

        try {
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append("\n");
            }

            // Captura erros se houver
            StringBuilder errors = new StringBuilder();
            while ((line = errorReader.readLine()) != null) {
                errors.append(line).append("\n");
            }

            int exitCode = process.waitFor();
            if (exitCode != 0 && errors.length() > 0) {
                result.append("\nErros:\n").append(errors);
            }

            logger.info("Ping para {} concluído com código {}", host, exitCode);
        } catch (IOException | InterruptedException e) {
            logger.error("Erro ao executar o ping para {}: {}", host, e.getMessage(), e);
            result.append("Erro ao executar o ping: ").append(e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }

        return CompletableFuture.completedFuture(result.toString());
    }
}
