package com.travelagent.travelplanner.auth.api;

import com.travelagent.travelplanner.api.ApiResponse;
import com.travelagent.travelplanner.auth.application.AuthService;
import com.travelagent.travelplanner.auth.config.AuthProperties;
import com.travelagent.travelplanner.auth.infrastructure.security.AuthenticatedUser;
import com.travelagent.travelplanner.auth.infrastructure.security.RequestIdentity;
import com.travelagent.travelplanner.common.error.BusinessException;
import com.travelagent.travelplanner.common.error.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Locale;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 注册、登录、刷新、退出和邮箱验证码接口。 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "认证", description = "账号注册、密码和邮箱验证码登录、令牌刷新与退出")
public class AuthController {

    private final AuthService authService;
    private final AuthProperties authProperties;

    /**
     * 创建控制器。
     *
     * @param authService 认证服务
     * @param authProperties 认证配置
     */
    public AuthController(AuthService authService, AuthProperties authProperties) {
        this.authService = authService;
        this.authProperties = authProperties;
    }

    /**
     * 注册新用户。
     *
     * @param request 注册请求
     * @return JWT 令牌对
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "使用独立账号、密码和 REGISTER 用途邮箱验证码注册")
    public ApiResponse<AuthTokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(AuthTokenResponse.from(authService.register(
                request.username(),
                request.password(),
                request.email(),
                request.emailCode())));
    }

    /**
     * 登录。
     *
     * @param request 登录请求
     * @return JWT 令牌对
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "支持账号密码和邮箱验证码两种模式")
    public ApiResponse<AuthTokenResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        String loginType = request.loginType().strip().toUpperCase(Locale.ROOT);
        return switch (loginType) {
            case "PASSWORD" -> ApiResponse.ok(AuthTokenResponse.from(
                    authService.loginWithPassword(
                            request.username(),
                            request.password(),
                            clientKey(httpRequest),
                            request.captchaChallengeId(),
                            request.captchaAnswer())));
            case "EMAIL_CODE" -> ApiResponse.ok(AuthTokenResponse.from(
                    authService.loginWithEmailCode(request.email(), request.emailCode())));
            default -> throw new BusinessException(ErrorCode.PARAM_INVALID, "loginType 必须是 PASSWORD 或 EMAIL_CODE");
        };
    }

    /**
     * 刷新访问令牌。
     *
     * @param request Refresh Token
     * @return 新令牌对
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新令牌")
    public ApiResponse<AuthTokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.ok(AuthTokenResponse.from(authService.refresh(request.refreshToken())));
    }

    /**
     * 退出当前登录会话。
     *
     * @param authentication 当前认证信息
     * @return 空成功响应
     */
    @PostMapping("/logout")
    @Operation(summary = "退出登录")
    public ApiResponse<Void> logout(Authentication authentication) {
        AuthenticatedUser user = currentUser(authentication);
        authService.logout(user.sessionId());
        return ApiResponse.ok(null);
    }

    /**
     * 发送邮箱验证码。
     *
     * @param request 邮箱与用途
     * @param authentication 当前认证信息，UPDATE_PROFILE 用途必填
     * @return 有效期与冷却时间
     */
    @PostMapping("/email-code")
    @Operation(summary = "发送邮箱验证码", description = "验证码保存在 Redis，5 分钟有效，60 秒内不得重复发送")
    public ApiResponse<EmailCodeResponse> sendEmailCode(
            @Valid @RequestBody EmailCodeRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        if ("UPDATE_PROFILE".equalsIgnoreCase(request.purpose().strip()) && authentication == null) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
        return ApiResponse.ok(EmailCodeResponse.from(authService.sendEmailCode(
                request.email(),
                request.purpose().strip(),
                request.captchaChallengeId(),
                request.captchaAnswer(),
                clientKey(httpRequest))));
    }

    private AuthenticatedUser currentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
        return user;
    }

    private String clientKey(HttpServletRequest request) {
        return RequestIdentity.clientKey(
                request,
                authProperties.rateLimit().trustForwardedHeader());
    }
}
