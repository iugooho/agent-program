package com.travelagent.travelplanner.auth.infrastructure.security;

/**
 * 已认证用户。
 *
 * @param id 用户主键
 * @param username 登录账号
 * @param role 角色
 * @param sessionId 会话标识
 */
public record AuthenticatedUser(long id, String username, String role, String sessionId) {
}
