package com.travelagent.travelplanner.auth.api;

import com.travelagent.travelplanner.api.ApiResponse;
import com.travelagent.travelplanner.auth.application.AuthService;
import com.travelagent.travelplanner.auth.infrastructure.security.AuthenticatedUser;
import com.travelagent.travelplanner.common.error.BusinessException;
import com.travelagent.travelplanner.common.error.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 当前登录用户接口。 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "用户", description = "当前登录用户资料")
public class UserController {

    private final AuthService authService;

    /**
     * 创建控制器。
     *
     * @param authService 认证服务
     */
    public UserController(AuthService authService) {
        this.authService = authService;
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
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
        return ApiResponse.ok(UserResponse.from(authService.currentUser(user.id())));
    }
}
