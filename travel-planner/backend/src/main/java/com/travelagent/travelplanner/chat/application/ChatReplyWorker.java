package com.travelagent.travelplanner.chat.application;

import com.travelagent.travelplanner.chat.config.ChatProperties;
import com.travelagent.travelplanner.chat.domain.ChatMessageRepository;
import com.travelagent.travelplanner.chat.domain.ChatModelPort;
import com.travelagent.travelplanner.chat.domain.ChatModelPort.ChatPrompt;
import com.travelagent.travelplanner.chat.domain.ConversationRepository;
import com.travelagent.travelplanner.chat.infrastructure.llm.RuleBasedChatModel;
import com.travelagent.travelplanner.common.error.ErrorCode;
import java.time.LocalDateTime;
import java.util.concurrent.Executor;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 助手回复生成器。
 *
 * <p>发送消息的事务提交后才开始调用模型：生成线程不占用 Web 请求线程，
 * 即使浏览器提前断开，回复仍会完整落库，重新进入会话即可看到结果。</p>
 */
@Component
public class ChatReplyWorker {

    private static final Logger log = LoggerFactory.getLogger(ChatReplyWorker.class);
    private static final String PUBLIC_FAILURE = "回复生成失败，请稍后重新发送";

    private final ChatMessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final ChatPromptAssembler promptAssembler;
    private final ChatModelRouter modelRouter;
    private final ChatStreamHub streamHub;
    private final ChatProperties properties;
    private final Executor executor;

    /**
     * 创建回复生成器。
     *
     * @param messageRepository 消息仓储
     * @param conversationRepository 会话仓储
     * @param promptAssembler 提示词组装器
     * @param modelRouter 模型路由
     * @param streamHub 流事件总线
     * @param properties 对话配置
     * @param executor 生成线程池
     */
    public ChatReplyWorker(
            ChatMessageRepository messageRepository,
            ConversationRepository conversationRepository,
            ChatPromptAssembler promptAssembler,
            ChatModelRouter modelRouter,
            ChatStreamHub streamHub,
            ChatProperties properties,
            @Qualifier("chatExecutor") Executor executor) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.promptAssembler = promptAssembler;
        this.modelRouter = modelRouter;
        this.streamHub = streamHub;
        this.properties = properties;
        this.executor = executor;
    }

    /**
     * 事务提交后把生成任务放进线程池。
     *
     * @param event 回复生成请求
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onReplyRequested(ChatReplyRequested event) {
        try {
            executor.execute(() -> generate(event));
        } catch (RuntimeException exception) {
            log.warn("对话生成任务提交失败 conversationId={} replyId={}",
                    event.conversationId(), event.replyMessageId(), exception);
            fail(event.conversationId(), event.replyMessageId(), "", PUBLIC_FAILURE, exception);
        }
    }

    private void generate(ChatReplyRequested event) {
        long conversationId = event.conversationId();
        long replyId = event.replyMessageId();
        streamHub.begin(replyId);
        if (messageRepository.markStreaming(replyId, "") == 0) {
            // 已经被其它线程接手，避免重复生成。
            return;
        }

        ChatPrompt prompt;
        try {
            prompt = promptAssembler.assemble(conversationId, replyId);
        } catch (RuntimeException exception) {
            log.warn("对话上下文组装失败 conversationId={} replyId={}", conversationId, replyId, exception);
            fail(conversationId, replyId, "", PUBLIC_FAILURE, exception);
            return;
        }

        ChatModelPort model = modelRouter.resolve();
        Buffer buffer = new Buffer();
        RuntimeException failure = null;
        try {
            streamInto(model, prompt, replyId, buffer);
        } catch (RuntimeException exception) {
            failure = exception;
        }

        if ((failure != null || buffer.isEmpty())
                && !RuleBasedChatModel.MODEL_ID.equals(model.id())
                && properties.fallbackEnabled()) {
            ChatModelPort fallback = modelRouter.find(RuleBasedChatModel.MODEL_ID).orElse(null);
            if (fallback != null) {
                log.warn("对话模型 {} 不可用，降级到规则回复 replyId={}", model.id(), replyId);
                try {
                    streamInto(fallback, prompt, replyId, buffer);
                    model = fallback;
                    failure = null;
                } catch (RuntimeException exception) {
                    failure = exception;
                }
            }
        }

        if (buffer.isEmpty()) {
            log.warn("对话生成结果为空 replyId={}", replyId, failure);
            fail(conversationId, replyId, "", PUBLIC_FAILURE, failure);
            return;
        }
        if (failure != null) {
            log.warn("对话生成中断，保留已生成内容 replyId={} chars={}",
                    replyId, buffer.length(), failure);
            fail(conversationId, replyId, buffer.content(), PUBLIC_FAILURE, failure);
            return;
        }

        messageRepository.complete(replyId, buffer.content(), model.id());
        streamHub.complete(replyId);
        conversationRepository.touch(conversationId, LocalDateTime.now());
    }

    private void streamInto(
            ChatModelPort model, ChatPrompt prompt, long replyId, Buffer buffer) {
        try (Stream<String> deltas = model.stream(prompt)) {
            deltas.forEach(content -> {
                if (content == null || content.isEmpty()) {
                    return;
                }
                buffer.append(content);
                streamHub.publish(replyId, buffer.sequence(), content);
                if (buffer.flushDue(properties.flushIntervalMs())) {
                    messageRepository.updateContent(replyId, buffer.content());
                }
            });
        }
    }

    private void fail(
            long conversationId, long replyId, String partial, String reason, Throwable cause) {
        messageRepository.fail(replyId, partial, reason);
        streamHub.fail(replyId, ErrorCode.CHAT_GENERATION_FAILED.code(), reason);
        conversationRepository.touch(conversationId, LocalDateTime.now());
        if (cause != null) {
            log.warn("对话生成失败 conversationId={} replyId={}", conversationId, replyId, cause);
        }
    }

    /** 流式生成缓冲，仅被生成线程访问。 */
    private static final class Buffer {

        private final StringBuilder content = new StringBuilder();
        private long sequence;
        private long lastFlushMillis = System.currentTimeMillis();

        private void append(String delta) {
            content.append(delta);
            sequence++;
        }

        private String content() {
            return content.toString();
        }

        private long length() {
            return content.length();
        }

        private long sequence() {
            return sequence;
        }

        private boolean isEmpty() {
            return content.isEmpty();
        }

        private boolean flushDue(long intervalMillis) {
            long now = System.currentTimeMillis();
            if (now - lastFlushMillis >= Math.max(200L, intervalMillis)) {
                lastFlushMillis = now;
                return true;
            }
            return false;
        }
    }
}
