package com.travelagent.travelplanner.auth.api;

import com.travelagent.travelplanner.api.ApiResponse;
import com.travelagent.travelplanner.auth.infrastructure.security.AuthenticatedUser;
import com.travelagent.travelplanner.common.error.BusinessException;
import com.travelagent.travelplanner.common.error.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 管理员权限校验接口。 */
@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "后台权限", description = "管理员 RBAC 校验")
public class AdminAccessController {

    /**
     * 校验当前用户是否具备管理员权限。
     *
     * @param authentication 当前认证信息
     * @return 管理员权限校验结果
     */
    @GetMapping("/access-check")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "校验管理员权限", description = "仅 ADMIN 角色可以访问")
    public ApiResponse<AdminAccessResponse> accessCheck(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
        return ApiResponse.ok(new AdminAccessResponse(
                user.username(),
                user.role(),
                true,
                "管理员权限校验通过"));
    }
}
