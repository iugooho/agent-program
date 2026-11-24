package com.travelagent.travelplanner.rbac.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 更新部门请求。
 *
 * @param parentId 父部门主键
 * @param name 部门名称
 * @param leaderUserId 负责人用户主键
 * @param status 状态
 * @param sortOrder 排序值
 */
public record DepartmentUpdateRequest(
        Long parentId,
        @NotBlank @Size(max = 100) String name,
        Long leaderUserId,
        @Size(max = 20) String status,
        Integer sortOrder) {
}
