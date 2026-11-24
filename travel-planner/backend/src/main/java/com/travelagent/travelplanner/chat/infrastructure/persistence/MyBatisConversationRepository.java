package com.travelagent.travelplanner.chat.infrastructure.persistence;

import com.travelagent.travelplanner.chat.domain.Conversation;
import com.travelagent.travelplanner.chat.domain.ConversationRepository;
import com.travelagent.travelplanner.chat.domain.ConversationStatus;
import com.travelagent.travelplanner.chat.domain.ConversationSummary;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** 基于 MyBatis 的会话仓储。 */
@Repository
public class MyBatisConversationRepository implements ConversationRepository {

    private final ConversationMapper mapper;

    /**
     * 创建会话仓储。
     *
     * @param mapper MyBatis Mapper
     */
    public MyBatisConversationRepository(ConversationMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Conversation create(long userId, String title) {
        ConversationInsert insert = new ConversationInsert(userId, title);
        mapper.insert(insert);
        return findByIdAndUser(insert.getId(), userId)
                .orElseThrow(() -> new IllegalStateException("会话创建后未能查询到"));
    }

    @Override
    public Optional<Conversation> findByIdAndUser(long conversationId, long userId) {
        return Optional.ofNullable(mapper.findByIdAndUser(conversationId, userId))
                .map(this::toDomain);
    }

    @Override
    public List<ConversationSummary> findSummaries(long userId, int offset, int limit) {
        return mapper.findSummaries(userId, limit, offset).stream()
                .map(this::toSummary)
                .toList();
    }

    @Override
    public long countByUser(long userId) {
        return mapper.countByUser(userId);
    }

    @Override
    public void touch(long conversationId, LocalDateTime lastMessageTime) {
        mapper.touch(conversationId, lastMessageTime);
    }

    @Override
    public void rename(long conversationId, String title) {
        mapper.rename(conversationId, title);
    }

    @Override
    public void softDelete(long conversationId) {
        mapper.softDelete(conversationId);
    }

    private Conversation toDomain(ConversationRow row) {
        return new Conversation(
                row.id(),
                row.userId(),
                row.title(),
                ConversationStatus.valueOf(row.status()),
                row.lastMessageTime(),
                row.createTime(),
                row.updateTime());
    }

    private ConversationSummary toSummary(ConversationSummaryRow row) {
        return new ConversationSummary(
                row.id(),
                row.title(),
                ConversationStatus.valueOf(row.status()),
                row.messageCount(),
                row.lastMessagePreview() == null ? "" : row.lastMessagePreview(),
                row.lastMessageTime(),
                row.createTime(),
                row.updateTime());
    }
}
