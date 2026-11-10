package com.travelagent.travelplanner.auth.infrastructure.cache;

import com.travelagent.travelplanner.auth.domain.CaptchaPurpose;
import com.travelagent.travelplanner.auth.domain.CaptchaStore;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** 本地和测试环境使用的人机验证存储。 */
@Component
@ConditionalOnProperty(
        name = "app.auth.redis.enabled",
        havingValue = "false",
        matchIfMissing = true)
public class InMemoryCaptchaStore implements CaptchaStore {

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();

    @Override
    public void saveChallenge(
            String challengeId,
            String clientKey,
            CaptchaPurpose purpose,
            String answerHash,
            Duration ttl) {
        entries.put(
                "challenge:" + challengeId,
                new Entry(
                        clientKey + "|" + purpose.name() + "|" + answerHash,
                        Instant.now().plus(ttl)));
    }

    @Override
    public boolean consumeChallenge(
            String challengeId,
            String clientKey,
            CaptchaPurpose purpose,
            String answerHash) {
        return consume(
                "challenge:" + challengeId,
                clientKey + "|" + purpose.name() + "|" + answerHash);
    }

    private boolean consume(String key, String expected) {
        Entry entry = entries.get(key);
        if (entry == null) {
            return false;
        }
        if (!entry.expiresAt().isAfter(Instant.now())) {
            entries.remove(key, entry);
            return false;
        }
        return entry.value().equals(expected) && entries.remove(key, entry);
    }

    private record Entry(String value, Instant expiresAt) {
    }
}
