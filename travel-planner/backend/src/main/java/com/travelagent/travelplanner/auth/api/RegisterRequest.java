package com.travelagent.travelplanner.auth.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 用户注册请求。
 *
 * @param username 4-32 位字母、数字或下划线
 * @param password 8-64 位明文密码
 * @param email 绑定邮箱
 * @param emailCode REGISTER 用途邮箱验证码
 */
public record RegisterRequest(
        @NotBlank
        @Size(min = 4, max = 32)
        @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "username 只能包含字母、数字或下划线")
        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                description = "4-32 位字母、数字或下划线",
                example = "traveler_01")
        String username,
        @NotBlank
        @Size(min = 8, max = 64)
        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                description = "8-64 位密码，服务端使用 BCrypt 保存",
                format = "password")
        String password,
        @NotBlank
        @Email
        @Size(max = 254)
        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                description = "绑定邮箱，全局唯一",
                format = "email")
        String email,
        @NotBlank
        @Pattern(regexp = "^\\d{6}$", message = "emailCode 必须是 6 位数字")
        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                description = "purpose=REGISTER 的 6 位邮箱验证码",
                example = "123456")
        String emailCode) {
}
