package com.travelagent.travelplanner.auth.infrastructure.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.travelagent.travelplanner.auth.config.AuthProperties;
import com.travelagent.travelplanner.auth.domain.CaptchaPurpose;
import com.travelagent.travelplanner.auth.domain.EmailPurpose;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** 使用真实 Redis 的验证码和会话撤销冒烟测试，仅在显式开启时运行。 */
@EnabledIfEnvironmentVariable(named = "APP_REDIS_LIVE_TEST", matches = "true")
class RedisStoresLiveTest {

    @Test
    @DisplayName("本机 Redis 完成验证码冷却、消费和会话撤销闭环")
    void redisStoresWorkEndToEnd() {
        AuthProperties.Redis options = new AuthProperties.Redis(
                true,
                requiredEnvironment("APP_REDIS_HOST"),
                Integer.parseInt(requiredEnvironment("APP_REDIS_PORT")),
                requiredEnvironment("APP_REDIS_PASSWORD"),
                Integer.parseInt(requiredEnvironment("APP_REDIS_DATABASE")),
                Boolean.parseBoolean(requiredEnvironment("APP_REDIS_SSL")),
                Duration.ofSeconds(2));
        SimpleRedisClient redis = new SimpleRedisClient(new AuthProperties(
                null,
                null,
                options,
                null,
                new AuthProperties.Captcha(
                        Duration.ofMinutes(2),
                        3,
                        Duration.ofMinutes(10)),
                new AuthProperties.RateLimit(true, false, 120, 60, 20)));
        redis.ping();

        RedisEmailCodeStore emailCodes = new RedisEmailCodeStore(redis);
        String email = UUID.randomUUID() + "@example.com";
        assertTrue(emailCodes.tryReserveSend(email, EmailPurpose.LOGIN, Duration.ofSeconds(2)));
        assertFalse(emailCodes.tryReserveSend(email, EmailPurpose.LOGIN, Duration.ofSeconds(2)));
        emailCodes.saveCode(email, EmailPurpose.LOGIN, "correct-hash", Duration.ofSeconds(30));
        assertFalse(emailCodes.consumeCode(email, EmailPurpose.LOGIN, "wrong-hash"));
        assertTrue(emailCodes.consumeCode(email, EmailPurpose.LOGIN, "correct-hash"));
        assertFalse(emailCodes.consumeCode(email, EmailPurpose.LOGIN, "correct-hash"));

        RedisSessionRevocationStore sessions = new RedisSessionRevocationStore(redis);
        String sessionId = UUID.randomUUID().toString();
        sessions.revoke(sessionId, Duration.ofSeconds(30));
        assertTrue(sessions.isRevoked(sessionId));
        assertTrue(redis.compareAndDelete("travel:auth:revoked-session:" + sessionId, "1"));
        assertFalse(sessions.isRevoked(sessionId));

        RedisCounterStore counters = new RedisCounterStore(redis);
        String counterKey = "live:" + UUID.randomUUID();
        assertEquals(1, counters.increment(counterKey, Duration.ofSeconds(30)));
        assertEquals(2, counters.increment(counterKey, Duration.ofSeconds(30)));
        assertEquals(2, counters.get(counterKey));
        counters.delete(counterKey);
        assertEquals(0, counters.get(counterKey));

        RedisCaptchaStore captchas = new RedisCaptchaStore(redis);
        String challengeId = UUID.randomUUID().toString();
        String clientKey = "live-client";
        captchas.saveChallenge(
                challengeId,
                clientKey,
                CaptchaPurpose.EMAIL_CODE,
                "answer-hash",
                Duration.ofSeconds(30));
        assertFalse(captchas.consumeChallenge(
                challengeId,
                clientKey,
                CaptchaPurpose.EMAIL_CODE,
                "wrong"));
        assertTrue(captchas.consumeChallenge(
                challengeId,
                clientKey,
                CaptchaPurpose.EMAIL_CODE,
                "answer-hash"));
        assertFalse(captchas.consumeChallenge(
                challengeId,
                clientKey,
                CaptchaPurpose.EMAIL_CODE,
                "answer-hash"));
    }

    private String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " 未配置");
        }
        return value;
    }
}
