package com.travelagent.travelplanner.rbac.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 复制角色请求。
 *
 * @param code 新角色编码
 * @param name 新角色名称
 * @param description 新角色说明
 * @param copyPermissions 是否复制权限
 * @param copyMembers 是否复制成员
 */
public record RoleCopyRequest(
        @NotBlank
        @Size(min = 2, max = 50)
        @Pattern(
                regexp = "^[A-Z][A-Z0-9_]*$",
                message = "code 必须为大写字母、数字或下划线")
        @Schema(example = "OPERATOR_COPY") String code,
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description,
        Boolean copyPermissions,
        Boolean copyMembers) {
}
