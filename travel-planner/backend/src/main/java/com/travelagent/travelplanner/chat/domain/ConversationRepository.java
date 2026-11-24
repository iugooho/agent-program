package com.travelagent.travelplanner.chat.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** 会话仓储。 */
public interface ConversationRepository {

    /**
     * 创建会话并返回落库后的记录。
     *
     * @param userId 所属用户
     * @param title 会话标题
     * @return 新会话
     */
    Conversation create(long userId, String title);

    /**
     * 按会话主键和所属用户查询，避免只按 ID 查询造成越权。
     *
     * @param conversationId 会话主键
     * @param userId 当前用户
     * @return 会话，不存在或不属于该用户时为空
     */
    Optional<Conversation> findByIdAndUser(long conversationId, long userId);

    /**
     * 分页查询用户的会话列表。
     *
     * @param userId 当前用户
     * @param offset 偏移量
     * @param limit 每页条数
     * @return 会话摘要，按最近消息时间倒序
     */
    List<ConversationSummary> findSummaries(long userId, int offset, int limit);

    /**
     * 统计用户的会话数。
     *
     * @param userId 当前用户
     * @return 会话数
     */
    long countByUser(long userId);

    /**
     * 更新最近消息时间。
     *
     * @param conversationId 会话主键
     * @param lastMessageTime 最近消息时间
     */
    void touch(long conversationId, LocalDateTime lastMessageTime);

    /**
     * 更新会话标题。
     *
     * @param conversationId 会话主键
     * @param title 新标题
     */
    void rename(long conversationId, String title);

    /**
     * 软删除会话。
     *
     * @param conversationId 会话主键
     */
    void softDelete(long conversationId);
}

