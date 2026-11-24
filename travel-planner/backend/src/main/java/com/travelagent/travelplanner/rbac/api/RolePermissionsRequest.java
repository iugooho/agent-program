package com.travelagent.travelplanner.rbac.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 更新角色权限请求。
 *
 * @param permissionIds 权限主键
 * @param confirmHighRisk 是否确认新增高风险权限
 */
public record RolePermissionsRequest(
        @NotNull
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<Long> permissionIds,
        Boolean confirmHighRisk) {
}
