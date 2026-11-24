package com.travelagent.travelplanner.chat.domain;

/** 对话状态：ACTIVE 可继续对话，DELETED 为软删除。 */
public enum ConversationStatus {

    /** 会话可用。 */
    ACTIVE,

    /** 会话已归档，只读。 */
    ARCHIVED,

    /** 会话已软删除，不再出现在列表里。 */
    DELETED;
}
