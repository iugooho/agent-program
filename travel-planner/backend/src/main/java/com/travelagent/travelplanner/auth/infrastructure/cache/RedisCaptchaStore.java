package com.travelagent.travelplanner.auth.infrastructure.cache;

import com.travelagent.travelplanner.auth.domain.CaptchaPurpose;
import com.travelagent.travelplanner.auth.domain.CaptchaStore;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** 基于 Redis 的人机验证存储。 */
@Component
@ConditionalOnProperty(name = "app.auth.redis.enabled", havingValue = "true")
public class RedisCaptchaStore implements CaptchaStore {

    private static final String CHALLENGE_PREFIX = "travel:auth:captcha:challenge:";
    private final SimpleRedisClient redis;

    public RedisCaptchaStore(SimpleRedisClient redis) {
        this.redis = redis;
    }

    @Override
    public void saveChallenge(
            String challengeId,
            String clientKey,
            CaptchaPurpose purpose,
            String answerHash,
            Duration ttl) {
        redis.set(
                CHALLENGE_PREFIX + challengeId,
                clientKey + "|" + purpose.name() + "|" + answerHash,
                ttl);
    }

    @Override
    public boolean consumeChallenge(
            String challengeId,
            String clientKey,
            CaptchaPurpose purpose,
            String answerHash) {
        return redis.compareAndDelete(
                CHALLENGE_PREFIX + challengeId,
                clientKey + "|" + purpose.name() + "|" + answerHash);
    }
}
