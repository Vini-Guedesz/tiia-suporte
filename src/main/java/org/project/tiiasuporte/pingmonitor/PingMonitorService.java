package org.project.tiiasuporte.pingmonitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class PingMonitorService {

    private static final Logger logger = LoggerFactory.getLogger(PingMonitorService.class);

    private final Map<String, PingMonitorTarget> targets = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    private final SimpMessagingTemplate messagingTemplate;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    private final ExecutorService pingExecutor = Executors.newFixedThreadPool(20);

    @Value("${ping.command.windows}")
    private String pingCommandWindows;

    @Value("${ping.command.linux}")
    private String pingCommandLinux;

    public PingMonitorService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
        pingExecutor.shutdownNow();
        logger.info("Ping monitor service shutdown complete");
    }

    public PingMonitorTarget addTarget(String target, String name) {
        PingMonitorTarget monitorTarget = new PingMonitorTarget(target, name);
        targets.put(monitorTarget.getId(), monitorTarget);
        logger.info("Adicionado alvo de monitoramento: {} ({}) com ID: {}", name != null ? name : target, target, monitorTarget.getId());

        // Schedule monitoring task for this target
        scheduleMonitoring(monitorTarget);

        return monitorTarget;
    }

    private void scheduleMonitoring(PingMonitorTarget target) {
        String targetId = target.getId();

        // Cancel existing task if any
        ScheduledFuture<?> existingTask = scheduledTasks.get(targetId);
        if (existingTask != null) {
            existingTask.cancel(false);
        }

        // Schedule new task with custom interval
        int intervalSeconds = target.getConfig().getIntervalSeconds();
        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(
            () -> monitorTarget(target),
            0,
            intervalSeconds,
            TimeUnit.SECONDS
        );

        scheduledTasks.put(targetId, task);
        logger.debug("Scheduled monitoring for {} with interval {}s", target.getTarget(), intervalSeconds);
    }

    private void monitorTarget(PingMonitorTarget target) {
        if (!target.isActive()) {
            return;
        }

        pingExecutor.submit(() -> {
            try {
                PingMonitorResult result = executePing(target);
                target.setLastUpdate(LocalDateTime.now());
                target.addHistoryEntry(result);

                // Send to WebSocket
                Map<String, Object> enrichedResult = new HashMap<>();
                enrichedResult.put("targetId", result.getTargetId());
                enrichedResult.put("target", result.getTarget());
                enrichedResult.put("online", result.isOnline());
                enrichedResult.put("latencyMs", result.getLatencyMs());
                enrichedResult.put("packetLoss", result.getPacketLoss());
                enrichedResult.put("timestamp", result.getTimestamp());
                enrichedResult.put("statistics", target.getStatistics());

                messagingTemplate.convertAndSend("/topic/ping-monitor", enrichedResult);

                logger.debug("Resultado de ping enviado para {}: online={}, latency={}, packetLoss={}",
                        target.getTarget(), result.isOnline(), result.getLatencyMs(), result.getPacketLoss());
            } catch (Exception e) {
                logger.error("Error monitoring target {}: {}", target.getTarget(), e.getMessage(), e);
            }
        });
    }

    public void removeTarget(String targetId) {
        PingMonitorTarget removed = targets.remove(targetId);
        if (removed != null) {
            // Cancel scheduled task
            ScheduledFuture<?> task = scheduledTasks.remove(targetId);
            if (task != null) {
                task.cancel(false);
            }
            logger.info("Removido alvo de monitoramento: {} com ID: {}", removed.getTarget(), targetId);
        }
    }

    public void pauseTarget(String targetId) {
        PingMonitorTarget target = targets.get(targetId);
        if (target != null) {
            target.setActive(false);
            logger.info("Pausado alvo de monitoramento: {} com ID: {}", target.getTarget(), targetId);
        }
    }

    public void resumeTarget(String targetId) {
        PingMonitorTarget target = targets.get(targetId);
        if (target != null) {
            target.setActive(true);
            logger.info("Retomado alvo de monitoramento: {} com ID: {}", target.getTarget(), targetId);
        }
    }

    public void updateTargetConfig(String targetId, PingTargetConfig config) {
        PingMonitorTarget target = targets.get(targetId);
        if (target != null) {
            target.setConfig(config);
            // Reschedule with new interval
            scheduleMonitoring(target);
            logger.info("Atualizada configuração para alvo {}", target.getTarget());
        }
    }

    public Map<String, PingMonitorTarget> getTargets() {
        return new ConcurrentHashMap<>(targets);
    }

    public Map<String, Object> getSummary() {
        int total = targets.size();
        long online = targets.values().stream().filter(PingMonitorTarget::isCurrentlyOnline).count();
        long offline = total - online;
        long paused = targets.values().stream().filter(t -> !t.isActive()).count();

        Map<String, Object> summary = new HashMap<>();
        summary.put("total", total);
        summary.put("online", online);
        summary.put("offline", offline);
        summary.put("paused", paused);

        return summary;
    }

    public List<HistoryEntry> getHistory(String targetId, int limit) {
        PingMonitorTarget target = targets.get(targetId);
        if (target != null) {
            return target.getRecentHistory(limit);
        }
        return new ArrayList<>();
    }

    private PingMonitorResult executePing(PingMonitorTarget target) {
        PingMonitorResult result = new PingMonitorResult(target.getId(), target.getTarget());

        String os = System.getProperty("os.name").toLowerCase();
        ProcessBuilder processBuilder;

        int packetCount = target.getConfig().getPacketCount();

        if (os.contains("win")) {
            processBuilder = new ProcessBuilder(pingCommandWindows, "-n", String.valueOf(packetCount), target.getTarget());
        } else {
            processBuilder = new ProcessBuilder(pingCommandLinux, "-c", String.valueOf(packetCount), target.getTarget());
        }

        try {
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            int exitCode = process.waitFor();
            String outputStr = output.toString();

            if (exitCode == 0) {
                result.setOnline(true);
                parsePingOutput(outputStr, result, os.contains("win"));
            } else {
                result.setOnline(false);
                result.setErrorMessage("Ping falhou com código de saída: " + exitCode);
            }

        } catch (IOException | InterruptedException e) {
            logger.error("Erro ao executar ping para {}: {}", target.getTarget(), e.getMessage(), e);
            result.setOnline(false);
            result.setErrorMessage(e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }

        return result;
    }

    private void parsePingOutput(String output, PingMonitorResult result, boolean isWindows) {
        if (isWindows) {
            parseWindowsPing(output, result);
        } else {
            parseLinuxPing(output, result);
        }
    }

    private void parseWindowsPing(String output, PingMonitorResult result) {
        Pattern timePattern = Pattern.compile("tempo[<=](\\d+)ms", Pattern.CASE_INSENSITIVE);
        Pattern lossPattern = Pattern.compile("\\((\\d+)% de perda\\)", Pattern.CASE_INSENSITIVE);

        Matcher timeMatcher = timePattern.matcher(output);
        double totalTime = 0;
        int count = 0;

        while (timeMatcher.find()) {
            totalTime += Double.parseDouble(timeMatcher.group(1));
            count++;
        }

        if (count > 0) {
            result.setLatencyMs(totalTime / count);
        }

        Matcher lossMatcher = lossPattern.matcher(output);
        if (lossMatcher.find()) {
            result.setPacketLoss(Double.parseDouble(lossMatcher.group(1)));
        } else {
            result.setPacketLoss(0.0);
        }
    }

    private void parseLinuxPing(String output, PingMonitorResult result) {
        Pattern timePattern = Pattern.compile("time=(\\d+\\.?\\d*)\\s*ms", Pattern.CASE_INSENSITIVE);
        Pattern lossPattern = Pattern.compile("(\\d+)%\\s+packet loss", Pattern.CASE_INSENSITIVE);

        Matcher timeMatcher = timePattern.matcher(output);
        double totalTime = 0;
        int count = 0;

        while (timeMatcher.find()) {
            totalTime += Double.parseDouble(timeMatcher.group(1));
            count++;
        }

        if (count > 0) {
            result.setLatencyMs(totalTime / count);
        }

        Matcher lossMatcher = lossPattern.matcher(output);
        if (lossMatcher.find()) {
            result.setPacketLoss(Double.parseDouble(lossMatcher.group(1)));
        } else {
            result.setPacketLoss(0.0);
        }
    }
}
