package com.travelagent.travelplanner.rbac.domain;

/**
 * 权限包。
 *
 * @param id 主键
 * @param code 编码
 * @param name 名称
 * @param description 说明
 * @param status 状态
 */
public record RbacPermissionPackage(
        long id,
        String code,
        String name,
        String description,
        String status) {
}
