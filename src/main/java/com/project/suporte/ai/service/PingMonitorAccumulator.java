package com.project.suporte.ai.service;

import com.project.suporte.ai.dto.PingMonitorEventDTO;

import java.time.Instant;

class PingMonitorAccumulator {

    private final String target;
    private long attempts;
    private long successfulAttempts;
    private long failedAttempts;
    private long outages;
    private int consecutiveFailures;
    private boolean connected;
    private Boolean previousConnected;
    private double totalLatencyMs;

    PingMonitorAccumulator(String target) {
        this.target = target;
    }

    PingMonitorEventDTO started(String message) {
        return new PingMonitorEventDTO(
                "started",
                target,
                "WAITING",
                false,
                null,
                null,
                attempts,
                successfulAttempts,
                failedAttempts,
                outages,
                0.0,
                consecutiveFailures,
                message,
                Instant.now()
        );
    }

    PingMonitorEventDTO sample(PingProbeResult probeResult) {
        attempts++;

        if (probeResult.successful()) {
            successfulAttempts++;
            connected = true;
            consecutiveFailures = 0;
            if (probeResult.latencyMs() != null) {
                totalLatencyMs += probeResult.latencyMs();
            }
        } else {
            failedAttempts++;
            connected = false;
            consecutiveFailures++;
            if (Boolean.TRUE.equals(previousConnected)) {
                outages++;
            }
        }

        previousConnected = probeResult.successful();

        return new PingMonitorEventDTO(
                "sample",
                target,
                connected ? "ONLINE" : "OFFLINE",
                connected,
                probeResult.latencyMs(),
                successfulAttempts > 0 ? round(totalLatencyMs / successfulAttempts) : null,
                attempts,
                successfulAttempts,
                failedAttempts,
                outages,
                round(attempts > 0 ? (failedAttempts * 100.0) / attempts : 0.0),
                consecutiveFailures,
                probeResult.message(),
                Instant.now()
        );
    }

    PingMonitorEventDTO completed(String message) {
        return new PingMonitorEventDTO(
                "completed",
                target,
                connected ? "ONLINE" : "STOPPED",
                connected,
                null,
                successfulAttempts > 0 ? round(totalLatencyMs / successfulAttempts) : null,
                attempts,
                successfulAttempts,
                failedAttempts,
                outages,
                round(attempts > 0 ? (failedAttempts * 100.0) / attempts : 0.0),
                consecutiveFailures,
                message,
                Instant.now()
        );
    }

    PingMonitorEventDTO error(String message) {
        return new PingMonitorEventDTO(
                "error",
                target,
                connected ? "ONLINE" : "OFFLINE",
                connected,
                null,
                successfulAttempts > 0 ? round(totalLatencyMs / successfulAttempts) : null,
                attempts,
                successfulAttempts,
                failedAttempts,
                outages,
                round(attempts > 0 ? (failedAttempts * 100.0) / attempts : 0.0),
                consecutiveFailures,
                message,
                Instant.now()
        );
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
