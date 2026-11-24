package com.travelagent.travelplanner.itinerary.api;

import com.travelagent.travelplanner.itinerary.domain.SavedTravelPlan;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 已保存旅行计划。
 *
 * @param id 主键
 * @param ownerId 所有者主键
 * @param departmentId 所属部门主键
 * @param request 旅行需求
 * @param itinerary 行程
 * @param mode 生成模式
 * @param createTime 创建时间
 */
public record SavedTravelPlanResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long ownerId,
        Long departmentId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) TripRequestResponse request,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ItineraryResponse itinerary,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String mode,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createTime) {

    /** 从领域对象转换。 */
    public static SavedTravelPlanResponse from(SavedTravelPlan plan) {
        return new SavedTravelPlanResponse(
                plan.id(),
                plan.ownerId(),
                plan.departmentId(),
                TripRequestResponse.from(plan.request()),
                ItineraryResponse.from(plan.itinerary()),
                plan.mode(),
                plan.createTime());
    }
}
