package org.project.tiiasuporte.pingmonitor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ping-monitor")
@Tag(name = "Ping Monitor", description = "Monitoramento contínuo de ping em tempo real")
public class PingMonitorController {

    private static final Logger logger = LoggerFactory.getLogger(PingMonitorController.class);
    private final PingMonitorService pingMonitorService;

    public PingMonitorController(PingMonitorService pingMonitorService) {
        this.pingMonitorService = pingMonitorService;
    }

    @PostMapping("/targets")
    @Operation(summary = "Adicionar alvo de monitoramento", description = "Adiciona um novo host ou IP para monitoramento contínuo")
    public ResponseEntity<PingMonitorTarget> addTarget(
            @Parameter(description = "Endereço IP ou hostname para monitorar", example = "google.com")
            @RequestParam String target,
            @Parameter(description = "Nome/label para identificar o alvo", example = "Servidor Principal")
            @RequestParam(required = false) String name) {

        logger.info("Requisição para adicionar alvo de monitoramento: {} ({})", target, name);
        PingMonitorTarget monitorTarget = pingMonitorService.addTarget(target, name);
        return ResponseEntity.status(HttpStatus.CREATED).body(monitorTarget);
    }

    @GetMapping("/targets")
    @Operation(summary = "Listar alvos", description = "Lista todos os alvos sendo monitorados")
    public ResponseEntity<Map<String, PingMonitorTarget>> getTargets() {
        logger.info("Requisição para listar alvos de monitoramento");
        return ResponseEntity.ok(pingMonitorService.getTargets());
    }

    @DeleteMapping("/targets/{targetId}")
    @Operation(summary = "Remover alvo", description = "Remove um alvo de monitoramento")
    public ResponseEntity<Void> removeTarget(
            @Parameter(description = "ID do alvo a ser removido")
            @PathVariable String targetId) {

        logger.info("Requisição para remover alvo de monitoramento: {}", targetId);
        pingMonitorService.removeTarget(targetId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/targets/{targetId}/pause")
    @Operation(summary = "Pausar monitoramento", description = "Pausa o monitoramento de um alvo específico")
    public ResponseEntity<Void> pauseTarget(
            @Parameter(description = "ID do alvo a ser pausado")
            @PathVariable String targetId) {

        logger.info("Requisição para pausar alvo de monitoramento: {}", targetId);
        pingMonitorService.pauseTarget(targetId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/targets/{targetId}/resume")
    @Operation(summary = "Retomar monitoramento", description = "Retoma o monitoramento de um alvo pausado")
    public ResponseEntity<Void> resumeTarget(
            @Parameter(description = "ID do alvo a ser retomado")
            @PathVariable String targetId) {

        logger.info("Requisição para retomar alvo de monitoramento: {}", targetId);
        pingMonitorService.resumeTarget(targetId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/targets/{targetId}/config")
    @Operation(summary = "Atualizar configuração", description = "Atualiza as configurações de um alvo específico")
    public ResponseEntity<Void> updateConfig(
            @Parameter(description = "ID do alvo")
            @PathVariable String targetId,
            @RequestBody PingTargetConfig config) {

        logger.info("Requisição para atualizar configuração do alvo: {}", targetId);
        pingMonitorService.updateTargetConfig(targetId, config);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/targets/{targetId}/history")
    @Operation(summary = "Obter histórico", description = "Retorna o histórico de ping de um alvo")
    public ResponseEntity<List<HistoryEntry>> getHistory(
            @Parameter(description = "ID do alvo")
            @PathVariable String targetId,
            @Parameter(description = "Limite de registros (padrão: 50)")
            @RequestParam(defaultValue = "50") int limit) {

        logger.info("Requisição para obter histórico do alvo: {} (limit: {})", targetId, limit);
        List<HistoryEntry> history = pingMonitorService.getHistory(targetId, limit);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/summary")
    @Operation(summary = "Obter resumo", description = "Retorna um resumo geral de todos os alvos monitorados")
    public ResponseEntity<Map<String, Object>> getSummary() {
        logger.info("Requisição para obter resumo geral");
        return ResponseEntity.ok(pingMonitorService.getSummary());
    }
}
