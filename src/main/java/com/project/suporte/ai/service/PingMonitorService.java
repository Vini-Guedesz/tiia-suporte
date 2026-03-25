package com.project.suporte.ai.service;

import com.project.suporte.ai.dto.PingMonitorEventDTO;
import com.project.suporte.ai.support.ProcessLauncher;
import com.project.suporte.ai.support.TargetValidator;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PingMonitorService {

    private static final Pattern LATENCY_PATTERN = Pattern.compile(
            "(?:time|tempo)\\s*[=<]?\\s*<?\\s*(\\d+(?:[\\.,]\\d+)?)\\s*ms",
            Pattern.CASE_INSENSITIVE
    );

    private final TargetValidator targetValidator;
    private final ProcessLauncher processLauncher;

    public PingMonitorService(TargetValidator targetValidator, ProcessLauncher processLauncher) {
        this.targetValidator = targetValidator;
        this.processLauncher = processLauncher;
    }

    @Async("diagnosticsExecutor")
    public void monitor(SseEmitter emitter, String rawTarget, int intervalMs, int timeoutMs) {
        String target = targetValidator.normalizeTarget(rawTarget);
        AtomicBoolean active = new AtomicBoolean(true);
        PingMonitorAccumulator accumulator = new PingMonitorAccumulator(target);
        registerCallbacks(emitter, active);

        if (!safeSend(emitter, "started", accumulator.started("Monitoramento iniciado."))) {
            return;
        }

        try {
            while (active.get() && !Thread.currentThread().isInterrupted()) {
                PingProbeResult probeResult = probeOnce(target, timeoutMs);
                if (!safeSend(emitter, "sample", accumulator.sample(probeResult))) {
                    return;
                }

                if (!active.get()) {
                    break;
                }

                Thread.sleep(intervalMs);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (Exception exception) {
            safeSend(emitter, "error", accumulator.error("Falha ao monitorar o alvo: " + exception.getMessage()));
            emitter.complete();
            return;
        }

        if (active.compareAndSet(true, false)) {
            safeSend(emitter, "completed", accumulator.completed("Monitoramento encerrado."));
            emitter.complete();
        }
    }

    PingProbeResult probeOnce(String target, int timeoutMs) throws Exception {
        Process process = processLauncher.start(buildSinglePingCommand(target, timeoutMs));
        List<String> outputLines = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                outputLines.add(line.trim());
            }
        }

        int exitCode = process.waitFor();
        Double latencyMs = extractLatencyMs(outputLines);
        boolean success = exitCode == 0;
        String message = buildProbeMessage(target, success, latencyMs, outputLines);

        return new PingProbeResult(success, latencyMs, message, exitCode);
    }

    List<String> buildSinglePingCommand(String target, int timeoutMs) {
        boolean isWindows = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
        if (isWindows) {
            return List.of("ping", "-n", "1", "-w", String.valueOf(timeoutMs), target);
        }

        int timeoutSeconds = Math.max(1, (int) Math.ceil(timeoutMs / 1000.0));
        return List.of("ping", "-c", "1", "-W", String.valueOf(timeoutSeconds), target);
    }

    Double extractLatencyMs(List<String> outputLines) {
        for (String line : outputLines) {
            Matcher matcher = LATENCY_PATTERN.matcher(line);
            if (matcher.find()) {
                return Double.parseDouble(matcher.group(1).replace(',', '.'));
            }
        }
        return null;
    }

    private String buildProbeMessage(String target, boolean success, Double latencyMs, List<String> outputLines) {
        if (success) {
            if (latencyMs != null) {
                return "Ping OK em " + formatLatency(latencyMs) + " ms.";
            }
            return "Ping OK para " + target + ".";
        }

        String rawLine = outputLines.stream()
                .filter(line -> !line.isBlank())
                .filter(line -> !line.toLowerCase(Locale.ROOT).contains("ping statistics"))
                .reduce((first, second) -> second)
                .orElse("Sem resposta do alvo.");

        return rawLine.length() > 180 ? rawLine.substring(0, 180) : rawLine;
    }

    private String formatLatency(Double latencyMs) {
        return latencyMs % 1 == 0 ? String.valueOf(latencyMs.longValue()) : String.format(Locale.US, "%.2f", latencyMs);
    }

    private void registerCallbacks(SseEmitter emitter, AtomicBoolean active) {
        emitter.onCompletion(() -> active.set(false));
        emitter.onTimeout(() -> active.set(false));
        emitter.onError(error -> active.set(false));
    }

    private boolean safeSend(SseEmitter emitter, String eventName, PingMonitorEventDTO payload) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(payload));
            return true;
        } catch (Exception exception) {
            emitter.complete();
            return false;
        }
    }
}
