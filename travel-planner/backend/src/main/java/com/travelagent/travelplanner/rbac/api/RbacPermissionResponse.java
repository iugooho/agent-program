package com.travelagent.travelplanner.rbac.api;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 权限点。
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
public record RbacPermissionResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long id,
        Long parentId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String code,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String module,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String type,
        String routePath,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String description,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String riskLevel,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean builtIn) {
}
