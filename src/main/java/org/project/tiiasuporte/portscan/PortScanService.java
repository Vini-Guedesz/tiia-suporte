package org.project.tiiasuporte.portscan;

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

@Service
public class PortScanService {

    private static final Logger logger = LoggerFactory.getLogger(PortScanService.class);

    public CompletableFuture<List<Integer>> scanPorts(String host, List<Integer> ports, int timeout) {
        return CompletableFuture.supplyAsync(() -> {
            List<Integer> openPorts = new ArrayList<>();

            if (!ValidationUtils.isValidIpOrHostname(host)) {
                logger.warn("Tentativa de scan de portas com host inválido: {}", host);
                return openPorts; // Retorna lista vazia para host inválido
            }

            for (int port : ports) {
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(host, port), timeout);
                    openPorts.add(port);
                    logger.info("Porta {} aberta em {}", port, host);
                } catch (IOException e) {
                    logger.debug("Porta {} fechada ou inacessível em {}: {}", port, host, e.getMessage());
                }
            }
            return openPorts;
        });
    }
}
