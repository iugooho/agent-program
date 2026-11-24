package com.travelagent.travelplanner.rbac.api;

import com.travelagent.travelplanner.api.ApiResponse;
import com.travelagent.travelplanner.auth.infrastructure.security.AuthenticatedUser;
import com.travelagent.travelplanner.common.error.BusinessException;
import com.travelagent.travelplanner.common.error.ErrorCode;
import com.travelagent.travelplanner.rbac.application.RbacOperator;
import com.travelagent.travelplanner.rbac.application.RbacService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
     * 查询最近的授权变更记录。
     *
     * @param limit 数量上限
     * @return 审计记录
     */
    @GetMapping("/audit")
    @Operation(summary = "查询授权变更记录")
    public ApiResponse<List<RbacAuditLogResponse>> audit(
            @RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.ok(rbacService.auditLogs(limit));
    }

    /**
     * 创建部门。
     *
     * @param request 创建请求
     * @param authentication 当前用户
     * @param httpRequest HTTP 请求
     * @return 部门
     */
    @PostMapping("/departments")
    @Operation(summary = "创建部门")
    public ApiResponse<RbacDepartmentResponse> createDepartment(
            @Valid @RequestBody DepartmentCreateRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        return ApiResponse.ok(rbacService.createDepartment(
                request,
                operator(authentication, httpRequest)));
    }

    /**
     * 更新部门。
     *
     * @param departmentId 部门主键
     * @param request 更新请求
     * @param authentication 当前用户
     * @param httpRequest HTTP 请求
     * @return 部门
     */
    @PutMapping("/departments/{departmentId}")
    @Operation(summary = "更新部门")
    public ApiResponse<RbacDepartmentResponse> updateDepartment(
            @PathVariable long departmentId,
            @Valid @RequestBody DepartmentUpdateRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        return ApiResponse.ok(rbacService.updateDepartment(
                departmentId,
                request,
                operator(authentication, httpRequest)));
    }

    /**
     * 删除部门。
     *
     * @param departmentId 部门主键
     * @param authentication 当前用户
     * @param httpRequest HTTP 请求
     * @return 空响应
     */
    @DeleteMapping("/departments/{departmentId}")
    @Operation(summary = "删除部门")
    public ApiResponse<Void> deleteDepartment(
            @PathVariable long departmentId,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        rbacService.deleteDepartment(
                departmentId,
                operator(authentication, httpRequest));
        return ApiResponse.ok(null);
    }

    /**
     * 更新部门成员。
     *
     * @param departmentId 部门主键
     * @param request 成员请求
     * @param authentication 当前用户
     * @param httpRequest HTTP 请求
     * @return 空响应
     */
    @PutMapping("/departments/{departmentId}/members")
    @Operation(summary = "更新部门成员")
    public ApiResponse<Void> updateDepartmentMembers(
            @PathVariable long departmentId,
            @Valid @RequestBody DepartmentMembersRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        rbacService.updateDepartmentMembers(
                departmentId,
                request.userIds(),
                operator(authentication, httpRequest));
        return ApiResponse.ok(null);
    }

    /**
     * 创建自定义权限点。
     *
     * @param request 创建请求
     * @return 创建的权限点
     */
    @PostMapping("/permissions")
    @Operation(summary = "创建自定义权限点")
    public ApiResponse<RbacPermissionResponse> createPermission(
            @Valid @RequestBody PermissionCreateRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        return ApiResponse.ok(rbacService.createPermission(
                request,
                operator(authentication, httpRequest)));
    }

    /**
     * 删除自定义权限点。
     *
     * @param permissionId 权限主键
     * @return 空响应
     */
    @DeleteMapping("/permissions/{permissionId}")
    @Operation(summary = "删除自定义权限点")
    public ApiResponse<Void> deletePermission(
            @PathVariable long permissionId,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        rbacService.deletePermission(
                permissionId,
                operator(authentication, httpRequest));
        return ApiResponse.ok(null);
    }

    /**
     * 创建自定义角色。
     *
     * @param request 创建请求
     * @return 创建的角色
     */
    @PostMapping("/roles")
    @Operation(summary = "创建自定义角色")
    public ApiResponse<RbacRoleResponse> createRole(
            @Valid @RequestBody RoleCreateRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        return ApiResponse.ok(rbacService.createRole(
                request,
                operator(authentication, httpRequest)));
    }

    /**
     * 复制角色。
     *
     * @param roleId 源角色主键
     * @param request 复制请求
     * @param authentication 当前用户
     * @param httpRequest HTTP 请求
     * @return 新角色
     */
    @PostMapping("/roles/{roleId}/copy")
    @Operation(summary = "复制角色")
    public ApiResponse<RbacRoleResponse> copyRole(
            @PathVariable long roleId,
            @Valid @RequestBody RoleCopyRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        return ApiResponse.ok(rbacService.copyRole(
                roleId,
                request,
                operator(authentication, httpRequest)));
    }

    /**
     * 更新角色数据范围。
     *
     * @param roleId 角色主键
     * @param request 数据范围请求
     * @param authentication 当前用户
     * @param httpRequest HTTP 请求
     * @return 空响应
     */
    @PutMapping("/roles/{roleId}/data-scope")
    @Operation(summary = "更新角色数据范围")
    public ApiResponse<Void> updateRoleDataScope(
            @PathVariable long roleId,
            @Valid @RequestBody RoleDataScopeRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        rbacService.updateRoleDataScope(
                roleId,
                request,
                operator(authentication, httpRequest));
        return ApiResponse.ok(null);
    }

    /**
     * 更新自定义角色名称和说明。
     *
     * @param roleId 角色主键
     * @param request 更新请求
     * @return 更新后的角色
     */
    @PutMapping("/roles/{roleId}")
    @Operation(summary = "更新自定义角色")
    public ApiResponse<RbacRoleResponse> updateRole(
            @PathVariable long roleId,
            @Valid @RequestBody RoleUpdateRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        return ApiResponse.ok(rbacService.updateRole(
                roleId,
                request,
                operator(authentication, httpRequest)));
    }

    /**
     * 启用或停用自定义角色。
     *
     * @param roleId 角色主键
     * @param request 状态请求
     * @return 更新后的角色
     */
    @PutMapping("/roles/{roleId}/status")
    @Operation(summary = "启用或停用角色")
    public ApiResponse<RbacRoleResponse> updateRoleStatus(
            @PathVariable long roleId,
            @Valid @RequestBody RoleStatusRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        return ApiResponse.ok(rbacService.updateRoleStatus(
                roleId,
                request.status(),
                operator(authentication, httpRequest)));
    }

    /**
     * 软删除自定义角色。
     *
     * @param roleId 角色主键
     * @return 空响应
     */
    @DeleteMapping("/roles/{roleId}")
    @Operation(summary = "删除自定义角色")
    public ApiResponse<Void> deleteRole(
            @PathVariable long roleId,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        rbacService.deleteRole(
                roleId,
                operator(authentication, httpRequest));
        return ApiResponse.ok(null);
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
            @Valid @RequestBody RolePermissionsRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        rbacService.updateRolePermissions(
                roleId,
                request.permissionIds(),
                Boolean.TRUE.equals(request.confirmHighRisk()),
                operator(authentication, httpRequest));
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
            @Valid @RequestBody UserRolesRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        rbacService.updateUserRoles(
                userId,
                request.roleIds(),
                operator(authentication, httpRequest));
        return ApiResponse.ok(null);
    }

    /**
     * 更新用户所属部门。
     *
     * @param userId 用户主键
     * @param request 部门请求
     * @param authentication 当前用户
     * @param httpRequest HTTP 请求
     * @return 空响应
     */
    @PutMapping("/users/{userId}/departments")
    @Operation(summary = "更新用户所属部门")
    public ApiResponse<Void> updateUserDepartments(
            @PathVariable long userId,
            @Valid @RequestBody UserDepartmentsRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        rbacService.updateUserDepartments(
                userId,
                request.departmentIds(),
                operator(authentication, httpRequest));
        return ApiResponse.ok(null);
    }

    private RbacOperator operator(
            Authentication authentication,
            HttpServletRequest request) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
        return new RbacOperator(user.id(), user.username(), request.getRemoteAddr());
    }
}
