package com.travelagent.travelplanner.chat.domain;

import java.time.LocalDateTime;

/**
 * 一条对话消息。
 *
 * @param id 消息主键
 * @param conversationId 所属会话
 * @param role 消息角色
 * @param content 消息内容，流式生成中为已生成的部分内容
 * @param type 消息类型
 * @param status 投递状态
 * @param model 生成该回复的模型标识，用户消息为 null
 * @param errorMessage 生成失败原因，成功时为 null
 * @param createTime 创建时间
 * @param updateTime 更新时间
 */
public record ChatMessage(
        long id,
        long conversationId,
        MessageRole role,
        String content,
        MessageType type,
        MessageStatus status,
        String model,
        String errorMessage,
        LocalDateTime createTime,
        LocalDateTime updateTime) {
}

