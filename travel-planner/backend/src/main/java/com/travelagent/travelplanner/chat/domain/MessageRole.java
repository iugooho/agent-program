package com.travelagent.travelplanner.chat.domain;

/** 消息角色。 */
public enum MessageRole {

    /** 用户发送。 */
    USER,

    /** 助手回复。 */
    ASSISTANT,

    /** 系统提示，仅内部使用。 */
    SYSTEM;
}
