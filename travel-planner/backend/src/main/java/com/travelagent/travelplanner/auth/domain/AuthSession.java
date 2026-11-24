package com.travelagent.travelplanner.auth.domain;

import java.time.LocalDateTime;

/**
 * 登录会话。
 *
 * @param sessionId 会话标识
 * @param userId 用户主键
 * @param clientIp 客户端 IP
 * @param userAgent 客户端标识
 * @param createTime 创建时间
 * @param lastSeenTime 最近活跃时间
 * @param expiresAt 过期时间
 * @param revokedAt 撤销时间
 */
public record AuthSession(
        String sessionId,
        long userId,
        String clientIp,
        String userAgent,
        LocalDateTime createTime,
        LocalDateTime lastSeenTime,
        LocalDateTime expiresAt,
        LocalDateTime revokedAt) {

    /** 会话是否仍然有效。 */
    public boolean active() {
        return revokedAt == null && expiresAt.isAfter(LocalDateTime.now());
    }
}
