package com.travelagent.travelplanner.itinerary.domain;

import java.math.BigDecimal;
import java.time.LocalTime;

/** 一天中的单项安排。 */
public record ItineraryItem(
        String title,
        String type,
        LocalTime startTime,
        LocalTime endTime,
        String location,
        BigDecimal estimatedCost,
        String notes) {
}
