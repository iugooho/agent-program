package com.travelagent.travelplanner.auth.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 人机验证挑战用途。
 *
 * @param purpose LOGIN 或 EMAIL_CODE
 */
public record CaptchaChallengeRequest(
        @NotBlank @Schema(example = "EMAIL_CODE") String purpose) {
}
