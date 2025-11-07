package org.project.tiiasuporte.portscan;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.project.tiiasuporte.util.ValidationUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class PortScanService {

    private static final Logger logger = LoggerFactory.getLogger(PortScanService.class);
    private static final int MIN_PORT = 1;
    private static final int MAX_PORT = 65535;
    private static final int MAX_PORTS_PER_SCAN = 100;

    @RateLimiter(name = "portScan")
    public CompletableFuture<List<Integer>> scanPorts(String host, List<Integer> ports, int timeout) {
        return CompletableFuture.supplyAsync(() -> {
            List<Integer> openPorts = new ArrayList<>();

            if (!ValidationUtils.isValidIpOrHostname(host)) {
                logger.warn("Tentativa de scan de portas com host inválido: {}", host);
                return openPorts; // Retorna lista vazia para host inválido
            }

            // Filtra portas válidas
            List<Integer> validPorts = ports.stream()
                .filter(port -> {
                    if (port < MIN_PORT || port > MAX_PORT) {
                        logger.warn("Porta fora do range válido (1-65535): {}", port);
                        return false;
                    }
                    return true;
                })
                .distinct()
                .collect(Collectors.toList());

            // Limita o número de portas para evitar abuso
            if (validPorts.size() > MAX_PORTS_PER_SCAN) {
                logger.warn("Tentativa de scan com {} portas. Limitando a {}", validPorts.size(), MAX_PORTS_PER_SCAN);
                validPorts = validPorts.subList(0, MAX_PORTS_PER_SCAN);
            }

            logger.info("Iniciando scan de {} portas em {}", validPorts.size(), host);

            for (int port : validPorts) {
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(host, port), timeout);
                    openPorts.add(port);
                    logger.info("Porta {} aberta em {}", port, host);
                } catch (IOException e) {
                    logger.debug("Porta {} fechada ou inacessível em {}: {}", port, host, e.getMessage());
                }
            }

            logger.info("Scan concluído em {}: {} portas abertas de {} verificadas", host, openPorts.size(), validPorts.size());
            return openPorts;
        });
    }
}
