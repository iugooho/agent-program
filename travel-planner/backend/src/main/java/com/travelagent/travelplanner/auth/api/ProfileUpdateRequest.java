package com.travelagent.travelplanner.auth.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 更新当前用户资料请求。
 *
 * @param nickname 昵称
 * @param phone 联系电话
 * @param newUsername 新登录账号
 * @param newPassword 新密码
 * @param newEmail 新邮箱
 * @param verificationCode 当前邮箱验证码
 * @param newEmailCode 新邮箱验证码
 */
public record ProfileUpdateRequest(
        @Size(max = 50) String nickname,
        @Size(max = 20) String phone,
        @Size(min = 4, max = 32)
        @Pattern(regexp = "^$|^[A-Za-z0-9_]+$")
        String newUsername,
        @Size(min = 8, max = 64) String newPassword,
        @Email @Size(max = 254) String newEmail,
        @Size(max = 6) String verificationCode,
        @Size(max = 6) String newEmailCode) {

    /** 是否修改敏感账号字段。 */
    @Schema(hidden = true)
    public boolean changesSensitiveFields() {
        return hasText(newUsername) || hasText(newPassword) || hasText(newEmail);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
