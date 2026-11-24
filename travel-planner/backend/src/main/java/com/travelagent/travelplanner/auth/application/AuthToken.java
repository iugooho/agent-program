package com.travelagent.travelplanner.auth.application;

import java.time.Instant;

/**
 * 登录令牌结果。
 *
 * @param accessToken 访问令牌
 * @param refreshToken 刷新令牌
 * @param tokenType 固定为 Bearer
 * @param expiresIn access token 有效秒数
 * @param userId 用户主键
 * @param sessionId 会话标识
 * @param refreshExpiresAt Refresh Token 过期时间
 */
public record AuthToken(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        long userId,
        String sessionId,
        Instant refreshExpiresAt) {
}
