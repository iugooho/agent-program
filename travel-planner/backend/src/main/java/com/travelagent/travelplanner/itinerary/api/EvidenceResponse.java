package com.travelagent.travelplanner.itinerary.api;

import com.travelagent.travelplanner.itinerary.domain.ToolEvidence;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** 外部旅行数据来源。 */
public record EvidenceResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String source,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String description,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant fetchedAt) {

    /** 从领域模型转换。 */
    public static EvidenceResponse from(ToolEvidence evidence) {
        return new EvidenceResponse(evidence.source(), evidence.description(), evidence.fetchedAt());
    }
}
