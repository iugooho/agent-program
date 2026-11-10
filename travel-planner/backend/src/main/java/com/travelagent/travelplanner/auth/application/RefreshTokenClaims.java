package com.travelagent.travelplanner.auth.application;

import java.time.Instant;

/**
 * 已校验的 Refresh Token 声明。
 *
 * @param userId 用户主键
 * @param sessionId 会话标识
 * @param tokenId 令牌标识
 * @param expiresAt 过期时间
 */
public record RefreshTokenClaims(long userId, String sessionId, String tokenId, Instant expiresAt) {
}
