package com.travelagent.travelplanner.rbac.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 权限包响应。
 *
 * @param id 主键
 * @param code 编码
 * @param name 名称
 * @param description 说明
 * @param permissionIds 包含的权限主键
 */
public record RbacPermissionPackageResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String code,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        String description,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<Long> permissionIds) {
}
