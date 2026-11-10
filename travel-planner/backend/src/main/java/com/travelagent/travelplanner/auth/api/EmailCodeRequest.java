package com.travelagent.travelplanner.auth.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 发送邮箱验证码请求。
 *
 * @param email 接收验证码的邮箱
 * @param purpose REGISTER、LOGIN 或 UPDATE_PROFILE
 * @param captchaChallengeId 人机验证挑战
 * @param captchaAnswer 人机验证答案
 */
public record EmailCodeRequest(
        @NotBlank @Email @Schema(format = "email") String email,
        @NotBlank @Schema(example = "LOGIN") String purpose,
        @Schema(description = "发送验证码前必须提交") String captchaChallengeId,
        @Schema(description = "发送验证码前必须提交") String captchaAnswer) {
}
