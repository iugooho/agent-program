package com.travelagent.travelplanner.auth.domain;

import com.travelagent.travelplanner.common.error.BusinessException;
import com.travelagent.travelplanner.common.error.ErrorCode;

/** 邮箱验证码用途。 */
public enum EmailPurpose {

    /** 注册新账号。 */
    REGISTER,

    /** 邮箱验证码登录。 */
    LOGIN,

    /** 修改敏感资料。 */
    UPDATE_PROFILE;

    /**
     * 解析接口传入的用途。
     *
     * @param value 用途字符串
     * @return 标准枚举
     */
    public static EmailPurpose fromValue(String value) {
        try {
            return EmailPurpose.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "purpose 必须是 REGISTER、LOGIN 或 UPDATE_PROFILE");
        }
    }
}
