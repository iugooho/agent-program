package com.travelagent.travelplanner.auth.api;

import com.travelagent.travelplanner.api.ApiResponse;
import com.travelagent.travelplanner.auth.application.AccountDeletionService;
import com.travelagent.travelplanner.auth.application.AccountSecurityService;
import com.travelagent.travelplanner.auth.application.ClientContext;
import com.travelagent.travelplanner.auth.application.NotificationPreferencesService;
import com.travelagent.travelplanner.auth.application.UserDataExportService;
import com.travelagent.travelplanner.auth.config.AuthProperties;
import com.travelagent.travelplanner.auth.infrastructure.security.AuthenticatedUser;
import com.travelagent.travelplanner.auth.infrastructure.security.RequestIdentity;
import com.travelagent.travelplanner.common.error.BusinessException;
import com.travelagent.travelplanner.common.error.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 个人中心安全、通知、数据导出与账号注销接口。 */
@RestController
@RequestMapping("/api/v1/users/me")
@Tag(name = "用户", description = "当前登录用户资料")
public class AccountCenterController {

    private final AccountSecurityService accountSecurityService;
    private final NotificationPreferencesService notificationPreferencesService;
    private final UserDataExportService userDataExportService;
    private final AccountDeletionService accountDeletionService;
    private final AuthProperties authProperties;

    /**
     * 创建账号中心控制器。
     *
     * @param accountSecurityService 账号安全服务
     * @param notificationPreferencesService 通知偏好服务
     * @param userDataExportService 数据导出服务
     * @param accountDeletionService 账号注销服务
     * @param authProperties 认证配置
     */
    public AccountCenterController(
            AccountSecurityService accountSecurityService,
            NotificationPreferencesService notificationPreferencesService,
            UserDataExportService userDataExportService,
            AccountDeletionService accountDeletionService,
            AuthProperties authProperties) {
        this.accountSecurityService = accountSecurityService;
        this.notificationPreferencesService = notificationPreferencesService;
        this.userDataExportService = userDataExportService;
        this.accountDeletionService = accountDeletionService;
        this.authProperties = authProperties;
    }

    /**
     * 查询有效登录会话。
     *
     * @param authentication 当前认证信息
     * @return 会话列表
     */
    @GetMapping("/sessions")
    @PreAuthorize("hasAuthority('user.profile.read')")
    @Operation(summary = "查询当前用户登录会话")
    public ApiResponse<List<UserSessionResponse>> sessions(Authentication authentication) {
        AuthenticatedUser user = currentUser(authentication);
        return ApiResponse.ok(accountSecurityService.sessions(user.id(), user.sessionId()));
    }

    /**
     * 撤销指定登录会话。
     *
     * @param sessionId 会话标识
     * @param authentication 当前认证信息
     * @param request HTTP 请求
     * @return 空成功响应
     */
    @DeleteMapping("/sessions/{sessionId}")
    @PreAuthorize("hasAuthority('user.profile.update')")
    @Operation(summary = "撤销当前用户指定登录会话")
    public ApiResponse<Void> revokeSession(
            @PathVariable String sessionId,
            Authentication authentication,
            HttpServletRequest request) {
        AuthenticatedUser user = currentUser(authentication);
        accountSecurityService.revokeSession(user.id(), sessionId, clientContext(request));
        return ApiResponse.ok(null);
    }

    /**
     * 查询最近安全事件。
     *
     * @param authentication 当前认证信息
     * @return 安全事件
     */
    @GetMapping("/security-log")
    @PreAuthorize("hasAuthority('user.profile.read')")
    @Operation(summary = "查询当前用户账号安全日志")
    public ApiResponse<List<SecurityEventResponse>> securityLog(Authentication authentication) {
        AuthenticatedUser user = currentUser(authentication);
        return ApiResponse.ok(accountSecurityService.securityEvents(user.id()));
    }

    /**
     * 查询通知偏好。
     *
     * @param authentication 当前认证信息
     * @return 通知偏好
     */
    @GetMapping("/notification-preferences")
    @PreAuthorize("hasAuthority('user.profile.read')")
    @Operation(summary = "获取当前用户通知偏好")
    public ApiResponse<NotificationPreferencesResponse> notificationPreferences(
            Authentication authentication) {
        AuthenticatedUser user = currentUser(authentication);
        return ApiResponse.ok(NotificationPreferencesResponse.from(
                notificationPreferencesService.get(user.id())));
    }

    /**
     * 更新通知偏好。
     *
     * @param request 更新请求
     * @param authentication 当前认证信息
     * @return 通知偏好
     */
    @PutMapping("/notification-preferences")
    @PreAuthorize("hasAuthority('user.profile.update')")
    @Operation(summary = "更新当前用户通知偏好")
    public ApiResponse<NotificationPreferencesResponse> updateNotificationPreferences(
            @Valid @RequestBody NotificationPreferencesRequest request,
            Authentication authentication) {
        AuthenticatedUser user = currentUser(authentication);
        return ApiResponse.ok(NotificationPreferencesResponse.from(
                notificationPreferencesService.update(user.id(), request)));
    }

    /**
     * 导出当前用户数据。
     *
     * @param authentication 当前认证信息
     * @return 数据导出
     */
    @GetMapping("/export")
    @PreAuthorize("hasAuthority('user.profile.read')")
    @Operation(summary = "导出当前用户数据")
    public ApiResponse<UserDataExportResponse> export(Authentication authentication) {
        AuthenticatedUser user = currentUser(authentication);
        return ApiResponse.ok(userDataExportService.export(user.id()));
    }

    /**
     * 注销当前用户账号。
     *
     * @param request 注销请求
     * @param authentication 当前认证信息
     * @param httpRequest HTTP 请求
     * @return 空成功响应
     */
    @PostMapping("/deletion")
    @PreAuthorize("hasAuthority('user.profile.update')")
    @Operation(summary = "注销当前用户账号")
    public ApiResponse<Void> deleteAccount(
            @Valid @RequestBody AccountDeletionRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        AuthenticatedUser user = currentUser(authentication);
        accountDeletionService.delete(
                user.id(),
                request.verificationCode(),
                clientContext(httpRequest));
        return ApiResponse.ok(null);
    }

    private AuthenticatedUser currentUser(Authentication authentication) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
        return user;
    }

    private ClientContext clientContext(HttpServletRequest request) {
        return new ClientContext(
                RequestIdentity.clientIp(request, authProperties.rateLimit().trustForwardedHeader()),
                request.getHeader("User-Agent"));
    }
}
