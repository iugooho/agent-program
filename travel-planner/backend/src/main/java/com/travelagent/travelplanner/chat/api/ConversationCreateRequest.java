package com.travelagent.travelplanner.chat.api;

import jakarta.validation.constraints.Size;

/**
 * 创建会话请求。标题为空时由服务端生成默认标题。
 *
 * @param title 可选的会话标题
 */
public record ConversationCreateRequest(
        @Size(max = 200, message = "标题不能超过 200 个字符") String title) {
}
