package com.project.suporte.ai.controller;

import com.project.suporte.ai.service.TracerouteService;
import com.project.suporte.ai.support.SseEmitterFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/traceroute")
@Tag(name = "Traceroute", description = "Endpoints para Traceroute")
public class TracerouteController {

    private final TracerouteService tracerouteService;
    private final SseEmitterFactory emitterFactory;

    public TracerouteController(TracerouteService tracerouteService, SseEmitterFactory emitterFactory) {
        this.tracerouteService = tracerouteService;
        this.emitterFactory = emitterFactory;
    }

    @GetMapping(produces = "text/event-stream")
    @Operation(summary = "Executa traceroute com streaming SSE", description = "Aceita host, IP ou URL via query param 'target' e retorna eventos estruturados.")
    public SseEmitter traceroute(@Parameter(example = "1.1.1.1") @RequestParam String target) {
        return startTraceroute(target);
    }

    @GetMapping(value = "/{host}", produces = "text/event-stream")
    @Operation(summary = "Executa um comando traceroute e transmite a saída", description = "Rota legada mantida por compatibilidade. Prefira /api/v1/traceroute?target=host.")
    public SseEmitter tracerouteLegacy(@PathVariable String host) {
        return startTraceroute(host);
    }

    private SseEmitter startTraceroute(String host) {
        SseEmitter emitter = emitterFactory.create();
        tracerouteService.executeTraceroute(emitter, host);
        return emitter;
    }
}
