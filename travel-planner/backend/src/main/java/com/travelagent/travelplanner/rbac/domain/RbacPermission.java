package com.travelagent.travelplanner.rbac.domain;

/**
 * RBAC 权限点。
 *
 * @param id 权限主键
 * @param code 权限编码
 * @param name 权限名称
 * @param module 权限分组
 * @param description 权限说明
 */
public record RbacPermission(
        long id,
        String code,
        String name,
        String module,
        String description) {
}
