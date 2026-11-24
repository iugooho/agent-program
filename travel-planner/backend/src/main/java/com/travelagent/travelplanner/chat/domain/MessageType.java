package com.travelagent.travelplanner.chat.domain;

/** 消息类型：普通文本或通过 SSE 流式传输的回复。 */
public enum MessageType {

    /** 普通文本消息。 */
    TEXT,

    /** 由流式通道生成的助手回复。 */
    STREAM;
}
