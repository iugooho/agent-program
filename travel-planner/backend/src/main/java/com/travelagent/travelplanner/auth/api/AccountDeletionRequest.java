package com.travelagent.travelplanner.auth.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 注销账号请求。
 *
 * @param verificationCode 当前邮箱验证码
 */
public record AccountDeletionRequest(
        @NotBlank @Size(max = 6) String verificationCode) {
}
