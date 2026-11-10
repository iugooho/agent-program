package com.travelagent.travelplanner.auth.infrastructure.cache;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.travelagent.travelplanner.auth.domain.EmailPurpose;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Redis 验证码与会话撤销存储单元测试。 */
class RedisStoresTest {

    @Test
    @DisplayName("验证码键使用用途和规范化邮箱，消费后删除")
    void emailCodeStoreUsesNamespacedKeys() {
        SimpleRedisClient redis = mock(SimpleRedisClient.class);
        RedisEmailCodeStore store = new RedisEmailCodeStore(redis);
        Duration cooldown = Duration.ofSeconds(60);
        Duration ttl = Duration.ofMinutes(5);
        String baseKey = "travel:auth:email-code:LOGIN:user@example.com";
        when(redis.setIfAbsent(baseKey + ":cooldown", "1", cooldown)).thenReturn(true);
        when(redis.compareAndDelete(baseKey + ":code", "hash-value")).thenReturn(true);

        assertTrue(store.tryReserveSend(" User@Example.com ", EmailPurpose.LOGIN, cooldown));
        store.saveCode("user@example.com", EmailPurpose.LOGIN, "hash-value", ttl);
        assertTrue(store.consumeCode("user@example.com", EmailPurpose.LOGIN, "hash-value"));

        verify(redis).setIfAbsent(baseKey + ":cooldown", "1", cooldown);
        verify(redis).set(baseKey + ":code", "hash-value", ttl);
        verify(redis).compareAndDelete(baseKey + ":code", "hash-value");
    }

    @Test
    @DisplayName("会话撤销写入带命名空间的 Redis 键")
    void sessionRevocationUsesNamespacedKey() {
        SimpleRedisClient redis = mock(SimpleRedisClient.class);
        RedisSessionRevocationStore store = new RedisSessionRevocationStore(redis);
        Duration ttl = Duration.ofDays(7);
        when(redis.exists("travel:auth:revoked-session:session-1")).thenReturn(true);

        store.revoke("session-1", ttl);

        assertTrue(store.isRevoked("session-1"));
        verify(redis).set("travel:auth:revoked-session:session-1", "1", ttl);
    }

    @Test
    @DisplayName("冷却占用失败和验证码未命中均返回 false")
    void redisMissAllowsSendAndRejectsCode() {
        SimpleRedisClient redis = mock(SimpleRedisClient.class);
        RedisEmailCodeStore store = new RedisEmailCodeStore(redis);
        when(redis.setIfAbsent("travel:auth:email-code:REGISTER:user@example.com:cooldown", "1",
                Duration.ofSeconds(60))).thenReturn(false);
        when(redis.compareAndDelete("travel:auth:email-code:REGISTER:user@example.com:code", "wrong"))
                .thenReturn(false);

        assertFalse(store.tryReserveSend(
                "user@example.com", EmailPurpose.REGISTER, Duration.ofSeconds(60)));
        assertFalse(store.consumeCode("user@example.com", EmailPurpose.REGISTER, "wrong"));
    }
}
