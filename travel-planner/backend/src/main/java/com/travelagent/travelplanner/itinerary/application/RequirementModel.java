package com.travelagent.travelplanner.itinerary.application;

import com.travelagent.travelplanner.itinerary.domain.TripRequest;
import java.util.Optional;

/** 大模型需求解析端口；不可用时由规则解析器接替。 */
public interface RequirementModel {

    /** 尝试从自然语言中提取结构化旅行需求。 */
    Optional<TripRequest> extract(String message);
}
