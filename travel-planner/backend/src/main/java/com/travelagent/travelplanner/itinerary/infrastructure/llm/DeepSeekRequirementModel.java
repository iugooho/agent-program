package com.travelagent.travelplanner.itinerary.infrastructure.llm;

import com.travelagent.travelplanner.itinerary.application.RequirementModel;
import com.travelagent.travelplanner.itinerary.domain.TripRequest;
import java.time.LocalDate;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 通过 Spring AI ChatClient 调用 DeepSeek，把自然语言转换为结构化旅行需求。 */
@Component
public class DeepSeekRequirementModel implements RequirementModel {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekRequirementModel.class);

    private final ChatClient chatClient;
    private final String apiKey;

    /** 从 Spring AI 配置创建 ChatClient，密钥只从环境变量注入。 */
    public DeepSeekRequirementModel(
            ObjectProvider<ChatClient.Builder> chatClientBuilder,
            @Value("${spring.ai.deepseek.api-key:}") String apiKey) {
        this.apiKey = apiKey;
        this.chatClient = createChatClient(chatClientBuilder, apiKey);
    }

    @Override
    public Optional<TripRequest> extract(String message) {
        if (apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }
        if (chatClient == null) {
            return Optional.empty();
        }
        try {
            TripRequirementDraft draft = chatClient.prompt()
                    .user(message)
                    .call()
                    .entity(TripRequirementDraft.class);
            return Optional.ofNullable(draft)
                    .flatMap(value -> value.toTripRequest(LocalDate.now()));
        } catch (RuntimeException exception) {
            log.warn("Spring AI 需求解析不可用，降级到规则解析: {}",
                    exception.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    private String systemPrompt() {
        return """
                你是旅游需求解析器。今天是 %s。
                从用户消息中提取目的地、出发城市、出发日期、天数、人数、人民币预算、旅行风格、
                交通偏好、饮食偏好、禁忌项和住宿偏好。
                无法确定目的地时 destinations 返回空数组。
                不要虚构用户明确要求的日期、天数、人数或预算；未提到的可选字段留空或使用合理默认值。
                """.formatted(LocalDate.now());
    }

    private ChatClient createChatClient(
            ObjectProvider<ChatClient.Builder> chatClientBuilder, String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        try {
            ChatClient.Builder builder = chatClientBuilder.getIfAvailable();
            if (builder == null) {
                log.warn("已配置 DeepSeek 密钥，但 Spring AI 模型未启用；请设置 SPRING_AI_MODEL_CHAT=deepseek");
                return null;
            }
            return builder.defaultSystem(systemPrompt()).build();
        } catch (RuntimeException exception) {
            log.warn("Spring AI ChatClient 初始化失败，降级到规则解析: {}",
                    exception.getClass().getSimpleName());
            return null;
        }
    }
}
