package com.travelagent.travelplanner.chat.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.travelagent.travelplanner.chat.api.ChatExchangeResponse;
import com.travelagent.travelplanner.chat.api.ChatMessagePageResponse;
import com.travelagent.travelplanner.chat.domain.ChatMessage;
import com.travelagent.travelplanner.chat.domain.ChatMessageRepository;
import com.travelagent.travelplanner.chat.domain.Conversation;
import com.travelagent.travelplanner.chat.domain.ConversationRepository;
import com.travelagent.travelplanner.chat.domain.ConversationStatus;
import com.travelagent.travelplanner.chat.domain.MessageRole;
import com.travelagent.travelplanner.chat.domain.MessageStatus;
import com.travelagent.travelplanner.chat.domain.MessageType;
import com.travelagent.travelplanner.chat.support.ChatTestSupport;
import com.travelagent.travelplanner.common.error.BusinessException;
import com.travelagent.travelplanner.common.error.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

/** 对话用例的事务边界、归属校验和分页语义。 */
class ChatServiceTest {

    private ConversationRepository conversationRepository;
    private ChatMessageRepository messageRepository;
    private ApplicationEventPublisher eventPublisher;
    private ChatService chatService;

    @BeforeEach
    void setUp() {
        conversationRepository = org.mockito.Mockito.mock(ConversationRepository.class);
        messageRepository = org.mockito.Mockito.mock(ChatMessageRepository.class);
        eventPublisher = org.mockito.Mockito.mock(ApplicationEventPublisher.class);
        chatService = new ChatService(
                conversationRepository, messageRepository, ChatTestSupport.properties(), eventPublisher);
    }

    @Test
    @DisplayName("发送消息先落用户消息和助手占位，事务提交后请求生成")
    void sendsMessageAndRequestsReply() {
        Conversation conversation = conversation("新的旅行对话");
        when(conversationRepository.findByIdAndUser(5L, 1L)).thenReturn(Optional.of(conversation));
        when(messageRepository.countByConversation(5L)).thenReturn(0L);
        ChatMessage userMessage = message(11L, MessageRole.USER, "去杭州玩 4 天", MessageStatus.COMPLETED);
        ChatMessage reply = message(12L, MessageRole.ASSISTANT, "", MessageStatus.PENDING);
        when(messageRepository.create(eq(5L), eq(MessageRole.USER), eq(MessageType.TEXT),
                eq("去杭州玩 4 天"), eq(MessageStatus.COMPLETED), isNull(), isNull()))
                .thenReturn(userMessage);
        when(messageRepository.create(eq(5L), eq(MessageRole.ASSISTANT), eq(MessageType.STREAM),
                eq(""), eq(MessageStatus.PENDING), isNull(), isNull()))
                .thenReturn(reply);
        when(messageRepository.findRecent(eq(5L), isNull(), eq(1))).thenReturn(List.of(userMessage));

        ChatExchangeResponse exchange = chatService.sendMessage(1L, 5L, "  去杭州玩 4 天  ");

        assertThat(exchange.userMessage().content()).isEqualTo("去杭州玩 4 天");
        assertThat(exchange.reply().status()).isEqualTo("PENDING");
        verify(conversationRepository).rename(5L, "去杭州玩 4 天");
        ArgumentCaptor<ChatReplyRequested> event = ArgumentCaptor.forClass(ChatReplyRequested.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().conversationId()).isEqualTo(5L);
        assertThat(event.getValue().replyMessageId()).isEqualTo(12L);
    }

    @Test
    @DisplayName("空消息被拒绝，且不写库")
    void rejectsBlankMessage() {
        assertThatThrownBy(() -> chatService.sendMessage(1L, 5L, "   "))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PARAM_INVALID);
    }

    @Test
    @DisplayName("访问不属于自己的会话返回 404 语义")
    void rejectsForeignConversation() {
        when(conversationRepository.findByIdAndUser(5L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.getConversation(2L, 5L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CONVERSATION_NOT_FOUND);
    }

    @Test
    @DisplayName("消息分页返回最新一页并给出上一页游标")
    void paginatesMessagesWithCursor() {
        when(conversationRepository.findByIdAndUser(7L, 1L)).thenReturn(Optional.of(conversation("行程")));
        when(messageRepository.findRecent(7L, null, 3)).thenReturn(List.of(
                message(1L, MessageRole.USER, "一", MessageStatus.COMPLETED),
                message(2L, MessageRole.ASSISTANT, "二", MessageStatus.COMPLETED),
                message(3L, MessageRole.USER, "三", MessageStatus.COMPLETED)));

        ChatMessagePageResponse page = chatService.listMessages(1L, 7L, null, 2);

        assertThat(page.hasMore()).isTrue();
        assertThat(page.nextBeforeId()).isEqualTo(2L);
        assertThat(page.items()).extracting("id").containsExactly(2L, 3L);
    }

    private Conversation conversation(String title) {
        LocalDateTime now = LocalDateTime.now();
        return new Conversation(5L, 1L, title, ConversationStatus.ACTIVE, null, now, now);
    }

    private ChatMessage message(
            long id, MessageRole role, String content, MessageStatus status) {
        LocalDateTime now = LocalDateTime.now();
        return new ChatMessage(
                id,
                5L,
                role,
                content,
                role == MessageRole.ASSISTANT ? MessageType.STREAM : MessageType.TEXT,
                status,
                null,
                null,
                now,
                now);
    }
}
