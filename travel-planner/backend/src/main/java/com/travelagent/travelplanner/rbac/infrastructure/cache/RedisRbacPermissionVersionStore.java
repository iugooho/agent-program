package com.travelagent.travelplanner.rbac.infrastructure.cache;

import com.travelagent.travelplanner.auth.infrastructure.cache.SimpleRedisClient;
import com.travelagent.travelplanner.rbac.domain.RbacPermissionVersionStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** 基于 Redis 的跨实例授权版本存储。 */
@Component
@ConditionalOnProperty(name = "app.auth.redis.enabled", havingValue = "true")
public class RedisRbacPermissionVersionStore implements RbacPermissionVersionStore {

    private static final String KEY = "travel:rbac:permission-version";

    private final SimpleRedisClient redis;

    /**
     * 创建版本存储。
     *
     * @param redis Redis 客户端
     */
    public RedisRbacPermissionVersionStore(SimpleRedisClient redis) {
        this.redis = redis;
    }

    @Override
    public long currentVersion() {
        String value = redis.get(KEY);
        return value == null ? 1 : Long.parseLong(value);
    }

    @Override
    public long bumpVersion() {
        return redis.increment(KEY);
    }
}
