package com.travelagent.travelplanner.itinerary.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** 创建旅行规划任务的请求。 */
public record TripPlanRequest(
        @NotBlank(message = "旅行需求不能为空")
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
                example = "从北京去杭州玩 3 天，2 人，预算 6000，想轻松一点，不要购物")
        String message) {
}
