package com.travelagent.travelplanner.auth.infrastructure.cache;

import com.travelagent.travelplanner.auth.domain.EmailCodeStore;
import com.travelagent.travelplanner.auth.domain.EmailPurpose;
import java.time.Duration;
import java.util.Locale;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** 基于 Redis 的邮箱验证码存储。 */
@Component
@ConditionalOnProperty(name = "app.auth.redis.enabled", havingValue = "true")
public class RedisEmailCodeStore implements EmailCodeStore {

    private static final String PREFIX = "travel:auth:email-code:";

    private final SimpleRedisClient redis;

    /**
     * 创建存储。
     *
     * @param redis Redis 客户端
     */
    public RedisEmailCodeStore(SimpleRedisClient redis) {
        this.redis = redis;
    }

    @Override
    public boolean tryReserveSend(String email, EmailPurpose purpose, Duration retryAfter) {
        return redis.setIfAbsent(key(email, purpose) + ":cooldown", "1", retryAfter);
    }

    @Override
    public void saveCode(String email, EmailPurpose purpose, String codeHash, Duration ttl) {
        redis.set(key(email, purpose) + ":code", codeHash, ttl);
    }

    @Override
    public boolean consumeCode(String email, EmailPurpose purpose, String codeHash) {
        return redis.compareAndDelete(key(email, purpose) + ":code", codeHash);
    }

    private String key(String email, EmailPurpose purpose) {
        return PREFIX + purpose.name() + ":" + email.strip().toLowerCase(Locale.ROOT);
    }
}
