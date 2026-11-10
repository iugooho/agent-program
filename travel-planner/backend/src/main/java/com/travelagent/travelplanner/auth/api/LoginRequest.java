package com.travelagent.travelplanner.auth.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求。
 *
 * @param loginType PASSWORD 或 EMAIL_CODE
 * @param username 密码登录账号
 * @param password 密码登录密码
 * @param email 验证码登录邮箱
 * @param emailCode LOGIN 用途验证码
 * @param captchaChallengeId 登录失败后触发的人机验证挑战
 * @param captchaAnswer 人机验证答案
 */
public record LoginRequest(
        @NotBlank @Schema(description = "PASSWORD 或 EMAIL_CODE", example = "PASSWORD") String loginType,
        @Schema(description = "PASSWORD 模式下必填") String username,
        @Schema(description = "PASSWORD 模式下必填", format = "password") String password,
        @Email @Schema(description = "EMAIL_CODE 模式下必填", format = "email") String email,
        @Schema(description = "EMAIL_CODE 模式下必填") String emailCode,
        @Schema(description = "触发人机验证后必填") String captchaChallengeId,
        @Schema(description = "触发人机验证后必填") String captchaAnswer) {
}
