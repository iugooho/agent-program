package com.travelagent.travelplanner.auth.application;

import java.time.Instant;

/**
 * 已校验的 Access Token 声明。
 *
 * @param userId 用户主键
 * @param username 登录账号
 * @param role 角色
 * @param sessionId 会话标识
 * @param tokenId 令牌标识
 * @param expiresAt 过期时间
 */
public record AccessTokenClaims(
        long userId,
        String username,
        String role,
        String sessionId,
        String tokenId,
        Instant expiresAt) {
}
