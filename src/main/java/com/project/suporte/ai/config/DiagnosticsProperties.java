package com.project.suporte.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;

@Validated
@ConfigurationProperties(prefix = "diagnostics")
public class DiagnosticsProperties {

    private final Sse sse = new Sse();
    private final Async async = new Async();
    private final Portscan portscan = new Portscan();
    private final Geolocation geolocation = new Geolocation();
    private final Cache cache = new Cache();
    private final Whois whois = new Whois();

    public Sse getSse() {
        return sse;
    }

    public Async getAsync() {
        return async;
    }

    public Portscan getPortscan() {
        return portscan;
    }

    public Geolocation getGeolocation() {
        return geolocation;
    }

    public Cache getCache() {
        return cache;
    }

    public Whois getWhois() {
        return whois;
    }

    public static class Sse {
        @Min(1_000)
        private long timeoutMs = 180_000;

        public long getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(long timeoutMs) {
            this.timeoutMs = timeoutMs;
        }
    }

    public static class Async {
        @Min(1)
        private int corePoolSize = 4;
        @Min(1)
        private int maxPoolSize = 12;
        @Min(0)
        private int queueCapacity = 100;

        public int getCorePoolSize() {
            return corePoolSize;
        }

        public void setCorePoolSize(int corePoolSize) {
            this.corePoolSize = corePoolSize;
        }

        public int getMaxPoolSize() {
            return maxPoolSize;
        }

        public void setMaxPoolSize(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
        }

        public int getQueueCapacity() {
            return queueCapacity;
        }

        public void setQueueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
        }
    }

    public static class Portscan {
        @Min(1)
        private int defaultTimeoutMs = 500;
        @Min(50)
        private int maxTimeoutMs = 5_000;
        @Min(1)
        private int maxPorts = 64;
        @Min(1)
        private int threadPoolSize = 32;

        public int getDefaultTimeoutMs() {
            return defaultTimeoutMs;
        }

        public void setDefaultTimeoutMs(int defaultTimeoutMs) {
            this.defaultTimeoutMs = defaultTimeoutMs;
        }

        public int getMaxTimeoutMs() {
            return maxTimeoutMs;
        }

        public void setMaxTimeoutMs(int maxTimeoutMs) {
            this.maxTimeoutMs = maxTimeoutMs;
        }

        public int getMaxPorts() {
            return maxPorts;
        }

        public void setMaxPorts(int maxPorts) {
            this.maxPorts = maxPorts;
        }

        public int getThreadPoolSize() {
            return threadPoolSize;
        }

        public void setThreadPoolSize(int threadPoolSize) {
            this.threadPoolSize = threadPoolSize;
        }
    }

    public static class Geolocation {
        private String baseUrl = "http://ip-api.com/json";
        @Min(100)
        private int connectTimeoutMs = 2_000;
        @Min(100)
        private int readTimeoutMs = 3_000;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public int getConnectTimeoutMs() {
            return connectTimeoutMs;
        }

        public void setConnectTimeoutMs(int connectTimeoutMs) {
            this.connectTimeoutMs = connectTimeoutMs;
        }

        public int getReadTimeoutMs() {
            return readTimeoutMs;
        }

        public void setReadTimeoutMs(int readTimeoutMs) {
            this.readTimeoutMs = readTimeoutMs;
        }
    }

    public static class Cache {
        @Min(0)
        private long dnsTtlSeconds = 300;
        @Min(0)
        private long geolocationTtlSeconds = 300;
        @Min(0)
        private long whoisTtlSeconds = 600;

        public long getDnsTtlSeconds() {
            return dnsTtlSeconds;
        }

        public void setDnsTtlSeconds(long dnsTtlSeconds) {
            this.dnsTtlSeconds = dnsTtlSeconds;
        }

        public long getGeolocationTtlSeconds() {
            return geolocationTtlSeconds;
        }

        public void setGeolocationTtlSeconds(long geolocationTtlSeconds) {
            this.geolocationTtlSeconds = geolocationTtlSeconds;
        }

        public long getWhoisTtlSeconds() {
            return whoisTtlSeconds;
        }

        public void setWhoisTtlSeconds(long whoisTtlSeconds) {
            this.whoisTtlSeconds = whoisTtlSeconds;
        }
    }

    public static class Whois {
        @Min(100)
        private int connectTimeoutMs = 2_000;
        @Min(100)
        private int readTimeoutMs = 3_000;

        public int getConnectTimeoutMs() {
            return connectTimeoutMs;
        }

        public void setConnectTimeoutMs(int connectTimeoutMs) {
            this.connectTimeoutMs = connectTimeoutMs;
        }

        public int getReadTimeoutMs() {
            return readTimeoutMs;
        }

        public void setReadTimeoutMs(int readTimeoutMs) {
            this.readTimeoutMs = readTimeoutMs;
        }
    }
}
