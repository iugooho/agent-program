package com.travelagent.travelplanner.rbac.domain;

import java.util.List;
import java.util.Map;

/**
 * 权限配置页所需的一次性快照。
 *
 * @param roles 角色
 * @param permissions 权限点
 * @param permissionPackages 权限包
 * @param users 用户
 * @param rolePermissionIds 角色到权限主键的映射
 * @param packagePermissionIds 权限包到权限主键的映射
 * @param permissionDependencies 权限依赖
 * @param departments 部门
 * @param userRoleIds 用户到角色主键的映射
 * @param userDepartmentIds 用户到部门主键的映射
 * @param roleDataScopeDepartmentIds 角色自定义数据范围部门映射
 */
public record RbacSnapshot(
        List<RbacRole> roles,
        List<RbacPermission> permissions,
        List<RbacPermissionPackage> permissionPackages,
        List<RbacUser> users,
        Map<Long, List<Long>> rolePermissionIds,
        Map<Long, List<Long>> packagePermissionIds,
        List<RbacPermissionDependency> permissionDependencies,
        List<RbacDepartment> departments,
        Map<Long, List<Long>> userRoleIds,
        Map<Long, List<Long>> userDepartmentIds,
        Map<Long, List<Long>> roleDataScopeDepartmentIds) {
}
