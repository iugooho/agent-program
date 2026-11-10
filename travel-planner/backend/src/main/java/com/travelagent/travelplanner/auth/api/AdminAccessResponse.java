package com.travelagent.travelplanner.auth.api;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 管理员权限校验结果。
 *
 * @param username 当前登录账号
 * @param role 当前角色
 * @param accessGranted 是否通过管理员校验
 * @param message 校验说明
 */
public record AdminAccessResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String username,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String role,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean accessGranted,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String message) {
}
