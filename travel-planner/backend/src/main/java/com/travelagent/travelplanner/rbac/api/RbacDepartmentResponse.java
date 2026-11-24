package com.travelagent.travelplanner.rbac.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 部门响应。
 *
 * @param id 主键
 * @param parentId 父部门主键
 * @param code 部门编码
 * @param name 部门名称
 * @param leaderUserId 负责人用户主键
 * @param status 状态
 * @param sortOrder 排序值
 * @param memberIds 成员用户主键
 */
public record RbacDepartmentResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long id,
        Long parentId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String code,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        Long leaderUserId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int sortOrder,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<Long> memberIds) {
}
