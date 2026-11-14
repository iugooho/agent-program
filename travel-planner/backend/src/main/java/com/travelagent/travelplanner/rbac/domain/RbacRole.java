package com.travelagent.travelplanner.rbac.domain;

/**
 * RBAC 角色。
 *
 * @param id 角色主键
 * @param code 角色编码
 * @param name 角色名称
 * @param description 角色说明
 * @param builtIn 是否为内置角色
 */
public record RbacRole(long id, String code, String name, String description, boolean builtIn) {
}
