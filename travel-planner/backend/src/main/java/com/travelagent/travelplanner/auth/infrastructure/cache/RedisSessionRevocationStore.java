package com.travelagent.travelplanner.auth.infrastructure.cache;

import com.travelagent.travelplanner.auth.domain.SessionRevocationStore;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** 基于 Redis 的登录会话撤销存储。 */
@Component
@ConditionalOnProperty(name = "app.auth.redis.enabled", havingValue = "true")
public class RedisSessionRevocationStore implements SessionRevocationStore {

    private static final String PREFIX = "travel:auth:revoked-session:";

    private final SimpleRedisClient redis;

    /**
     * 创建存储。
     *
     * @param redis Redis 客户端
     */
    public RedisSessionRevocationStore(SimpleRedisClient redis) {
        this.redis = redis;
    }

    @Override
    public void revoke(String sessionId, Duration ttl) {
        redis.set(PREFIX + sessionId, "1", ttl);
    }

    @Override
    public boolean isRevoked(String sessionId) {
        return redis.exists(PREFIX + sessionId);
    }
}
