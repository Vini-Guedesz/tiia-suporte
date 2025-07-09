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
        return CompletableFuture.supplyAsync(() -> {
            String command;
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                command = pingCommandWindows + " " + host;
            } else {
                command = pingCommandLinux + " " + host;
            }

            StringBuilder result = new StringBuilder();
            try {
                Process process = Runtime.getRuntime().exec(command);
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line).append("\n");
                }
                process.waitFor();
                logger.info("Ping para {} concluído:\n{}", host, result.toString());
            } catch (IOException | InterruptedException e) {
                logger.error("Erro ao executar o ping para {}: {}", host, e.getMessage(), e);
                result.append("Erro ao executar o ping: ").append(e.getMessage());
            }
            return result.toString();
        });
    }
}
