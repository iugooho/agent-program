package com.travelagent.travelplanner.itinerary.infrastructure.persistence;

import java.time.LocalDateTime;

/**
 * 旅行计划数据库行。
 *
 * @param id 主键
 * @param ownerId 所有者主键
 * @param departmentId 所属部门主键
 * @param requestPayload 需求 JSON
 * @param itineraryPayload 行程 JSON
 * @param mode 生成模式
 * @param createTime 创建时间
 */
public record TravelPlanRow(
        long id,
        long ownerId,
        Long departmentId,
        String requestPayload,
        String itineraryPayload,
        String mode,
        LocalDateTime createTime) {
}
