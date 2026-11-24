package com.travelagent.travelplanner.auth.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.travelagent.travelplanner.auth.domain.UserAccount;
import com.travelagent.travelplanner.rbac.domain.EffectiveAuthorization;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 当前用户响应。
 *
 * @param id 用户主键
 * @param username 登录账号
 * @param email 登录邮箱
 * @param nickname 昵称
 * @param phone 联系电话
 * @param avatarUrl 头像访问地址
 * @param bio 个人简介
 * @param role 当前主角色
 * @param roles 全部角色
 * @param permissions 全部权限
 * @param status 状态
 * @param createTime 创建时间
 * @param lastLoginTime 最近登录时间
 */
public record UserResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String username,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String email,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String nickname,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String phone,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String avatarUrl,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String bio,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String role,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<String> roles,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<String> permissions,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime createTime,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime lastLoginTime) {

    /**
     * 从领域用户转换。
     *
     * @param user 领域用户
     * @param authorization 当前生效授权
     * @return API 响应
     */
    public static UserResponse from(UserAccount user, EffectiveAuthorization authorization) {
        String primaryRole = authorization.roles().contains("SUPER_ADMIN")
                ? "SUPER_ADMIN"
                : authorization.roles().contains("ADMIN")
                        ? "ADMIN"
                        : authorization.roles().contains("USER")
                                ? "USER"
                                : "UNASSIGNED";
        return new UserResponse(
                user.id(),
                user.username(),
                user.email(),
                user.nickname(),
                user.phone(),
                user.avatarUrl() == null ? "" : user.avatarUrl(),
                user.bio() == null ? "" : user.bio(),
                primaryRole,
                authorization.roles(),
                authorization.permissions(),
                user.status(),
                user.createTime(),
                user.lastLoginTime());
    }
}
