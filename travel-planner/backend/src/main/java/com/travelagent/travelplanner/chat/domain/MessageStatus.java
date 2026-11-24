package com.travelagent.travelplanner.chat.domain;

/**
 * 消息投递状态。
 *
 * <p>助手消息先以 PENDING 占位，生成开始后转为 STREAMING，结束写 COMPLETED；
 * 失败写 FAILED，已经生成的部分内容仍然保留，前端可以继续展示。</p>
 */
public enum MessageStatus {

    /** 已落库，尚未开始生成。 */
    PENDING,

    /** 正在流式生成。 */
    STREAMING,

    /** 生成完成。 */
    COMPLETED,

    /** 生成失败。 */
    FAILED;
}
