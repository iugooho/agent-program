package com.travelagent.travelplanner.auth.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.travelagent.travelplanner.auth.infrastructure.cache.InMemoryCounterStore;
import com.travelagent.travelplanner.common.error.BusinessException;
import com.travelagent.travelplanner.common.error.ErrorCode;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** IP 限流服务测试。 */
class RateLimitServiceTest {

    @Test
    @DisplayName("超过窗口上限后返回 429")
    void rejectsRequestAfterLimit() {
        RateLimitService service = new RateLimitService(new InMemoryCounterStore());

        assertTrue(service.isAllowed("auth", "client-a", 2, Duration.ofMinutes(1)));
        assertTrue(service.isAllowed("auth", "client-a", 2, Duration.ofMinutes(1)));
        assertFalse(service.isAllowed("auth", "client-a", 2, Duration.ofMinutes(1)));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                service.requireAllowed("auth", "client-a", 2, Duration.ofMinutes(1)));
        assertEquals(ErrorCode.RATE_LIMITED, exception.errorCode());
    }
}
