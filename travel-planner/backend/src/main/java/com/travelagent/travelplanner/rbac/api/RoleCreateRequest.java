package com.travelagent.travelplanner.rbac.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 创建自定义角色请求。
 *
 * @param code 角色编码
 * @param name 角色名称
 * @param description 角色说明
 * @param permissionIds 初始权限主键
 */
public record RoleCreateRequest(
        @NotBlank
        @Size(min = 2, max = 50)
        @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "code 必须为大写字母、数字或下划线")
        @Schema(example = "OPERATOR") String code,
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description,
        List<Long> permissionIds) {
}
