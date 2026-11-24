package com.travelagent.travelplanner.chat.infrastructure.persistence;

import java.time.LocalDateTime;

/**
 * messages 表查询行。
 *
 * @param id 消息主键
 * @param conversationId 所属会话
 * @param role 角色
 * @param content 内容
 * @param msgType 消息类型
 * @param status 投递状态
 * @param model 生成模型
 * @param errorMessage 失败原因
 * @param createTime 创建时间
 * @param updateTime 更新时间
 */
public record ChatMessageRow(
        long id,
        long conversationId,
        String role,
        String content,
        String msgType,
        String status,
        String model,
        String errorMessage,
        LocalDateTime createTime,
        LocalDateTime updateTime) {
}
