package com.travelagent.travelplanner.auth.application;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.travelagent.travelplanner.auth.config.AuthProperties;
import com.travelagent.travelplanner.auth.infrastructure.cache.InMemoryCounterStore;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 登录失败风控测试。 */
class RiskControlServiceTest {

    @Test
    @DisplayName("同一账号和客户端连续失败三次后要求 CAPTCHA")
    void requiresCaptchaAfterThreshold() {
        AuthProperties properties = new AuthProperties(
                null,
                null,
                null,
                null,
                new AuthProperties.Captcha(
                        Duration.ofMinutes(2),
                        3,
                        Duration.ofMinutes(10)),
                new AuthProperties.RateLimit(true, false, 120, 60, 20));
        RiskControlService service = new RiskControlService(
                new InMemoryCounterStore(),
                properties);

        assertFalse(service.requiresCaptcha("demo", "client-a"));
        service.recordFailure("demo", "client-a");
        service.recordFailure("demo", "client-a");
        assertFalse(service.requiresCaptcha("demo", "client-a"));
        service.recordFailure("demo", "client-a");
        assertTrue(service.requiresCaptcha("demo", "client-a"));
        assertFalse(service.requiresCaptcha("demo", "client-b"));

        service.clearFailures("demo", "client-a");
        assertFalse(service.requiresCaptcha("demo", "client-a"));
    }
}
