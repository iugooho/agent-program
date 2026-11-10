package com.travelagent.travelplanner.auth.infrastructure.cache;

import com.travelagent.travelplanner.auth.domain.SessionRevocationStore;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** 本地与测试环境使用的会话撤销存储。 */
@Component
@ConditionalOnProperty(name = "app.auth.redis.enabled", havingValue = "false", matchIfMissing = true)
public class InMemorySessionRevocationStore implements SessionRevocationStore {

    private final Map<String, Instant> revoked = new ConcurrentHashMap<>();

    @Override
    public void revoke(String sessionId, Duration ttl) {
        revoked.put(sessionId, Instant.now().plus(ttl));
    }

    @Override
    public boolean isRevoked(String sessionId) {
        Instant expiresAt = revoked.get(sessionId);
        if (expiresAt == null) {
            return false;
        }
        if (expiresAt.isAfter(Instant.now())) {
            return true;
        }
        revoked.remove(sessionId);
        return false;
    }
}
