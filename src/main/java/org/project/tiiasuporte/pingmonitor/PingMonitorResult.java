package org.project.tiiasuporte.pingmonitor;

import java.time.LocalDateTime;

public class PingMonitorResult {
    private String targetId;
    private String target;
    private boolean online;
    private Double latencyMs;
    private Double packetLoss;
    private String errorMessage;
    private LocalDateTime timestamp;

    public PingMonitorResult() {
        this.timestamp = LocalDateTime.now();
    }

    public PingMonitorResult(String targetId, String target) {
        this();
        this.targetId = targetId;
        this.target = target;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
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

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
