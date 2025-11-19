package org.project.tiiasuporte.pingmonitor;

public class PingTargetConfig {
    private int intervalSeconds = 2;
    private int packetCount = 2;
    private double latencyThresholdMs = 100.0;
    private boolean enableAlerts = true;

    public PingTargetConfig() {
    }

    public PingTargetConfig(int intervalSeconds, int packetCount, double latencyThresholdMs, boolean enableAlerts) {
        this.intervalSeconds = intervalSeconds;
        this.packetCount = packetCount;
        this.latencyThresholdMs = latencyThresholdMs;
        this.enableAlerts = enableAlerts;
    }

    // Getters and setters
    public int getIntervalSeconds() {
        return intervalSeconds;
    }

    public void setIntervalSeconds(int intervalSeconds) {
        if (intervalSeconds < 1) intervalSeconds = 1;
        if (intervalSeconds > 60) intervalSeconds = 60;
        this.intervalSeconds = intervalSeconds;
    }

    public int getPacketCount() {
        return packetCount;
    }

    public void setPacketCount(int packetCount) {
        if (packetCount < 1) packetCount = 1;
        if (packetCount > 10) packetCount = 10;
        this.packetCount = packetCount;
    }

    public double getLatencyThresholdMs() {
        return latencyThresholdMs;
    }

    public void setLatencyThresholdMs(double latencyThresholdMs) {
        this.latencyThresholdMs = latencyThresholdMs;
    }

    public boolean isEnableAlerts() {
        return enableAlerts;
    }

    public void setEnableAlerts(boolean enableAlerts) {
        this.enableAlerts = enableAlerts;
    }
}
