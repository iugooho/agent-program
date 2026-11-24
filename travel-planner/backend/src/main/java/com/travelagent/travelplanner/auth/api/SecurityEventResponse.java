package com.travelagent.travelplanner.auth.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.travelagent.travelplanner.auth.domain.SecurityEvent;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 账号安全事件。
 *
 * @param id 主键
 * @param eventType 事件类型
 * @param description 描述
 * @param clientIp 客户端 IP
 * @param userAgent 客户端标识
 * @param createTime 发生时间
 */
public record SecurityEventResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String eventType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String description,
        String clientIp,
        String userAgent,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime createTime) {

    /** 从领域对象转换。 */
    public static SecurityEventResponse from(SecurityEvent event) {
        return new SecurityEventResponse(
                event.id(),
                event.eventType(),
                event.description(),
                event.clientIp(),
                event.userAgent(),
                event.createTime());
    }
}
