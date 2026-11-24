package com.travelagent.travelplanner.chat.infrastructure.llm;

import com.travelagent.travelplanner.chat.domain.ChatModelPort;
import java.util.List;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Spring AI 对话模型适配器。
 *
 * <p>地址、模型名和密钥继续由 spring.ai.deepseek.* 提供，
 * 更换供应商时只需要替换 Spring AI starter 或注册另一个 {@link ChatModelPort}。</p>
 */
@Component
public class SpringAiChatModel implements ChatModelPort {

    /** 稳定标识，会随消息落库。 */
    public static final String MODEL_ID = "spring-ai";

    private static final Logger log = LoggerFactory.getLogger(SpringAiChatModel.class);

    private final ChatClient chatClient;

    /**
     * 创建适配器。没有配置密钥时保持不可用，由路由自动降级。
     *
     * @param builderProvider Spring AI ChatClient 构造器
     * @param apiKey 模型密钥
     */
    public SpringAiChatModel(
            ObjectProvider<ChatClient.Builder> builderProvider,
            @Value("${spring.ai.deepseek.api-key:}") String apiKey) {
        this.chatClient = createChatClient(builderProvider, apiKey);
    }

    @Override
    public String id() {
        return MODEL_ID;
    }

    @Override
    public boolean available() {
        return chatClient != null;
    }

    @Override
    public Stream<String> stream(ChatPrompt prompt) {
        if (chatClient == null) {
            throw new IllegalStateException("Spring AI 对话模型未启用");
        }
        List<Message> messages = prompt.turns().stream()
                .map(this::toMessage)
                .toList();
        if (messages.isEmpty()) {
            throw new IllegalArgumentException("对话上下文为空，无法调用模型");
        }
        return chatClient.prompt()
                .system(prompt.systemPrompt())
                .messages(messages)
                .stream()
                .content()
                .toStream();
    }

    private Message toMessage(ChatTurn turn) {
        if ("ASSISTANT".equalsIgnoreCase(turn.role())) {
            return new AssistantMessage(turn.content());
        }
        return new UserMessage(turn.content());
    }

    private ChatClient createChatClient(
            ObjectProvider<ChatClient.Builder> builderProvider, String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            log.info("未配置 DeepSeek 密钥，实时对话使用规则降级实现");
            return null;
        }
        try {
            ChatClient.Builder builder = builderProvider.getIfAvailable();
            if (builder == null) {
                log.warn("已配置 DeepSeek 密钥，但 Spring AI 模型未启用；请设置 SPRING_AI_MODEL_CHAT=deepseek");
                return null;
            }
            return builder.build();
        } catch (RuntimeException exception) {
            log.warn("Spring AI ChatClient 初始化失败，实时对话降级到规则实现: {}",
                    exception.getClass().getSimpleName());
            return null;
        }
    }
}
