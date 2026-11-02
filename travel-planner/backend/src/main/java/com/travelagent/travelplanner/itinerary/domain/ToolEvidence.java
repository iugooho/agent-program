package com.travelagent.travelplanner.itinerary.domain;

import java.time.Instant;

/** 外部事实的数据来源，便于前端提示信息有效期并支持问题追溯。 */
public record ToolEvidence(String source, String description, Instant fetchedAt) {
}
