package com.travelagent.travelplanner.itinerary.domain;

import java.time.LocalDate;
import java.util.List;

/** 单日行程。 */
public record DayPlan(
        int dayNumber,
        LocalDate date,
        String city,
        List<ItineraryItem> items,
        List<String> notes) {
}
