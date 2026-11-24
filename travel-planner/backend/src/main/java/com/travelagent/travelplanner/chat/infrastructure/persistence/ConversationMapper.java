package com.travelagent.travelplanner.chat.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** conversations 表 Mapper。 */
@Mapper
public interface ConversationMapper {

    /**
     * 插入会话并回填主键。
     *
     * @param conversation 插入参数
     * @return 插入行数
     */
    @Insert("""
            INSERT INTO conversations (user_id, title)
            VALUES (#{userId}, #{title})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ConversationInsert conversation);

    /**
     * 按主键与用户查询未删除的会话。
     *
     * @param conversationId 会话主键
     * @param userId 用户主键
     * @return 会话行
     */
    @Select("""
            SELECT id, user_id AS userId, title, status, last_message_time AS lastMessageTime,
                   create_time AS createTime, update_time AS updateTime
            FROM conversations
            WHERE id = #{conversationId}
              AND user_id = #{userId}
              AND status <> 'DELETED'
            """)
    ConversationRow findByIdAndUser(
            @Param("conversationId") long conversationId,
            @Param("userId") long userId);

    /**
     * 分页查询用户的会话列表。
     *
     * @param userId 用户主键
     * @param limit 每页条数
     * @param offset 偏移量
     * @return 会话摘要行
     */
    @Select("""
            SELECT c.id, c.title, c.status,
                   (SELECT COUNT(*) FROM messages m WHERE m.conversation_id = c.id) AS messageCount,
                   (SELECT m2.content FROM messages m2
                    WHERE m2.conversation_id = c.id
                    ORDER BY m2.id DESC LIMIT 1) AS lastMessagePreview,
                   c.last_message_time AS lastMessageTime,
                   c.create_time AS createTime, c.update_time AS updateTime
            FROM conversations c
            WHERE c.user_id = #{userId}
              AND c.status <> 'DELETED'
            ORDER BY COALESCE(c.last_message_time, c.create_time) DESC, c.id DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<ConversationSummaryRow> findSummaries(
            @Param("userId") long userId,
            @Param("limit") int limit,
            @Param("offset") int offset);

    /**
     * 统计用户未删除的会话数。
     *
     * @param userId 用户主键
     * @return 会话数
     */
    @Select("""
            SELECT COUNT(*)
            FROM conversations
            WHERE user_id = #{userId}
              AND status <> 'DELETED'
            """)
    long countByUser(@Param("userId") long userId);

    /**
     * 更新最近消息时间。
     *
     * @param conversationId 会话主键
     * @param lastMessageTime 最近消息时间
     * @return 更新行数
     */
    @Update("""
            UPDATE conversations
            SET last_message_time = #{lastMessageTime}
            WHERE id = #{conversationId}
            """)
    int touch(
            @Param("conversationId") long conversationId,
            @Param("lastMessageTime") LocalDateTime lastMessageTime);

    /**
     * 更新会话标题。
     *
     * @param conversationId 会话主键
     * @param title 新标题
     * @return 更新行数
     */
    @Update("""
            UPDATE conversations
            SET title = #{title}
            WHERE id = #{conversationId}
            """)
    int rename(
            @Param("conversationId") long conversationId,
            @Param("title") String title);

    /**
     * 软删除会话。
     *
     * @param conversationId 会话主键
     * @return 更新行数
     */
    @Update("UPDATE conversations SET status = 'DELETED' WHERE id = #{conversationId}")
    int softDelete(@Param("conversationId") long conversationId);
}
