package com.travelagent.travelplanner.auth.infrastructure.cache;

import com.travelagent.travelplanner.auth.domain.CounterStore;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** 本地和测试环境使用的原子计数器。 */
@Component
@ConditionalOnProperty(
        name = "app.auth.redis.enabled",
        havingValue = "false",
        matchIfMissing = true)
public class InMemoryCounterStore implements CounterStore {

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();

    @Override
    public synchronized long increment(String key, Duration ttl) {
        Instant now = Instant.now();
        Entry entry = entries.get(key);
        if (entry == null || !entry.expiresAt().isAfter(now)) {
            entry = new Entry(0, now.plus(ttl));
        }
        Entry updated = new Entry(entry.value() + 1, entry.expiresAt());
        entries.put(key, updated);
        return updated.value();
    }

    @Override
    public synchronized long get(String key) {
        Entry entry = entries.get(key);
        if (entry == null) {
            return 0;
        }
        if (!entry.expiresAt().isAfter(Instant.now())) {
            entries.remove(key);
            return 0;
        }
        return entry.value();
    }

    @Override
    public void delete(String key) {
        entries.remove(key);
    }

    private record Entry(long value, Instant expiresAt) {
    }
}
