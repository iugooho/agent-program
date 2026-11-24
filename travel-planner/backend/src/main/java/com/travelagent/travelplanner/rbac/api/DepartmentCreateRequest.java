package com.travelagent.travelplanner.rbac.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 创建部门请求。
 *
 * @param parentId 父部门主键
 * @param code 部门编码
 * @param name 部门名称
 * @param leaderUserId 负责人用户主键
 * @param sortOrder 排序值
 */
public record DepartmentCreateRequest(
        Long parentId,
        @NotBlank
        @Size(max = 50)
        @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "code 必须为大写字母、数字或下划线")
        @Schema(example = "PRODUCT") String code,
        @NotBlank @Size(max = 100) String name,
        Long leaderUserId,
        Integer sortOrder) {
}
