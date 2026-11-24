package com.travelagent.travelplanner.rbac.domain;

/**
 * 权限依赖关系。
 *
 * @param permissionId 权限主键
 * @param requiredPermissionId 必需的前置权限主键
 */
public record RbacPermissionDependency(long permissionId, long requiredPermissionId) {
}
