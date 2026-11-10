package com.travelagent.travelplanner.auth.domain;

/** 邮箱验证码发送适配器。 */
public interface EmailCodeSender {

    /**
     * 发送验证码。
     *
     * @param email 收件邮箱
     * @param purpose 验证码用途
     * @param code 明文验证码，只用于发送，不得落库或写日志
     */
    void send(String email, EmailPurpose purpose, String code);
}
