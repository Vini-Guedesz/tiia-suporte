package com.project.suporte.ai.controller;

import com.project.suporte.ai.exceptions.ApiException;
import com.project.suporte.ai.service.PingMonitorService;
import com.project.suporte.ai.service.PingService;
import com.project.suporte.ai.support.SseEmitterFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/ping")
@Tag(name = "Ping", description = "Endpoints para Ping")
public class PingController {

    private final PingService pingService;
    private final PingMonitorService pingMonitorService;
    private final SseEmitterFactory emitterFactory;

    public PingController(PingService pingService, PingMonitorService pingMonitorService, SseEmitterFactory emitterFactory) {
        this.pingService = pingService;
        this.pingMonitorService = pingMonitorService;
        this.emitterFactory = emitterFactory;
    }

    @GetMapping(produces = "text/event-stream")
    @Operation(summary = "Executa ping com streaming SSE", description = "Aceita host, IP ou URL via query param 'target'. O streaming retorna eventos estruturados de início, saída, erro e conclusão.")
    public SseEmitter ping(
            @Parameter(example = "8.8.8.8") @RequestParam String target,
            @Parameter(example = "4") @RequestParam(defaultValue = "4") int count
    ) {
        validateCount(count);
        return startPing(target, count);
    }

    @GetMapping(value = "/{host}", produces = "text/event-stream")
    @Operation(summary = "Executa um comando ping e transmite a saída", description = "Rota legada mantida por compatibilidade. Prefira /api/v1/ping?target=host.")
    public SseEmitter pingLegacy(@PathVariable String host, @RequestParam(defaultValue = "4") int count) {
        validateCount(count);
        return startPing(host, count);
    }

    @GetMapping(value = "/monitor", produces = "text/event-stream")
    @Operation(summary = "Monitora conectividade em tempo real", description = "Executa probes continuos de ping para host, IP ou URL. O stream retorna status atual, ping medio, perda de pacote e quantidade de quedas enquanto a conexao permanecer aberta.")
    public SseEmitter monitor(
            @Parameter(example = "cliente.exemplo.com.br") @RequestParam String target,
            @Parameter(example = "5000") @RequestParam(defaultValue = "5000") int intervalMs,
            @Parameter(example = "2000") @RequestParam(defaultValue = "2000") int timeoutMs
    ) {
        validateMonitorParameters(intervalMs, timeoutMs);
        SseEmitter emitter = emitterFactory.createContinuous();
        pingMonitorService.monitor(emitter, target, intervalMs, timeoutMs);
        return emitter;
    }

    private SseEmitter startPing(String host, int count) {
        SseEmitter emitter = emitterFactory.create();
        pingService.executePing(emitter, host, count);
        return emitter;
    }

    private void validateCount(int count) {
        if (count < 1 || count > 10) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_count", "O parametro count deve estar entre 1 e 10.");
        }
    }

    private void validateMonitorParameters(int intervalMs, int timeoutMs) {
        if (intervalMs < 1000 || intervalMs > 60000) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_interval", "O intervalo deve estar entre 1000 ms e 60000 ms.");
        }
        if (timeoutMs < 500 || timeoutMs > 10000) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_timeout", "O timeout deve estar entre 500 ms e 10000 ms.");
        }
    }
}
