package com.travelagent.travelplanner.chat.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 会话分页响应。
 *
 * @param page 当前页码
 * @param size 每页条数
 * @param total 总条数
 * @param items 会话列表
 */
public record ConversationListResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int page,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int size,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long total,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<ConversationResponse> items) {
}
