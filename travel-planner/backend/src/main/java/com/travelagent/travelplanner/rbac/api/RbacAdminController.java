package com.travelagent.travelplanner.rbac.api;

import com.travelagent.travelplanner.api.ApiResponse;
import com.travelagent.travelplanner.rbac.application.RbacService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 超级管理员 RBAC 配置接口。 */
@RestController
@RequestMapping("/api/v1/admin/rbac")
@PreAuthorize("hasRole('SUPER_ADMIN') and hasAuthority('admin.rbac.manage')")
@Tag(name = "RBAC 权限管理", description = "超级管理员分配用户角色与角色权限")
public class RbacAdminController {

    private final RbacService rbacService;

    /**
     * 创建权限管理控制器。
     *
     * @param rbacService RBAC 服务
     */
    public RbacAdminController(RbacService rbacService) {
        this.rbacService = rbacService;
    }

    /**
     * 获取权限分配页面数据。
     *
     * @return 角色、权限分组和用户
     */
    @GetMapping("/overview")
    @Operation(summary = "获取 RBAC 权限分配数据")
    public ApiResponse<RbacOverviewResponse> overview() {
        return ApiResponse.ok(rbacService.overview());
    }

    /**
     * 更新角色权限。
     *
     * @param roleId 角色主键
     * @param request 权限主键
     * @return 空响应
     */
    @PutMapping("/roles/{roleId}/permissions")
    @Operation(summary = "更新角色权限")
    public ApiResponse<Void> updateRolePermissions(
            @PathVariable long roleId,
            @Valid @RequestBody RolePermissionsRequest request) {
        rbacService.updateRolePermissions(roleId, request.permissionIds());
        return ApiResponse.ok(null);
    }

    /**
     * 更新用户角色。
     *
     * @param userId 用户主键
     * @param request 角色主键
     * @return 空响应
     */
    @PutMapping("/users/{userId}/roles")
    @Operation(summary = "更新用户角色")
    public ApiResponse<Void> updateUserRoles(
            @PathVariable long userId,
            @Valid @RequestBody UserRolesRequest request) {
        rbacService.updateUserRoles(userId, request.roleIds());
        return ApiResponse.ok(null);
    }
}
