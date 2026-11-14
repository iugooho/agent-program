package com.travelagent.travelplanner.rbac.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 按业务模块分组的权限。
 *
 * @param module 模块名称
 * @param permissions 权限点
 */
public record RbacPermissionGroupResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String module,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        List<RbacPermissionResponse> permissions) {
}
