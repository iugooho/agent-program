package com.travelagent.travelplanner.rbac.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 权限分配页面数据。
 *
 * @param roles 角色
 * @param permissionGroups 权限分组
 * @param permissionPackages 权限包
 * @param permissionDependencies 权限依赖
 * @param departments 部门
 * @param users 用户
 */
public record RbacOverviewResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<RbacRoleResponse> roles,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        List<RbacPermissionGroupResponse> permissionGroups,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        List<RbacPermissionPackageResponse> permissionPackages,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        List<RbacPermissionDependencyResponse> permissionDependencies,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        List<RbacDepartmentResponse> departments,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<RbacUserResponse> users) {
}
