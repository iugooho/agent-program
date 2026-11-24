package com.travelagent.travelplanner.chat.infrastructure.persistence;

import com.travelagent.travelplanner.chat.domain.ChatMessage;
import com.travelagent.travelplanner.chat.domain.ChatMessageRepository;
import com.travelagent.travelplanner.chat.domain.MessageRole;
import com.travelagent.travelplanner.chat.domain.MessageStatus;
import com.travelagent.travelplanner.chat.domain.MessageType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** 基于 MyBatis 的消息仓储。 */
@Repository
public class MyBatisChatMessageRepository implements ChatMessageRepository {

    private final ChatMessageMapper mapper;

    /**
     * 创建消息仓储。
     *
     * @param mapper MyBatis Mapper
     */
    public MyBatisChatMessageRepository(ChatMessageMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public ChatMessage create(
            long conversationId,
            MessageRole role,
            MessageType type,
            String content,
            MessageStatus status,
            String model,
            String errorMessage) {
        ChatMessageInsert insert = new ChatMessageInsert(
                conversationId,
                role.name(),
                content,
                type.name(),
                status.name(),
                model,
                errorMessage);
        mapper.insert(insert);
        return findByIdAndConversation(insert.getId(), conversationId)
                .orElseThrow(() -> new IllegalStateException("消息创建后未能查询到"));
    }

    @Override
    public Optional<ChatMessage> findByIdAndConversation(long messageId, long conversationId) {
        return Optional.ofNullable(mapper.findByIdAndConversation(messageId, conversationId))
                .map(this::toDomain);
    }

    @Override
    public List<ChatMessage> findRecent(long conversationId, Long beforeId, int limit) {
        List<ChatMessageRow> rows = mapper.findRecent(conversationId, beforeId, limit);
        List<ChatMessage> messages = new ArrayList<>(rows.size());
        rows.forEach(row -> messages.add(toDomain(row)));
        Collections.reverse(messages);
        return List.copyOf(messages);
    }

    @Override
    public List<ChatMessage> findContext(long conversationId, long beforeMessageId, int limit) {
        List<ChatMessageRow> rows = mapper.findContext(conversationId, beforeMessageId, limit);
        List<ChatMessage> messages = new ArrayList<>(rows.size());
        rows.forEach(row -> messages.add(toDomain(row)));
        Collections.reverse(messages);
        return List.copyOf(messages);
    }

    @Override
    public long countByConversation(long conversationId) {
        return mapper.countByConversation(conversationId);
    }

    @Override
    public int markStreaming(long messageId, String content) {
        return mapper.markStreaming(messageId, content);
    }

    @Override
    public void updateContent(long messageId, String content) {
        mapper.updateContent(messageId, content);
    }

    @Override
    public void complete(long messageId, String content, String model) {
        mapper.complete(messageId, content, model);
    }

    @Override
    public void fail(long messageId, String content, String errorMessage) {
        mapper.fail(messageId, content, errorMessage);
    }

    private ChatMessage toDomain(ChatMessageRow row) {
        return new ChatMessage(
                row.id(),
                row.conversationId(),
                MessageRole.valueOf(row.role()),
                row.content(),
                MessageType.valueOf(row.msgType()),
                MessageStatus.valueOf(row.status()),
                row.model(),
                row.errorMessage(),
                row.createTime(),
                row.updateTime());
    }
}
