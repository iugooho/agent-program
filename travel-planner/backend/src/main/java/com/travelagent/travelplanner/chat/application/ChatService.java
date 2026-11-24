package com.travelagent.travelplanner.chat.application;

import com.travelagent.travelplanner.chat.api.ChatExchangeResponse;
import com.travelagent.travelplanner.chat.api.ChatMessagePageResponse;
import com.travelagent.travelplanner.chat.api.ChatMessageResponse;
import com.travelagent.travelplanner.chat.api.ConversationListResponse;
import com.travelagent.travelplanner.chat.api.ConversationResponse;
import com.travelagent.travelplanner.chat.config.ChatProperties;
import com.travelagent.travelplanner.chat.domain.ChatMessage;
import com.travelagent.travelplanner.chat.domain.ChatMessageRepository;
import com.travelagent.travelplanner.chat.domain.Conversation;
import com.travelagent.travelplanner.chat.domain.ConversationRepository;
import com.travelagent.travelplanner.chat.domain.ConversationStatus;
import com.travelagent.travelplanner.chat.domain.MessageRole;
import com.travelagent.travelplanner.chat.domain.MessageStatus;
import com.travelagent.travelplanner.chat.domain.MessageType;
import com.travelagent.travelplanner.common.error.BusinessException;
import com.travelagent.travelplanner.common.error.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 实时对话用例：会话、消息与生成任务的编排。 */
@Service
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatProperties properties;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 创建对话服务。
     *
     * @param conversationRepository 会话仓储
     * @param messageRepository 消息仓储
     * @param properties 对话配置
     * @param eventPublisher 事件发布器
     */
    public ChatService(
            ConversationRepository conversationRepository,
            ChatMessageRepository messageRepository,
            ChatProperties properties,
            ApplicationEventPublisher eventPublisher) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.properties = properties;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 创建会话。
     *
     * @param userId 当前用户
     * @param title 可选标题
     * @return 新会话
     */
    @Transactional
    public ConversationResponse createConversation(long userId, String title) {
        String normalized = title == null ? "" : title.strip();
        if (normalized.length() > 200) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "标题不能超过 200 个字符");
        }
        if (normalized.isEmpty()) {
            String configured = properties.defaultTitle();
            normalized = configured == null || configured.isBlank() ? "新的对话" : configured;
        }
        Conversation conversation = conversationRepository.create(userId, normalized);
        return toResponse(conversation);
    }

    /**
     * 分页查询当前用户的会话。
     *
     * @param userId 当前用户
     * @param page 页码，从 1 开始
     * @param size 每页条数
     * @return 会话分页
     */
    public ConversationListResponse listConversations(long userId, int page, int size) {
        long total = conversationRepository.countByUser(userId);
        int offset = (page - 1) * size;
        List<ConversationResponse> items = conversationRepository.findSummaries(userId, offset, size)
                .stream()
                .map(summary -> ConversationResponse.from(summary, properties.previewMaxLength()))
                .toList();
        return new ConversationListResponse(page, size, total, items);
    }

    /**
     * 查询会话详情。
     *
     * @param userId 当前用户
     * @param conversationId 会话主键
     * @return 会话
     */
    public ConversationResponse getConversation(long userId, long conversationId) {
        return toResponse(requireConversation(userId, conversationId));
    }

    /**
     * 软删除会话。
     *
     * @param userId 当前用户
     * @param conversationId 会话主键
     */
    @Transactional
    public void deleteConversation(long userId, long conversationId) {
        requireConversation(userId, conversationId);
        conversationRepository.softDelete(conversationId);
    }

    /**
     * 查询消息历史，支持用 beforeId 向前翻页。
     *
     * @param userId 当前用户
     * @param conversationId 会话主键
     * @param beforeId 只取该消息之前的数据
     * @param limit 每页条数
     * @return 消息分页
     */
    public ChatMessagePageResponse listMessages(
            long userId, long conversationId, Long beforeId, int limit) {
        requireConversation(userId, conversationId);
        int safeLimit = Math.max(1, limit);
        List<ChatMessage> fetched =
                messageRepository.findRecent(conversationId, beforeId, safeLimit + 1);
        boolean hasMore = fetched.size() > safeLimit;
        List<ChatMessage> page = hasMore
                ? fetched.subList(fetched.size() - safeLimit, fetched.size())
                : fetched;
        Long nextBeforeId = hasMore && !page.isEmpty() ? page.getFirst().id() : null;
        List<ChatMessageResponse> items = page.stream()
                .map(ChatMessageResponse::from)
                .toList();
        return new ChatMessagePageResponse(items, hasMore, nextBeforeId);
    }

    /**
     * 发送一条用户消息，并创建助手回复占位。
     *
     * <p>事务提交后由 {@link ChatReplyWorker} 异步生成回复；接口不阻塞等待模型。</p>
     *
     * @param userId 当前用户
     * @param conversationId 会话主键
     * @param content 用户消息
     * @return 用户消息与助手占位
     */
    @Transactional
    public ChatExchangeResponse sendMessage(long userId, long conversationId, String content) {
        String normalized = content == null ? "" : content.strip();
        if (normalized.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "消息不能为空");
        }
        int maxContentChars = Math.max(1, properties.maxContentChars());
        if (normalized.length() > maxContentChars) {
            throw new BusinessException(
                    ErrorCode.PARAM_INVALID,
                    "消息长度不能超过 " + maxContentChars + " 个字符");
        }
        Conversation conversation = requireConversation(userId, conversationId);
        if (conversation.status() != ConversationStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.CHAT_STATE_INVALID, "会话已归档，不能继续发送消息");
        }

        long existingMessages = messageRepository.countByConversation(conversationId);
        ChatMessage userMessage = messageRepository.create(
                conversationId,
                MessageRole.USER,
                MessageType.TEXT,
                normalized,
                MessageStatus.COMPLETED,
                null,
                null);
        ChatMessage reply = messageRepository.create(
                conversationId,
                MessageRole.ASSISTANT,
                MessageType.STREAM,
                "",
                MessageStatus.PENDING,
                null,
                null);
        LocalDateTime now = LocalDateTime.now();
        conversationRepository.touch(conversationId, now);
        if (existingMessages == 0) {
            conversationRepository.rename(conversationId, titleFrom(normalized));
        }

        eventPublisher.publishEvent(new ChatReplyRequested(conversationId, reply.id()));
        Conversation refreshed = requireConversation(userId, conversationId);
        return new ChatExchangeResponse(
                toResponse(refreshed),
                ChatMessageResponse.from(userMessage),
                ChatMessageResponse.from(reply));
    }

    /**
     * 查询当前用户有权访问的消息。
     *
     * @param userId 当前用户
     * @param conversationId 会话主键
     * @param messageId 消息主键
     * @return 消息
     */
    public ChatMessage getOwnedMessage(long userId, long conversationId, long messageId) {
        requireConversation(userId, conversationId);
        return messageRepository.findByIdAndConversation(messageId, conversationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_MESSAGE_NOT_FOUND));
    }

    private Conversation requireConversation(long userId, long conversationId) {
        return conversationRepository.findByIdAndUser(conversationId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND));
    }

    private ConversationResponse toResponse(Conversation conversation) {
        long messageCount = messageRepository.countByConversation(conversation.id());
        String lastMessage = messageRepository.findRecent(conversation.id(), null, 1).stream()
                .findFirst()
                .map(ChatMessage::content)
                .orElse("");
        return ConversationResponse.from(
                conversation,
                (int) messageCount,
                lastMessage,
                properties.previewMaxLength());
    }

    private String titleFrom(String content) {
        int maxLength = Math.max(1, properties.titleMaxLength());
        String normalized = content.replaceAll("\\s+", " ").strip();
        return normalized.length() <= maxLength
                ? normalized
                : normalized.substring(0, maxLength);
    }
}
