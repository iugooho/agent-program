package com.travelagent.travelplanner.auth.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.travelagent.travelplanner.auth.config.AuthProperties;
import com.travelagent.travelplanner.auth.domain.EmailCodeSender;
import com.travelagent.travelplanner.auth.domain.EmailCodeStore;
import com.travelagent.travelplanner.auth.domain.EmailPurpose;
import com.travelagent.travelplanner.auth.domain.SessionRevocationStore;
import com.travelagent.travelplanner.auth.domain.UserAccount;
import com.travelagent.travelplanner.auth.domain.UserRepository;
import com.travelagent.travelplanner.common.error.BusinessException;
import com.travelagent.travelplanner.common.error.ErrorCode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

/** 认证服务单元测试。 */
class AuthServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private TokenService tokenService;
    private SessionRevocationStore sessionRevocationStore;
    private EmailCodeStore emailCodeStore;
    private EmailCodeSender emailCodeSender;
    private CaptchaService captchaService;
    private RiskControlService riskControlService;
    private RateLimitService rateLimitService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        tokenService = mock(TokenService.class);
        sessionRevocationStore = mock(SessionRevocationStore.class);
        emailCodeStore = mock(EmailCodeStore.class);
        emailCodeSender = mock(EmailCodeSender.class);
        captchaService = mock(CaptchaService.class);
        riskControlService = mock(RiskControlService.class);
        rateLimitService = mock(RateLimitService.class);
        AuthProperties properties = new AuthProperties(
                new AuthProperties.Jwt("secret-value", "travel-planner", Duration.ofMinutes(15), Duration.ofDays(7)),
                new AuthProperties.EmailCode(Duration.ofMinutes(5), Duration.ofSeconds(60)),
                new AuthProperties.Redis(false, "127.0.0.1", 6379, "", 0, false, Duration.ofSeconds(2)),
                new AuthProperties.Email(
                        "local", "", 0, "", "", "", false, false, null, null, null),
                new AuthProperties.Captcha(
                        Duration.ofMinutes(2),
                        3,
                        Duration.ofMinutes(10)),
                new AuthProperties.RateLimit(true, false, 120, 60, 20));
        authService = new AuthService(
                userRepository,
                passwordEncoder,
                tokenService,
                sessionRevocationStore,
                emailCodeStore,
                emailCodeSender,
                properties,
                captchaService,
                riskControlService,
                rateLimitService);
    }

    @Test
    @DisplayName("账号密码正确时更新登录时间并签发令牌")
    void passwordLoginIssuesToken() {
        UserAccount user = user("ACTIVE");
        AuthToken token = new AuthToken("access", "refresh", "Bearer", 900);
        when(userRepository.findByUsername("demo")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Demo123456", user.passwordHash())).thenReturn(true);
        when(tokenService.issue(user)).thenReturn(token);

        AuthToken result = authService.loginWithPassword(
                "demo", "Demo123456", "client-key", null, null);

        assertEquals(token, result);
        verify(userRepository).updateLastLoginTime(user.id());
    }

    @Test
    @DisplayName("错误密码返回接口文档定义的 1003")
    void wrongPasswordReturnsBadCredentials() {
        UserAccount user = user("ACTIVE");
        when(userRepository.findByUsername("demo")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bad-password", user.passwordHash())).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.loginWithPassword(
                        "demo", "bad-password", "client-key", null, null));

        assertEquals(ErrorCode.BAD_CREDENTIALS, exception.errorCode());
        verify(riskControlService).recordFailure("demo", "client-key");
    }

    @Test
    @DisplayName("禁用账号不能登录")
    void disabledAccountCannotLogin() {
        UserAccount user = user("DISABLED");
        when(userRepository.findByUsername("demo")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Demo123456", user.passwordHash())).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.loginWithPassword(
                        "demo", "Demo123456", "client-key", null, null));

        assertEquals(ErrorCode.ACCOUNT_DISABLED, exception.errorCode());
    }

    @Test
    @DisplayName("注册消费 REGISTER 验证码、哈希密码并签发令牌")
    void registerCreatesUserAndIssuesToken() {
        UserAccount registered = user(2L, "new_user", "new@example.com", "encoded-password", "ACTIVE");
        AuthToken token = new AuthToken("access", "refresh", "Bearer", 900);
        when(userRepository.findByUsername("new_user"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(registered));
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(emailCodeStore.consumeCode(anyString(), any(EmailPurpose.class), anyString())).thenReturn(true);
        when(passwordEncoder.encode("Password123")).thenReturn("encoded-password");
        when(tokenService.issue(registered)).thenReturn(token);

        AuthToken result = authService.register(" new_user ", "Password123", " NEW@example.com ", "123456");

        assertEquals(token, result);
        verify(emailCodeStore).consumeCode(anyString(), any(EmailPurpose.class), anyString());
        verify(userRepository).createUser("new_user", "new@example.com", "encoded-password", "new_user");
        verify(userRepository).updateLastLoginTime(registered.id());
    }

    @Test
    @DisplayName("注册账号重复返回 1001 且不消费验证码")
    void duplicateUsernameReturnsDocumentedCode() {
        when(userRepository.findByUsername("demo")).thenReturn(Optional.of(user("ACTIVE")));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.register("demo", "Password123", "new@example.com", "123456"));

        assertEquals(ErrorCode.ACCOUNT_EXISTS, exception.errorCode());
        verify(emailCodeStore, never()).consumeCode(anyString(), any(EmailPurpose.class), anyString());
    }

    @Test
    @DisplayName("注册邮箱重复返回 1002")
    void duplicateEmailReturnsDocumentedCode() {
        when(userRepository.findByUsername("new_user")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("demo@example.com")).thenReturn(Optional.of(user("ACTIVE")));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.register("new_user", "Password123", "DEMO@example.com", "123456"));

        assertEquals(ErrorCode.EMAIL_EXISTS, exception.errorCode());
    }

    @Test
    @DisplayName("注册验证码错误返回 1004 且不写入用户")
    void invalidRegistrationCodeReturnsDocumentedCode() {
        when(userRepository.findByUsername("new_user")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(emailCodeStore.consumeCode(anyString(), any(EmailPurpose.class), anyString())).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.register("new_user", "Password123", "new@example.com", "123456"));

        assertEquals(ErrorCode.EMAIL_CODE_INVALID, exception.errorCode());
        verify(userRepository, never()).createUser(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("邮箱验证码正确时可以登录")
    void emailCodeLoginIssuesToken() {
        UserAccount user = user("ACTIVE");
        AuthToken token = new AuthToken("access", "refresh", "Bearer", 900);
        when(userRepository.findByEmail("demo@example.com")).thenReturn(Optional.of(user));
        when(emailCodeStore.consumeCode(anyString(), any(EmailPurpose.class), anyString())).thenReturn(true);
        when(tokenService.issue(user)).thenReturn(token);

        AuthToken result = authService.loginWithEmailCode("demo@example.com", "123456");

        assertEquals(token, result);
    }

    @Test
    @DisplayName("验证码发送有 60 秒冷却")
    void emailCodeSendHonorsCooldown() {
        when(emailCodeStore.tryReserveSend("demo@example.com", EmailPurpose.LOGIN, Duration.ofSeconds(60)))
                .thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.sendEmailCode(
                        "demo@example.com",
                        "LOGIN",
                        "captcha-challenge",
                        "ABCDE",
                        "client-key"));

        assertEquals(ErrorCode.EMAIL_CODE_TOO_FREQUENT, exception.errorCode());
    }

    private UserAccount user(String status) {
        return user(1L, "demo", "demo@example.com", "hash", status);
    }

    private UserAccount user(
            long id,
            String username,
            String email,
            String passwordHash,
            String status) {
        return new UserAccount(
                id,
                username,
                email,
                passwordHash,
                "USER",
                status,
                username,
                null,
                LocalDateTime.now(),
                null);
    }
}
