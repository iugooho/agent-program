package com.travelagent.travelplanner.rbac.domain;

/**
 * RBAC 权限点。
 *
 * @param id 权限主键
 * @param parentId 父权限主键
 * @param code 权限编码
 * @param name 权限名称
 * @param module 权限分组
 * @param type 权限类型
 * @param routePath 对应前端路由
 * @param description 权限说明
 * @param status 权限状态
 * @param riskLevel 风险等级
 * @param builtIn 是否内置权限
 */
public record RbacPermission(
        long id,
        Long parentId,
        String code,
        String name,
        String module,
        String type,
        String routePath,
        String description,
        String status,
        String riskLevel,
        boolean builtIn) {
}
