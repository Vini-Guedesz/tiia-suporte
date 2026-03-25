package com.project.suporte.ai.support;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class ExpiringCache<K, V> {

    private final Duration ttl;
    private final ConcurrentHashMap<K, Entry<V>> storage = new ConcurrentHashMap<>();

    public ExpiringCache(Duration ttl) {
        this.ttl = ttl;
    }

    public V get(K key, Supplier<V> supplier) {
        if (ttl.isZero() || ttl.isNegative()) {
            return supplier.get();
        }

        Instant now = Instant.now();
        Entry<V> cached = storage.get(key);
        if (cached != null && cached.expiresAt().isAfter(now)) {
            return cached.value();
        }

        Entry<V> refreshed = storage.compute(key, (ignored, current) -> {
            Instant currentTime = Instant.now();
            if (current != null && current.expiresAt().isAfter(currentTime)) {
                return current;
            }
            return new Entry<>(supplier.get(), currentTime.plus(ttl));
        });

        return refreshed.value();
    }

    private record Entry<V>(V value, Instant expiresAt) {
    }
}
