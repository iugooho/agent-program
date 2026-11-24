package com.travelagent.travelplanner.auth.domain;

import java.time.LocalDateTime;

/**
 * 账号安全事件。
 *
 * @param id 主键
 * @param userId 用户主键
 * @param eventType 事件类型
 * @param description 描述
 * @param clientIp 客户端 IP
 * @param userAgent 客户端标识
 * @param createTime 创建时间
 */
public record SecurityEvent(
        long id,
        long userId,
        String eventType,
        String description,
        String clientIp,
        String userAgent,
        LocalDateTime createTime) {
}
