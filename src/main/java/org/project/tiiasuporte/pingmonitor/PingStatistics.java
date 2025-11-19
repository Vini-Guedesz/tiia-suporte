package org.project.tiiasuporte.pingmonitor;

import java.time.LocalDateTime;

public class PingStatistics {
    private double uptimePercentage;
    private double minLatency;
    private double maxLatency;
    private double avgLatency;
    private int totalChecks;
    private int successfulChecks;
    private int failedChecks;
    private int connectionDrops;
    private LocalDateTime lastOnline;
    private LocalDateTime lastOffline;

    public PingStatistics() {
        this.minLatency = Double.MAX_VALUE;
        this.maxLatency = 0.0;
        this.avgLatency = 0.0;
        this.totalChecks = 0;
        this.successfulChecks = 0;
        this.failedChecks = 0;
        this.connectionDrops = 0;
        this.uptimePercentage = 100.0;
    }

    public void updateWithResult(PingMonitorResult result, boolean wasOnline) {
        totalChecks++;

        if (result.isOnline()) {
            successfulChecks++;
            lastOnline = LocalDateTime.now();

            if (result.getLatencyMs() != null) {
                double latency = result.getLatencyMs();
                if (latency < minLatency) minLatency = latency;
                if (latency > maxLatency) maxLatency = latency;

                // Calculate running average
                avgLatency = ((avgLatency * (successfulChecks - 1)) + latency) / successfulChecks;
            }

            // Detect reconnection
            if (!wasOnline && totalChecks > 1) {
                connectionDrops++;
            }
        } else {
            failedChecks++;
            lastOffline = LocalDateTime.now();
        }

        // Calculate uptime percentage
        uptimePercentage = (totalChecks > 0) ? (successfulChecks * 100.0 / totalChecks) : 100.0;
    }

    public void reset() {
        this.minLatency = Double.MAX_VALUE;
        this.maxLatency = 0.0;
        this.avgLatency = 0.0;
        this.totalChecks = 0;
        this.successfulChecks = 0;
        this.failedChecks = 0;
        this.connectionDrops = 0;
        this.uptimePercentage = 100.0;
    }

    // Getters and setters
    public double getUptimePercentage() {
        return uptimePercentage;
    }

    public void setUptimePercentage(double uptimePercentage) {
        this.uptimePercentage = uptimePercentage;
    }

    public double getMinLatency() {
        return minLatency == Double.MAX_VALUE ? 0.0 : minLatency;
    }

    public void setMinLatency(double minLatency) {
        this.minLatency = minLatency;
    }

    public double getMaxLatency() {
        return maxLatency;
    }

    public void setMaxLatency(double maxLatency) {
        this.maxLatency = maxLatency;
    }

    public double getAvgLatency() {
        return avgLatency;
    }

    public void setAvgLatency(double avgLatency) {
        this.avgLatency = avgLatency;
    }

    public int getTotalChecks() {
        return totalChecks;
    }

    public void setTotalChecks(int totalChecks) {
        this.totalChecks = totalChecks;
    }

    public int getSuccessfulChecks() {
        return successfulChecks;
    }

    public void setSuccessfulChecks(int successfulChecks) {
        this.successfulChecks = successfulChecks;
    }

    public int getFailedChecks() {
        return failedChecks;
    }

    public void setFailedChecks(int failedChecks) {
        this.failedChecks = failedChecks;
    }

    public int getConnectionDrops() {
        return connectionDrops;
    }

    public void setConnectionDrops(int connectionDrops) {
        this.connectionDrops = connectionDrops;
    }

    public LocalDateTime getLastOnline() {
        return lastOnline;
    }

    public void setLastOnline(LocalDateTime lastOnline) {
        this.lastOnline = lastOnline;
    }

    public LocalDateTime getLastOffline() {
        return lastOffline;
    }

    public void setLastOffline(LocalDateTime lastOffline) {
        this.lastOffline = lastOffline;
    }
}
