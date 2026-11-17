package com.travelagent.travelplanner.rbac.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 更新角色基本信息请求。
 *
 * @param name 角色名称
 * @param description 角色说明
 */
public record RoleUpdateRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description) {
}
