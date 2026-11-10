package com.travelagent.travelplanner.auth.infrastructure.mail;

import com.travelagent.travelplanner.auth.domain.EmailCodeSender;
import com.travelagent.travelplanner.auth.domain.EmailPurpose;
import com.travelagent.travelplanner.common.error.BusinessException;
import com.travelagent.travelplanner.common.error.ErrorCode;

/** 未配置生产邮件网关时的显式失败实现。 */
public class UnavailableEmailCodeSender implements EmailCodeSender {

    @Override
    public void send(String email, EmailPurpose purpose, String code) {
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "邮件服务未配置");
    }
}
