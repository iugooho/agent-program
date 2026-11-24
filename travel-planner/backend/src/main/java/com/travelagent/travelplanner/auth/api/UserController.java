package com.travelagent.travelplanner.auth.api;

import com.travelagent.travelplanner.api.ApiResponse;
import com.travelagent.travelplanner.auth.application.AuthService;
import com.travelagent.travelplanner.auth.application.ClientContext;
import com.travelagent.travelplanner.auth.application.UserOverviewService;
import com.travelagent.travelplanner.auth.application.UserProfileService;
import com.travelagent.travelplanner.auth.config.AuthProperties;
import com.travelagent.travelplanner.auth.domain.UserAccount;
import com.travelagent.travelplanner.auth.infrastructure.security.AuthenticatedUser;
import com.travelagent.travelplanner.auth.infrastructure.security.RequestIdentity;
import com.travelagent.travelplanner.common.error.BusinessException;
import com.travelagent.travelplanner.common.error.ErrorCode;
import com.travelagent.travelplanner.rbac.domain.EffectiveAuthorization;
import com.travelagent.travelplanner.rbac.domain.RbacRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 当前登录用户接口。 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "用户", description = "当前登录用户资料")
public class UserController {

    private final AuthService authService;
    private final RbacRepository rbacRepository;
    private final UserProfileService userProfileService;
    private final UserOverviewService userOverviewService;
    private final AuthProperties authProperties;

    /**
     * 创建控制器。
     *
     * @param authService 认证服务
     * @param rbacRepository RBAC 仓储
     * @param userProfileService 用户资料服务
     * @param userOverviewService 个人中心聚合服务
     * @param authProperties 认证配置
     */
    public UserController(
            AuthService authService,
            RbacRepository rbacRepository,
            UserProfileService userProfileService,
            UserOverviewService userOverviewService,
            AuthProperties authProperties) {
        this.authService = authService;
        this.rbacRepository = rbacRepository;
        this.userProfileService = userProfileService;
        this.userOverviewService = userOverviewService;
        this.authProperties = authProperties;
    }

    /**
     * 获取当前用户信息。
     *
     * @param authentication 当前认证信息
     * @return 当前用户
     */
    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息")
    public ApiResponse<UserResponse> me(Authentication authentication) {
        AuthenticatedUser user = currentUser(authentication);
        EffectiveAuthorization authorization =
                rbacRepository.findEffectiveAuthorization(user.id());
        return ApiResponse.ok(UserResponse.from(
                authService.currentUser(user.id()), authorization));
    }

    /**
     * 更新当前用户资料。
     *
     * @param request 更新请求
     * @param authentication 当前认证信息
     * @return 更新后的用户
     */
    @PutMapping("/me")
    @PreAuthorize("hasAuthority('user.profile.update')")
    @Operation(summary = "更新当前用户资料")
    public ApiResponse<UserResponse> updateProfile(
            @Valid @RequestBody ProfileUpdateRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        AuthenticatedUser authenticatedUser = currentUser(authentication);
        UserAccount updated = userProfileService.updateProfile(
                authenticatedUser.id(),
                request,
                clientContext(httpRequest));
        EffectiveAuthorization authorization =
                rbacRepository.findEffectiveAuthorization(authenticatedUser.id());
        return ApiResponse.ok(UserResponse.from(updated, authorization));
    }

    /**
     * 上传并更新当前用户头像。
     *
     * @param file 头像文件
     * @param authentication 当前认证信息
     * @return 更新后的用户
     */
    @PostMapping(value = "/me/avatar", consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('user.profile.update')")
    @Operation(summary = "更新当前用户头像")
    public ApiResponse<UserResponse> updateAvatar(
            @RequestPart("file") MultipartFile file,
            Authentication authentication) {
        AuthenticatedUser user = currentUser(authentication);
        try {
            UserAccount updated = userProfileService.updateAvatar(user.id(), file.getBytes());
            EffectiveAuthorization authorization = rbacRepository.findEffectiveAuthorization(user.id());
            return ApiResponse.ok(UserResponse.from(updated, authorization));
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "头像文件读取失败");
        }
    }

    /**
     * 获取当前用户个人中心概览。
     *
     * @param authentication 当前认证信息
     * @return 个人中心概览
     */
    @GetMapping("/me/overview")
    @PreAuthorize("hasAuthority('user.profile.read')")
    @Operation(summary = "获取当前用户个人中心概览")
    public ApiResponse<UserOverviewResponse> overview(Authentication authentication) {
        AuthenticatedUser user = currentUser(authentication);
        return ApiResponse.ok(userOverviewService.overview(user.id()));
    }

    /**
     * 获取当前用户长期偏好。
     *
     * @param authentication 当前认证信息
     * @return 用户长期偏好
     */
    @GetMapping("/me/preferences")
    @PreAuthorize("hasAuthority('user.profile.read')")
    @Operation(summary = "获取当前用户长期偏好")
    public ApiResponse<UserPreferencesResponse> preferences(Authentication authentication) {
        AuthenticatedUser user = currentUser(authentication);
        return ApiResponse.ok(UserPreferencesResponse.from(
                userProfileService.getPreferences(user.id())));
    }

    /**
     * 更新当前用户长期偏好。
     *
     * @param request 更新请求
     * @param authentication 当前认证信息
     * @return 更新后的长期偏好
     */
    @PutMapping("/me/preferences")
    @PreAuthorize("hasAuthority('user.profile.update')")
    @Operation(summary = "更新当前用户长期偏好")
    public ApiResponse<UserPreferencesResponse> updatePreferences(
            @Valid @RequestBody UserPreferencesRequest request,
            Authentication authentication) {
        AuthenticatedUser user = currentUser(authentication);
        return ApiResponse.ok(UserPreferencesResponse.from(
                userProfileService.updatePreferences(user.id(), request)));
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
