package com.project.suporte.ai.service;

import com.project.suporte.ai.config.DiagnosticsProperties;
import com.project.suporte.ai.dto.PortScanRequestDTO;
import com.project.suporte.ai.dto.PortScanResponseDTO;
import com.project.suporte.ai.exceptions.ApiException;
import com.project.suporte.ai.support.PortProbe;
import com.project.suporte.ai.support.TargetValidator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
public class PortScanService {

    private final TargetValidator targetValidator;
    private final PortProbe portProbe;
    private final Executor executor;
    private final DiagnosticsProperties properties;

    public PortScanService(
            TargetValidator targetValidator,
            PortProbe portProbe,
            @Qualifier("portScanExecutor") Executor executor,
            DiagnosticsProperties properties
    ) {
        this.targetValidator = targetValidator;
        this.portProbe = portProbe;
        this.executor = executor;
        this.properties = properties;
    }

    public PortScanResponseDTO scanPorts(PortScanRequestDTO request) {
        String host = targetValidator.normalizePortScanTarget(request.host());
        List<Integer> ports = request.ports().stream().distinct().sorted().toList();
        if (ports.size() > properties.getPortscan().getMaxPorts()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "too_many_ports",
                    "A quantidade de portas excede o limite permitido por requisição."
            );
        }

        int timeout = request.timeout() != null ? request.timeout() : properties.getPortscan().getDefaultTimeoutMs();
        if (timeout > properties.getPortscan().getMaxTimeoutMs()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_timeout", "O timeout informado excede o máximo permitido.");
        }

        List<CompletableFuture<Integer>> futures = ports.stream()
                .map(port -> CompletableFuture.supplyAsync(
                        () -> portProbe.isOpen(new InetSocketAddress(host, port), timeout) ? port : null,
                        executor
                ))
                .toList();

        List<Integer> openPorts = futures.stream()
                .map(CompletableFuture::join)
                .filter(port -> port != null)
                .sorted()
                .toList();

        return new PortScanResponseDTO(host, openPorts);
    }
}
