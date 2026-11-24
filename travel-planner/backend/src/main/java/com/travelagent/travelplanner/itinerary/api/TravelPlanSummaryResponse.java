package com.travelagent.travelplanner.itinerary.api;

import com.travelagent.travelplanner.itinerary.domain.SavedTravelPlan;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 旅行计划列表项。
 *
 * @param id 主键
 * @param ownerId 所有者主键
 * @param departmentId 所属部门主键
 * @param title 行程标题
 * @param destinations 目的地
 * @param startDate 出发日期
 * @param days 天数
 * @param totalBudget 总预算
 * @param mode 生成模式
 * @param createTime 创建时间
 */
public record TravelPlanSummaryResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long ownerId,
        Long departmentId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<String> destinations,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate startDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int days,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal totalBudget,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String mode,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createTime) {

    /** 从领域对象转换。 */
    public static TravelPlanSummaryResponse from(SavedTravelPlan plan) {
        return new TravelPlanSummaryResponse(
                plan.id(),
                plan.ownerId(),
                plan.departmentId(),
                plan.itinerary().title(),
                plan.request().destinations(),
                plan.request().startDate(),
                plan.request().days(),
                plan.itinerary().totalBudget(),
                plan.mode(),
                plan.createTime());
    }
}
