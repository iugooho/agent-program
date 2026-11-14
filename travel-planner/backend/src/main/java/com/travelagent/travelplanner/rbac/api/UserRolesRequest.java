package com.travelagent.travelplanner.rbac.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 更新用户角色请求。
 *
 * @param roleIds 角色主键
 */
public record UserRolesRequest(
        @NotNull
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<Long> roleIds) {
}
