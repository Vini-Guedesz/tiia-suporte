package org.project.tiiasuporte.pingmonitor;

import java.time.LocalDateTime;

public class HistoryEntry {
    private LocalDateTime timestamp;
    private boolean online;
    private Double latencyMs;
    private Double packetLoss;

    public HistoryEntry() {
        this.timestamp = LocalDateTime.now();
    }

    public HistoryEntry(boolean online, Double latencyMs, Double packetLoss) {
        this();
        this.online = online;
        this.latencyMs = latencyMs;
        this.packetLoss = packetLoss;
    }

    public HistoryEntry(PingMonitorResult result) {
        this.timestamp = result.getTimestamp();
        this.online = result.isOnline();
        this.latencyMs = result.getLatencyMs();
        this.packetLoss = result.getPacketLoss();
    }

    // Getters and setters
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public Double getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Double latencyMs) {
        this.latencyMs = latencyMs;
    }

    public Double getPacketLoss() {
        return packetLoss;
    }

    public void setPacketLoss(Double packetLoss) {
        this.packetLoss = packetLoss;
    }
}
