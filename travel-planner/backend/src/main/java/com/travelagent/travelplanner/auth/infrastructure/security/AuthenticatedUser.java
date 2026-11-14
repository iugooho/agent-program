package com.travelagent.travelplanner.auth.infrastructure.security;

import java.util.List;

/**
 * 已认证用户。
 *
 * @param id 用户主键
 * @param username 登录账号
 * @param role 当前主角色
 * @param sessionId 会话标识
 * @param roles 当前生效角色
 * @param permissions 当前生效权限
 */
public record AuthenticatedUser(
        long id,
        String username,
        String role,
        String sessionId,
        List<String> roles,
        List<String> permissions) {
}
