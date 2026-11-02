package com.travelagent.travelplanner.itinerary.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** 用户旅行需求：由模型或规则解析器生成，供规划和校验共同使用。 */
public record TripRequest(
        String departureCity,
        List<String> destinations,
        LocalDate startDate,
        int days,
        int travelers,
        BigDecimal budget,
        List<String> travelStyles,
        String transportPreference,
        List<String> foodPreferences,
        List<String> forbiddenItems,
        List<String> accommodationPreferences) {
}
