package com.travelagent.travelplanner.rbac.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 用户及其角色。
 *
 * @param id 用户主键
 * @param username 登录账号
 * @param nickname 昵称
 * @param email 邮箱
 * @param status 状态
 * @param roleIds 已分配角色主键
 * @param roleCodes 已分配角色编码
 */
public record RbacUserResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String username,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String nickname,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String email,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<Long> roleIds,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<String> roleCodes) {
}
