package org.project.tiiasuporte.pingmonitor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class PingMonitorTarget {
    private String id;
    private String target;
    private String name;
    private boolean active;
    private boolean currentlyOnline;
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdate;
    private PingTargetConfig config;
    private PingStatistics statistics;
    private LinkedList<HistoryEntry> history;
    private static final int MAX_HISTORY_SIZE = 100;

    public PingMonitorTarget() {
        this.id = UUID.randomUUID().toString();
        this.active = true;
        this.currentlyOnline = false;
        this.createdAt = LocalDateTime.now();
        this.lastUpdate = LocalDateTime.now();
        this.config = new PingTargetConfig();
        this.statistics = new PingStatistics();
        this.history = new LinkedList<>();
    }

    public PingMonitorTarget(String target) {
        this();
        this.target = target;
    }

    public PingMonitorTarget(String target, String name) {
        this();
        this.target = target;
        this.name = name;
    }

    public void addHistoryEntry(PingMonitorResult result) {
        history.addFirst(new HistoryEntry(result));
        if (history.size() > MAX_HISTORY_SIZE) {
            history.removeLast();
        }

        // Update statistics
        boolean wasOnline = currentlyOnline;
        currentlyOnline = result.isOnline();
        statistics.updateWithResult(result, wasOnline);
    }

    public List<HistoryEntry> getRecentHistory(int limit) {
        return new ArrayList<>(history.subList(0, Math.min(limit, history.size())));
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(LocalDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isCurrentlyOnline() {
        return currentlyOnline;
    }

    public void setCurrentlyOnline(boolean currentlyOnline) {
        this.currentlyOnline = currentlyOnline;
    }

    public PingTargetConfig getConfig() {
        return config;
    }

    public void setConfig(PingTargetConfig config) {
        this.config = config;
    }

    public PingStatistics getStatistics() {
        return statistics;
    }

    public void setStatistics(PingStatistics statistics) {
        this.statistics = statistics;
    }

    public LinkedList<HistoryEntry> getHistory() {
        return history;
    }

    public void setHistory(LinkedList<HistoryEntry> history) {
        this.history = history;
    }
}
