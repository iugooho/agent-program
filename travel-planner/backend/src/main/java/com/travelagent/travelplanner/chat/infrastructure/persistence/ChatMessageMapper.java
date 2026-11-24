package com.travelagent.travelplanner.chat.infrastructure.persistence;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** messages 表 Mapper。 */
@Mapper
public interface ChatMessageMapper {

    /**
     * 插入消息并回填主键。
     *
     * @param message 插入参数
     * @return 插入行数
     */
    @Insert("""
            INSERT INTO messages (conversation_id, role, content, msg_type, status, model, error_message)
            VALUES (#{conversationId}, #{role}, #{content}, #{msgType}, #{status}, #{model}, #{errorMessage})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ChatMessageInsert message);

    /**
     * 按消息和会话查询。
     *
     * @param messageId 消息主键
     * @param conversationId 会话主键
     * @return 消息行
     */
    @Select("""
            SELECT id, conversation_id AS conversationId, role, content, msg_type AS msgType,
                   status, model, error_message AS errorMessage,
                   create_time AS createTime, update_time AS updateTime
            FROM messages
            WHERE id = #{messageId}
              AND conversation_id = #{conversationId}
            """)
    ChatMessageRow findByIdAndConversation(
            @Param("messageId") long messageId,
            @Param("conversationId") long conversationId);

    /**
     * 按会话分页查询消息，倒序取一页，由仓储层反转成升序。
     *
     * @param conversationId 会话主键
     * @param beforeId 只取该 id 之前的消息，可为空
     * @param limit 最大条数
     * @return 消息行
     */
    @Select("""
            <script>
            SELECT id, conversation_id AS conversationId, role, content, msg_type AS msgType,
                   status, model, error_message AS errorMessage,
                   create_time AS createTime, update_time AS updateTime
            FROM messages
            WHERE conversation_id = #{conversationId}
            <if test="beforeId != null">
              AND id &lt; #{beforeId}
            </if>
            ORDER BY id DESC
            LIMIT #{limit}
            </script>
            """)
    List<ChatMessageRow> findRecent(
            @Param("conversationId") long conversationId,
            @Param("beforeId") Long beforeId,
            @Param("limit") int limit);

    /**
     * 查询组装提示词所需的历史消息，倒序取最近若干条。
     *
     * @param conversationId 会话主键
     * @param beforeMessageId 当前消息主键
     * @param limit 最大条数
     * @return 消息行
     */
    @Select("""
            SELECT id, conversation_id AS conversationId, role, content, msg_type AS msgType,
                   status, model, error_message AS errorMessage,
                   create_time AS createTime, update_time AS updateTime
            FROM messages
            WHERE conversation_id = #{conversationId}
              AND id < #{beforeMessageId}
              AND status = 'COMPLETED'
              AND role IN ('USER', 'ASSISTANT')
            ORDER BY id DESC
            LIMIT #{limit}
            """)
    List<ChatMessageRow> findContext(
            @Param("conversationId") long conversationId,
            @Param("beforeMessageId") long beforeMessageId,
            @Param("limit") int limit);

    /**
     * 统计会话消息数。
     *
     * @param conversationId 会话主键
     * @return 消息数
     */
    @Select("SELECT COUNT(*) FROM messages WHERE conversation_id = #{conversationId}")
    long countByConversation(@Param("conversationId") long conversationId);

    /**
     * 原子地把 PENDING 消息切换为 STREAMING。
     *
     * @param messageId 消息主键
     * @param content 初始内容
     * @return 更新行数
     */
    @Update("""
            UPDATE messages
            SET status = 'STREAMING', content = #{content}
            WHERE id = #{messageId}
              AND status = 'PENDING'
            """)
    int markStreaming(
            @Param("messageId") long messageId,
            @Param("content") String content);

    /**
     * 保存流式生成中的部分内容。
     *
     * @param messageId 消息主键
     * @param content 当前完整内容
     * @return 更新行数
     */
    @Update("""
            UPDATE messages
            SET content = #{content}
            WHERE id = #{messageId}
              AND status = 'STREAMING'
            """)
    int updateContent(
            @Param("messageId") long messageId,
            @Param("content") String content);

    /**
     * 标记生成完成。
     *
     * @param messageId 消息主键
     * @param content 完整内容
     * @param model 生成模型
     * @return 更新行数
     */
    @Update("""
            UPDATE messages
            SET content = #{content}, status = 'COMPLETED', model = #{model}, error_message = NULL
            WHERE id = #{messageId}
              AND status IN ('PENDING', 'STREAMING')
            """)
    int complete(
            @Param("messageId") long messageId,
            @Param("content") String content,
            @Param("model") String model);

    /**
     * 标记生成失败，保留已生成内容。
     *
     * @param messageId 消息主键
     * @param content 已生成内容
     * @param errorMessage 失败原因
     * @return 更新行数
     */
    @Update("""
            UPDATE messages
            SET content = #{content}, status = 'FAILED', error_message = #{errorMessage}
            WHERE id = #{messageId}
              AND status IN ('PENDING', 'STREAMING')
            """)
    int fail(
            @Param("messageId") long messageId,
            @Param("content") String content,
            @Param("errorMessage") String errorMessage);
}
