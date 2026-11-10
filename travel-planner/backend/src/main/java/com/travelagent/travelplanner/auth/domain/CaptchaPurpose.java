package com.travelagent.travelplanner.auth.domain;

import com.travelagent.travelplanner.common.error.BusinessException;
import com.travelagent.travelplanner.common.error.ErrorCode;

/** 人机验证票据用途。 */
public enum CaptchaPurpose {

    /** 登录失败后的验证。 */
    LOGIN,

    /** 发送邮箱验证码。 */
    EMAIL_CODE;

    /**
     * 解析客户端传入的用途。
     *
     * @param value 用途字符串
     * @return 标准枚举
     */
    public static CaptchaPurpose fromValue(String value) {
        try {
            return CaptchaPurpose.valueOf(value.strip().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "purpose 必须是 LOGIN 或 EMAIL_CODE");
        }
    }
}
