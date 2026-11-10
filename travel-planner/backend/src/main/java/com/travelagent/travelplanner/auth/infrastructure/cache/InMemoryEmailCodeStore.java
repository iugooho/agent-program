package com.travelagent.travelplanner.auth.infrastructure.cache;

import com.travelagent.travelplanner.auth.domain.EmailCodeStore;
import com.travelagent.travelplanner.auth.domain.EmailPurpose;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** 本地与测试环境使用的内存验证码存储。 */
@Component
@ConditionalOnProperty(name = "app.auth.redis.enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryEmailCodeStore implements EmailCodeStore {

    private final Map<String, Instant> sendReservations = new HashMap<>();
    private final Map<String, CodeEntry> codes = new HashMap<>();

    @Override
    public synchronized boolean tryReserveSend(String email, EmailPurpose purpose, Duration retryAfter) {
        String key = key(email, purpose);
        Instant now = Instant.now();
        Instant previous = sendReservations.get(key);
        if (previous != null && previous.isAfter(now)) {
            return false;
        }
        sendReservations.put(key, now.plus(retryAfter));
        return true;
    }

    @Override
    public synchronized void saveCode(String email, EmailPurpose purpose, String codeHash, Duration ttl) {
        codes.put(key(email, purpose), new CodeEntry(codeHash, Instant.now().plus(ttl)));
    }

    @Override
    public synchronized boolean consumeCode(String email, EmailPurpose purpose, String codeHash) {
        String key = key(email, purpose);
        CodeEntry entry = codes.get(key);
        if (entry == null || !entry.expiresAt().isAfter(Instant.now())) {
            codes.remove(key);
            return false;
        }
        boolean matches = MessageDigest.isEqual(
                entry.codeHash().getBytes(StandardCharsets.UTF_8),
                codeHash.getBytes(StandardCharsets.UTF_8));
        if (matches) {
            codes.remove(key);
        }
        return matches;
    }

    private String key(String email, EmailPurpose purpose) {
        return purpose.name() + ":" + email.strip().toLowerCase(Locale.ROOT);
    }

    private record CodeEntry(String codeHash, Instant expiresAt) {
    }
}
