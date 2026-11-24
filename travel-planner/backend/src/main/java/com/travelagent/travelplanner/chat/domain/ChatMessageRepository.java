package com.travelagent.travelplanner.chat.domain;

import java.util.List;
import java.util.Optional;

/** 消息仓储。 */
public interface ChatMessageRepository {

    /**
     * 写入一条消息。
     *
     * @param conversationId 所属会话
     * @param role 消息角色
     * @param type 消息类型
     * @param content 消息内容
     * @param status 投递状态
     * @param model 生成模型
     * @param errorMessage 失败原因
     * @return 落库后的消息
     */
    ChatMessage create(
            long conversationId,
            MessageRole role,
            MessageType type,
            String content,
            MessageStatus status,
            String model,
            String errorMessage);

    /**
     * 按消息主键与会话主键查询。
     *
     * @param messageId 消息主键
     * @param conversationId 会话主键
     * @return 消息，不存在或不属于该会话时为空
     */
    Optional<ChatMessage> findByIdAndConversation(long messageId, long conversationId);

    /**
     * 按会话分页查询消息，返回按 id 升序排列的结果。
     *
     * @param conversationId 会话主键
     * @param beforeId 只取该消息 id 之前的数据，为空时取最新一页
     * @param limit 最大条数
     * @return 消息列表
     */
    List<ChatMessage> findRecent(long conversationId, Long beforeId, int limit);

    /**
     * 查询组装提示词所需的历史消息，按 id 升序返回。
     *
     * @param conversationId 会话主键
     * @param beforeMessageId 当前消息 id，只取它之前的内容
     * @param limit 最大条数
     * @return 已完成的历史消息
     */
    List<ChatMessage> findContext(long conversationId, long beforeMessageId, int limit);

    /**
     * 统计会话消息数。
     *
     * @param conversationId 会话主键
     * @return 消息数
     */
    long countByConversation(long conversationId);

    /**
     * 原子地把 PENDING 消息切换为 STREAMING。
     *
     * @param messageId 消息主键
     * @param content 初始内容
     * @return 更新行数，0 表示消息已被其它线程接手或状态不允许
     */
    int markStreaming(long messageId, String content);

    /**
     * 保存流式生成中的部分内容。
     *
     * @param messageId 消息主键
     * @param content 当前完整内容
     */
    void updateContent(long messageId, String content);

    /**
     * 标记生成完成。
     *
     * @param messageId 消息主键
     * @param content 完整内容
     * @param model 生成模型
     */
    void complete(long messageId, String content, String model);

    /**
     * 标记生成失败，保留已生成内容。
     *
     * @param messageId 消息主键
     * @param content 已生成内容
     * @param errorMessage 失败原因
     */
    void fail(long messageId, String content, String errorMessage);
}
