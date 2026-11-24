package com.travelagent.travelplanner.chat.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelagent.travelplanner.auth.infrastructure.security.AuthenticatedUser;
import com.travelagent.travelplanner.chat.application.ChatService;
import com.travelagent.travelplanner.chat.application.ChatStreamHub;
import com.travelagent.travelplanner.chat.config.ChatProperties;
import com.travelagent.travelplanner.chat.domain.ChatMessage;
import com.travelagent.travelplanner.chat.domain.MessageRole;
import com.travelagent.travelplanner.chat.domain.MessageStatus;
import com.travelagent.travelplanner.chat.domain.MessageType;
import com.travelagent.travelplanner.common.error.BusinessException;
import com.travelagent.travelplanner.common.error.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 助手回复的 SSE 流接口。
 *
 * <p>接口只负责传输：用户消息和助手占位已经先落库，这里按 Last-Event-ID 回放
 * 或实时推送增量，完成后发送 done 事件。断线重连时带上最后收到的 id 即可续传。</p>
 */
@RestController
@RequestMapping("/api/v1/conversations")
@PreAuthorize("hasAuthority('chat.use')")
@Tag(name = "实时对话", description = "旅行规划多轮对话、消息历史与流式回复")
public class ChatStreamController {

    private static final Logger log = LoggerFactory.getLogger(ChatStreamController.class);
    private static final long DEFAULT_TIMEOUT_MILLIS = 300_000L;
    private static final long DEFAULT_HEARTBEAT_MILLIS = 15_000L;

    private final ChatService chatService;
    private final ChatStreamHub streamHub;
    private final ChatProperties properties;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService heartbeatScheduler;

    /**
     * 创建流式接口。
     *
     * @param chatService 对话服务
     * @param streamHub 流事件总线
     * @param properties 对话配置
     * @param objectMapper JSON 工具
     * @param heartbeatScheduler 心跳调度器
     */
    public ChatStreamController(
            ChatService chatService,
            ChatStreamHub streamHub,
            ChatProperties properties,
            ObjectMapper objectMapper,
            @Qualifier("chatHeartbeatScheduler") ScheduledExecutorService heartbeatScheduler) {
        this.chatService = chatService;
        this.streamHub = streamHub;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.heartbeatScheduler = heartbeatScheduler;
    }

    /**
     * 订阅助手回复增量。
     *
     * <p>EventSource 不能自定义 Authorization 头，因此前端使用 fetch + ReadableStream
     * 读取同一条 SSE 流，既保留 Bearer 认证，也能在断线后用 Last-Event-ID 续传。</p>
     *
     * @param conversationId 会话主键
     * @param messageId 助手消息主键
     * @param lastEventId 上次收到的最后一个事件 id
     * @param after 显式指定从哪个序号之后开始，优先级高于请求头
     * @param authentication 当前认证信息
     * @return SSE 通道
     */
    @GetMapping("/{conversationId}/messages/{messageId}/stream")
    @Operation(
            summary = "订阅助手回复流",
            description = "text/event-stream，事件名：ready / delta / done / error",
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "SSE 事件流",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "text/event-stream")))
    public SseEmitter stream(
            @PathVariable long conversationId,
            @PathVariable long messageId,
            @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId,
            @RequestParam(value = "after", required = false) Long after,
            Authentication authentication) {
        long userId = currentUser(authentication).id();
        ChatMessage message = chatService.getOwnedMessage(userId, conversationId, messageId);
        if (message.role() != MessageRole.ASSISTANT || message.type() != MessageType.STREAM) {
            throw new BusinessException(ErrorCode.CHAT_STATE_INVALID, "该消息不是流式回复");
        }

        long timeout = properties.streamTimeout() == null
                ? DEFAULT_TIMEOUT_MILLIS
                : properties.streamTimeout().toMillis();
        StreamSession session = new StreamSession(userId, conversationId, messageId, timeout);
        if (message.status() == MessageStatus.COMPLETED) {
            return defer(session, () -> {
                session.sendCompleted(message);
                session.finish();
            });
        }
        if (message.status() == MessageStatus.FAILED) {
            return defer(session, () -> {
                session.sendError(ErrorCode.CHAT_GENERATION_FAILED.code(), failureMessage(message));
                session.finish();
            });
        }
        if (message.status() == MessageStatus.STREAMING && !streamHub.isActive(messageId)) {
            return defer(session, () -> {
                session.sendError(ErrorCode.CHAT_STATE_INVALID.code(), "回复生成状态已失效，请重新发送");
                session.finish();
            });
        }

        long afterSequence = after != null ? Math.max(0, after) : parseSequence(lastEventId);
        return defer(session, () -> session.start(afterSequence));
    }

    /**
     * 交给调度线程发送首帧/终止帧，避免在控制器返回前完成 emitter 导致响应长度计算异常。
     */
    private SseEmitter defer(StreamSession session, Runnable action) {
        heartbeatScheduler.execute(() -> {
            try {
                action.run();
            } catch (RuntimeException exception) {
                log.warn("SSE 初始化失败 messageId={}", session.messageId, exception);
                session.finish();
            }
        });
        return session.emitter();
    }

    private String failureMessage(ChatMessage message) {
        return message.errorMessage() == null || message.errorMessage().isBlank()
                ? ErrorCode.CHAT_GENERATION_FAILED.message()
                : message.errorMessage();
    }

    private long parseSequence(String value) {
        if (value == null || value.isBlank()) {
            return 0L;
        }
        try {
            return Math.max(0L, Long.parseLong(value.strip()));
        } catch (NumberFormatException exception) {
            return 0L;
        }
    }

    private AuthenticatedUser currentUser(Authentication authentication) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
        return user;
    }

    /** 一次 SSE 订阅的完整生命周期。 */
    private final class StreamSession implements ChatStreamHub.Listener {

        private final long userId;
        private final long conversationId;
        private final long messageId;
        private final SseEmitter emitter;
        private final AtomicReference<ChatStreamHub.Subscription> subscription =
                new AtomicReference<>();
        private final AtomicBoolean finished = new AtomicBoolean();
        private volatile ScheduledFuture<?> heartbeat;

        private StreamSession(long userId, long conversationId, long messageId, long timeout) {
            this.userId = userId;
            this.conversationId = conversationId;
            this.messageId = messageId;
            this.emitter = new SseEmitter(timeout);
            this.emitter.onTimeout(this::finish);
            this.emitter.onError(ignored -> finish());
            this.emitter.onCompletion(this::finish);
        }

        private SseEmitter emitter() {
            return emitter;
        }

        private void start(long afterSequence) {
            sendEvent("ready", null, Map.of("messageId", messageId));
            ChatStreamHub.Subscription handle = streamHub.subscribe(messageId, afterSequence, this);
            subscription.set(handle);
            if (finished.get()) {
                handle.close();
                return;
            }
            Duration interval = properties.heartbeatInterval();
            long heartbeatMillis = interval == null
                    ? DEFAULT_HEARTBEAT_MILLIS
                    : Math.max(1_000L, interval.toMillis());
            heartbeat = heartbeatScheduler.scheduleAtFixedRate(
                    this::sendHeartbeat,
                    heartbeatMillis,
                    heartbeatMillis,
                    TimeUnit.MILLISECONDS);
        }

        @Override
        public void onChunk(long sequence, String content) {
            sendEvent(
                    "delta",
                    sequence,
                    new ChatDeltaEvent(messageId, sequence, content));
        }

        @Override
        public void onComplete() {
            ChatMessage message;
            try {
                message = chatService.getOwnedMessage(userId, conversationId, messageId);
            } catch (RuntimeException exception) {
                log.warn("读取流式回复最终状态失败 messageId={}", messageId, exception);
                sendError(ErrorCode.CHAT_GENERATION_FAILED.code(),
                        ErrorCode.CHAT_GENERATION_FAILED.message());
                finish();
                return;
            }
            sendCompleted(message);
            finish();
        }

        @Override
        public void onError(int code, String message) {
            sendError(code, message);
            finish();
        }

        private void sendCompleted(ChatMessage message) {
            sendEvent("done", null, ChatMessageResponse.from(message));
        }

        private void sendError(int code, String message) {
            sendEvent("error", null, new ChatStreamErrorEvent(code, message));
        }

        private void sendHeartbeat() {
            if (finished.get()) {
                return;
            }
            try {
                emitter.send(SseEmitter.event().comment("keepalive"));
            } catch (IOException | IllegalStateException exception) {
                finish();
            }
        }

        private void sendEvent(String name, Long id, Object data) {
            if (finished.get()) {
                return;
            }
            try {
                SseEmitter.SseEventBuilder builder = SseEmitter.event().name(name);
                if (id != null) {
                    builder.id(Long.toString(id));
                }
                builder.data(objectMapper.writeValueAsString(data));
                emitter.send(builder);
            } catch (IOException | IllegalStateException exception) {
                log.debug("SSE 发送失败，关闭订阅 messageId={}", messageId, exception);
                finish();
            }
        }

        private void finish() {
            if (!finished.compareAndSet(false, true)) {
                return;
            }
            ScheduledFuture<?> scheduled = heartbeat;
            if (scheduled != null) {
                scheduled.cancel(false);
            }
            ChatStreamHub.Subscription handle = subscription.get();
            if (handle != null) {
                handle.close();
            }
            try {
                emitter.complete();
            } catch (IllegalStateException ignored) {
                // 已经完成或超时，无需重复处理。
            }
        }
    }
}
