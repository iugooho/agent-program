package com.travelagent.travelplanner.rbac.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 角色及其已分配权限。
 *
 * @param id 角色主键
 * @param code 角色编码
 * @param name 角色名称
 * @param description 角色说明
 * @param builtIn 是否内置
 * @param status 角色状态
 * @param userCount 已绑定用户数
 * @param permissionIds 已分配权限主键
 * @param dataScope 数据范围
 * @param dataScopeDepartmentIds 自定义数据范围部门主键
 */
public record RbacRoleResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String code,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String description,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean builtIn,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int userCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<Long> permissionIds,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String dataScope,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        List<Long> dataScopeDepartmentIds) {
}
