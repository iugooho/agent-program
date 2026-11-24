package com.travelagent.travelplanner.itinerary.domain;

import java.time.LocalDateTime;

/**
 * 已保存的旅行计划。
 *
 * @param id 主键
 * @param ownerId 所有者用户主键
 * @param departmentId 所属部门主键
 * @param request 旅行需求
 * @param itinerary 行程方案
 * @param mode 生成模式
 * @param createTime 创建时间
 */
public record SavedTravelPlan(
        long id,
        long ownerId,
        Long departmentId,
        TripRequest request,
        Itinerary itinerary,
        String mode,
        LocalDateTime createTime) {
}
