package com.travelagent.travelplanner.auth.infrastructure.cache;

import com.travelagent.travelplanner.auth.domain.CounterStore;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** 基于 Redis 的原子计数器。 */
@Component
@ConditionalOnProperty(name = "app.auth.redis.enabled", havingValue = "true")
public class RedisCounterStore implements CounterStore {

    private static final String PREFIX = "travel:auth:counter:";

    private final SimpleRedisClient redis;

    public RedisCounterStore(SimpleRedisClient redis) {
        this.redis = redis;
    }

    @Override
    public long increment(String key, Duration ttl) {
        return redis.incrementAndExpire(PREFIX + key, ttl);
    }

    @Override
    public long get(String key) {
        String value = redis.get(PREFIX + key);
        return value == null ? 0 : Long.parseLong(value);
    }

    @Override
    public void delete(String key) {
        redis.delete(PREFIX + key);
    }
}
