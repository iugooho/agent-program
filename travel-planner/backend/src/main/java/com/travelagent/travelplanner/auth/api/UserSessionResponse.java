package com.travelagent.travelplanner.auth.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.travelagent.travelplanner.auth.domain.AuthSession;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 当前用户登录会话。
 *
 * @param sessionId 会话标识
 * @param clientIp 客户端 IP
 * @param userAgent 客户端标识
 * @param createTime 登录时间
 * @param lastSeenTime 最近活跃时间
 * @param expiresAt 过期时间
 * @param current 是否为当前会话
 */
public record UserSessionResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String sessionId,
        String clientIp,
        String userAgent,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime createTime,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime lastSeenTime,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime expiresAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean current) {

    /** 从领域对象转换。 */
    public static UserSessionResponse from(AuthSession session, String currentSessionId) {
        return new UserSessionResponse(
                session.sessionId(),
                session.clientIp(),
                session.userAgent(),
                session.createTime(),
                session.lastSeenTime(),
                session.expiresAt(),
                session.sessionId().equals(currentSessionId));
    }
}
