package com.travelagent.travelplanner.auth.domain;

/** 账号安全事件类型。 */
public enum SecurityEventType {

    /** 登录成功。 */
    LOGIN,

    /** 主动退出。 */
    LOGOUT,

    /** 撤销其他会话。 */
    SESSION_REVOKED,

    /** 安全资料变更。 */
    SECURITY_PROFILE_UPDATED,

    /** 账号注销。 */
    ACCOUNT_DELETED
}
