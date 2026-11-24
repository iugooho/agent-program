package com.travelagent.travelplanner.rbac.api;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 权限依赖响应。
 *
 * @param permissionId 权限主键
 * @param requiredPermissionId 必需的前置权限主键
 */
public record RbacPermissionDependencyResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long permissionId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long requiredPermissionId) {
}
