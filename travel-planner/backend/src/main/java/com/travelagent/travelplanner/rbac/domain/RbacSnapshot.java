package com.travelagent.travelplanner.rbac.domain;

import java.util.List;
import java.util.Map;

/**
 * 权限配置页所需的一次性快照。
 *
 * @param roles 角色
 * @param permissions 权限点
 * @param users 用户
 * @param rolePermissionIds 角色到权限主键的映射
 * @param userRoleIds 用户到角色主键的映射
 */
public record RbacSnapshot(
        List<RbacRole> roles,
        List<RbacPermission> permissions,
        List<RbacUser> users,
        Map<Long, List<Long>> rolePermissionIds,
        Map<Long, List<Long>> userRoleIds) {
}
