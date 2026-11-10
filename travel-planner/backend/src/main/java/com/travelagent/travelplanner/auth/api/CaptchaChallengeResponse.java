package com.travelagent.travelplanner.auth.api;

import com.travelagent.travelplanner.auth.application.CaptchaChallenge;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 人机验证挑战响应。
 *
 * @param challengeId 挑战标识
 * @param imageDataUrl SVG 图片数据地址
 * @param expiresIn 有效秒数
 */
public record CaptchaChallengeResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String challengeId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String imageDataUrl,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long expiresIn) {

    public static CaptchaChallengeResponse from(CaptchaChallenge challenge) {
        return new CaptchaChallengeResponse(
                challenge.challengeId(),
                challenge.imageDataUrl(),
                challenge.expiresIn());
    }
}
