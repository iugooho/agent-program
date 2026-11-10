package com.travelagent.travelplanner.auth.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelagent.travelplanner.auth.application.AccessTokenClaims;
import com.travelagent.travelplanner.auth.application.AuthToken;
import com.travelagent.travelplanner.auth.application.RefreshTokenClaims;
import com.travelagent.travelplanner.auth.config.AuthProperties;
import com.travelagent.travelplanner.auth.domain.UserAccount;
import com.travelagent.travelplanner.common.error.BusinessException;
import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** JWT 签发与校验测试。 */
class JwtTokenServiceTest {

    private final JwtTokenService service = new JwtTokenService(new ObjectMapper(), properties());

    @Test
    @DisplayName("访问令牌和刷新令牌可以分别校验")
    void issuedTokensCanBeValidated() {
        AuthToken token = service.issue(user());

        AccessTokenClaims access = service.validateAccessToken(token.accessToken());
        RefreshTokenClaims refresh = service.validateRefreshToken(token.refreshToken());

        assertEquals(7, access.userId());
        assertEquals(7, refresh.userId());
        assertEquals(access.sessionId(), refresh.sessionId());
        assertEquals("Bearer", token.tokenType());
        assertEquals(900, token.expiresIn());
    }

    @Test
    @DisplayName("令牌被篡改后校验失败")
    void tamperedTokenIsRejected() {
        AuthToken token = service.issue(user());

        assertThrows(BusinessException.class,
                () -> service.validateAccessToken(token.accessToken() + "x"));
    }

    @Test
    @DisplayName("访问令牌不能当作刷新令牌使用")
    void tokenTypesCannotBeMixed() {
        AuthToken token = service.issue(user());

        assertNotEquals(token.accessToken(), token.refreshToken());
        assertThrows(BusinessException.class, () -> service.validateRefreshToken(token.accessToken()));
    }

    private UserAccount user() {
        return new UserAccount(
                7, "demo", "demo@example.com", "hash", "USER", "ACTIVE",
                "演示用户", null, LocalDateTime.now(), null);
    }

    private AuthProperties properties() {
        return new AuthProperties(
                new AuthProperties.Jwt(
                        "0123456789abcdef0123456789abcdef",
                        "travel-planner",
                        Duration.ofMinutes(15),
                        Duration.ofDays(7)),
                new AuthProperties.EmailCode(Duration.ofMinutes(5), Duration.ofSeconds(60)),
                new AuthProperties.Redis(false, "127.0.0.1", 6379, "", 0, false, Duration.ofSeconds(2)),
                new AuthProperties.Email(
                        "local", "", 0, "", "", "", false, false, null, null, null),
                new AuthProperties.Captcha(
                        Duration.ofMinutes(2),
                        3,
                        Duration.ofMinutes(10)),
                new AuthProperties.RateLimit(true, false, 120, 60, 20));
    }
}
