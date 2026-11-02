package com.travelagent.travelplanner.itinerary.infrastructure.llm;

import com.travelagent.travelplanner.itinerary.domain.TripRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Spring AI 结构化解出的需求草稿，完成显式校验后再转换为领域对象。 */
record TripRequirementDraft(
        String departureCity,
        List<String> destinations,
        String startDate,
        int days,
        int travelers,
        BigDecimal budget,
        List<String> travelStyles,
        String transportPreference,
        List<String> foodPreferences,
        List<String> forbiddenItems,
        List<String> accommodationPreferences) {

    /** 将模型草稿转换为已验证的领域需求；无效内容返回空以触发规则降级。 */
    Optional<TripRequest> toTripRequest(LocalDate today) {
        List<String> normalizedDestinations = textList(destinations);
        if (normalizedDestinations.isEmpty() || days < 1 || days > 14 || travelers < 1 || travelers > 20
                || budget == null || budget.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }
        LocalDate parsedStartDate = parseDate(startDate);
        return Optional.of(new TripRequest(
                textOrDefault(departureCity, "北京"),
                normalizedDestinations,
                parsedStartDate == null ? today.plusDays(7) : parsedStartDate,
                days,
                travelers,
                budget,
                nonEmptyOrDefault(textList(travelStyles), List.of("经典观光")),
                textOrDefault(transportPreference, "高铁或飞机"),
                textList(foodPreferences),
                textList(forbiddenItems),
                nonEmptyOrDefault(textList(accommodationPreferences), List.of("交通便利"))));
    }

    private List<String> textList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String value : values) {
            String text = value == null ? "" : value.trim();
            if (!text.isEmpty() && !result.contains(text)) {
                result.add(text);
            }
        }
        return List.copyOf(result);
    }

    private List<String> nonEmptyOrDefault(List<String> values, List<String> defaults) {
        return values.isEmpty() ? defaults : values;
    }

    private String textOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private LocalDate parseDate(String value) {
        try {
            return value == null || value.isBlank() ? null : LocalDate.parse(value.trim());
        } catch (DateTimeParseException exception) {
            return null;
        }
    }
}
