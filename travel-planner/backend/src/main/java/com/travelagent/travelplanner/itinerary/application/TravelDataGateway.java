package com.travelagent.travelplanner.itinerary.application;

import com.travelagent.travelplanner.itinerary.domain.ToolEvidence;
import com.travelagent.travelplanner.itinerary.domain.TripRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** 旅行事实数据端口，第一版由高德和和风天气实现。 */
public interface TravelDataGateway {

    /** 收集景点、天气和数据来源。 */
    TravelContext collect(TripRequest request);

    /** 规划所需的规范化事实。 */
    record TravelContext(
            List<AttractionFact> attractions,
            List<WeatherFact> weather,
            List<ToolEvidence> evidences,
            List<String> warnings) {
    }

    /** 景点事实。 */
    record AttractionFact(
            String city,
            String name,
            String category,
            String address,
            BigDecimal estimatedCost) {
    }

    /** 天气事实。 */
    record WeatherFact(
            LocalDate date,
            String summary,
            String temperature,
            boolean outdoorFriendly) {
    }
}
