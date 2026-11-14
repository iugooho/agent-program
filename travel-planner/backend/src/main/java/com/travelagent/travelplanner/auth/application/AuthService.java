package com.travelagent.travelplanner.auth.application;

import com.travelagent.travelplanner.auth.config.AuthProperties;
import com.travelagent.travelplanner.auth.domain.EmailCodeSender;
import com.travelagent.travelplanner.auth.domain.EmailCodeStore;
import com.travelagent.travelplanner.auth.domain.EmailPurpose;
import com.travelagent.travelplanner.auth.domain.CaptchaPurpose;
import com.travelagent.travelplanner.auth.domain.SessionRevocationStore;
import com.travelagent.travelplanner.auth.domain.UserAccount;
import com.travelagent.travelplanner.auth.domain.UserRepository;
import com.travelagent.travelplanner.common.error.BusinessException;
import com.travelagent.travelplanner.common.error.ErrorCode;
import java.security.SecureRandom;
import java.util.Locale;
import java.time.Duration;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 注册、登录、刷新、退出和邮箱验证码用例。 */
@Service
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final SessionRevocationStore sessionRevocationStore;
    private final EmailCodeStore emailCodeStore;
    private final EmailCodeSender emailCodeSender;
    private final AuthProperties emailProperties;
    private final CaptchaService captchaService;
    private final RiskControlService riskControlService;
    private final RateLimitService rateLimitService;

    /**
     * 创建认证服务。
     *
     * @param userRepository 用户仓储
     * @param passwordEncoder 密码编码器
     * @param tokenService 令牌服务
     * @param sessionRevocationStore 会话撤销存储
     * @param emailCodeStore 邮箱验证码存储
     * @param emailCodeSender 邮箱验证码发送器
     * @param authProperties 认证配置
     * @param captchaService 人机验证服务
     * @param riskControlService 登录风控服务
     * @param rateLimitService 限流服务
     */
    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            TokenService tokenService,
            SessionRevocationStore sessionRevocationStore,
            EmailCodeStore emailCodeStore,
            EmailCodeSender emailCodeSender,
            AuthProperties authProperties,
            CaptchaService captchaService,
            RiskControlService riskControlService,
            RateLimitService rateLimitService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.sessionRevocationStore = sessionRevocationStore;
        this.emailCodeStore = emailCodeStore;
        this.emailCodeSender = emailCodeSender;
        this.emailProperties = authProperties;
        this.captchaService = captchaService;
        this.riskControlService = riskControlService;
        this.rateLimitService = rateLimitService;
    }

    /**
     * 注册新用户并直接签发登录令牌。
     *
     * @param username 独立登录账号
     * @param password 明文密码
     * @param email 绑定邮箱
     * @param emailCode REGISTER 用途验证码
     * @return 令牌对
     */
    @Transactional
    public AuthToken register(String username, String password, String email, String emailCode) {
        requireText(username, "username");
        requireText(password, "password");
        requireText(email, "email");
        requireText(emailCode, "emailCode");
        String normalizedUsername = username.strip();
        String normalizedEmail = normalizeEmail(email);
        if (userRepository.findByUsername(normalizedUsername).isPresent()) {
            throw new BusinessException(ErrorCode.ACCOUNT_EXISTS);
        }
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new BusinessException(ErrorCode.EMAIL_EXISTS);
        }
        boolean validCode = emailCodeStore.consumeCode(
                normalizedEmail,
                EmailPurpose.REGISTER,
                EmailCodeHasher.hash(normalizedEmail, EmailPurpose.REGISTER, emailCode.strip()));
        if (!validCode) {
            throw new BusinessException(ErrorCode.EMAIL_CODE_INVALID);
        }
        try {
            userRepository.createUser(
                    normalizedUsername,
                    normalizedEmail,
                    passwordEncoder.encode(password),
                    normalizedUsername);
        } catch (DuplicateKeyException exception) {
            throwConcurrentDuplicate(normalizedUsername, normalizedEmail, exception);
        }
        UserAccount user = userRepository.findByUsername(normalizedUsername)
                .orElseThrow(() -> new IllegalStateException("注册用户写入后未能查询到"));
        userRepository.updateLastLoginTime(user.id());
        return tokenService.issue(user);
    }

    /**
     * 账号密码登录。
     *
     * @param username 独立登录账号
     * @param password 明文密码
     * @param clientKey 客户端绑定信息
     * @param captchaChallengeId 登录 CAPTCHA 挑战
     * @param captchaAnswer 登录 CAPTCHA 答案
     * @return 令牌对
     */
    @Transactional
    public AuthToken loginWithPassword(
            String username,
            String password,
            String clientKey,
            String captchaChallengeId,
            String captchaAnswer) {
        requireText(username, "username");
        requireText(password, "password");
        String normalizedUsername = username.strip();
        if (riskControlService.requiresCaptcha(normalizedUsername, clientKey)) {
            captchaService.validate(
                    captchaChallengeId,
                    captchaAnswer,
                    CaptchaPurpose.LOGIN.name(),
                    clientKey);
        }
        UserAccount user = userRepository.findByUsername(normalizedUsername).orElse(null);
        if (user == null || !passwordEncoder.matches(password, user.passwordHash())) {
            riskControlService.recordFailure(normalizedUsername, clientKey);
            throw new BusinessException(ErrorCode.BAD_CREDENTIALS);
        }
        requireActive(user);
        riskControlService.clearFailures(normalizedUsername, clientKey);
        userRepository.updateLastLoginTime(user.id());
        return tokenService.issue(user);
    }

    /**
     * 邮箱验证码登录。
     *
     * @param email 登录邮箱
     * @param emailCode 邮箱验证码
     * @return 令牌对
     */
    @Transactional
    public AuthToken loginWithEmailCode(String email, String emailCode) {
        requireText(email, "email");
        requireText(emailCode, "emailCode");
        String normalizedEmail = normalizeEmail(email);
        UserAccount user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.EMAIL_CODE_INVALID));
        boolean valid = emailCodeStore.consumeCode(
                normalizedEmail,
                EmailPurpose.LOGIN,
                EmailCodeHasher.hash(normalizedEmail, EmailPurpose.LOGIN, emailCode.strip()));
        if (!valid) {
            throw new BusinessException(ErrorCode.EMAIL_CODE_INVALID);
        }
        requireActive(user);
        userRepository.updateLastLoginTime(user.id());
        return tokenService.issue(user);
    }

    /**
     * 刷新令牌。
     *
     * @param refreshToken Refresh Token
     * @return 新令牌对
     */
    public AuthToken refresh(String refreshToken) {
        requireText(refreshToken, "refreshToken");
        RefreshTokenClaims claims = tokenService.validateRefreshToken(refreshToken);
        if (sessionRevocationStore.isRevoked(claims.sessionId())) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        UserAccount user = userRepository.findById(claims.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID));
        requireActive(user);
        return tokenService.issue(user);
    }

    /**
     * 撤销当前登录会话。
     *
     * @param sessionId 会话标识
     */
    public void logout(String sessionId) {
        requireText(sessionId, "sessionId");
        sessionRevocationStore.revoke(sessionId, tokenService.refreshTokenLifetime());
    }

    /**
     * 发送邮箱验证码。
     *
     * @param email 收件邮箱
     * @param purposeValue 用途
     * @param captchaChallengeId CAPTCHA 挑战
     * @param captchaAnswer CAPTCHA 答案
     * @param clientKey 客户端绑定信息
     * @return 有效期和冷却时间
     */
    public EmailCodeDelivery sendEmailCode(
            String email,
            String purposeValue,
            String captchaChallengeId,
            String captchaAnswer,
            String clientKey) {
        requireText(email, "email");
        captchaService.validate(
                captchaChallengeId,
                captchaAnswer,
                CaptchaPurpose.EMAIL_CODE.name(),
                clientKey);
        rateLimitService.requireAllowed("email-code-ip", clientKey, 10, Duration.ofHours(1));
        EmailPurpose purpose = EmailPurpose.fromValue(purposeValue);
        String normalizedEmail = normalizeEmail(email);
        AuthProperties.EmailCode options = emailProperties.emailCode();
        if (!emailCodeStore.tryReserveSend(normalizedEmail, purpose, options.retryAfter())) {
            throw new BusinessException(ErrorCode.EMAIL_CODE_TOO_FREQUENT);
        }
        String code = String.format(Locale.ROOT, "%06d", RANDOM.nextInt(1_000_000));
        emailCodeStore.saveCode(
                normalizedEmail,
                purpose,
                EmailCodeHasher.hash(normalizedEmail, purpose, code),
                options.ttl());
        emailCodeSender.send(normalizedEmail, purpose, code);
        return new EmailCodeDelivery(options.ttl().toSeconds(), options.retryAfter().toSeconds());
    }

    /**
     * 查询当前登录用户。
     *
     * @param userId 用户主键
     * @return 用户
     */
    public UserAccount currentUser(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHENTICATED));
    }

    private void requireActive(UserAccount user) {
        if (!user.active()) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }
    }

    private void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, field + " 不能为空");
        }
    }

    private void throwConcurrentDuplicate(
            String username,
            String email,
            DuplicateKeyException originalException) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new BusinessException(ErrorCode.ACCOUNT_EXISTS);
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new BusinessException(ErrorCode.EMAIL_EXISTS);
        }
        throw originalException;
    }

    private String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

}
