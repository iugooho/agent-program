package com.travelagent.travelplanner.chat.api;

import jakarta.validation.constraints.NotBlank;

/**
 * 发送消息请求。最大长度由 {@code app.chat.max-content-chars} 控制，避免把长度限制写死在接口上。
 *
 * @param content 用户消息
 */
public record SendMessageRequest(
        @NotBlank(message = "消息不能为空") String content) {
}
