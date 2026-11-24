package com.travelagent.travelplanner.chat.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 消息游标分页响应。消息按时间升序返回。
 *
 * @param items 当前页消息
 * @param hasMore 是否还有更早的消息
 * @param nextBeforeId 下一页请求应传入的 beforeId
 */
public record ChatMessagePageResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<ChatMessageResponse> items,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean hasMore,
        Long nextBeforeId) {
}
