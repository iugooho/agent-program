package com.travelagent.travelplanner.chat.application;

import com.travelagent.travelplanner.chat.config.ChatProperties;
import com.travelagent.travelplanner.chat.domain.ChatModelPort;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 模型路由：按配置的候选链选择第一个可用实现。
 *
 * <p>默认链是 {@code spring-ai,rule}：配置了模型密钥就走真实模型，
 * 没有密钥或模型初始化失败时自动落到规则实现，业务层不需要感知。</p>
 */
@Component
public class ChatModelRouter {

    private static final Logger log = LoggerFactory.getLogger(ChatModelRouter.class);
    private static final String RULE_MODEL = "rule";

    private final Map<String, ChatModelPort> models;
    private final List<String> providers;

    /**
     * 创建模型路由。
     *
     * @param candidates 容器里的全部模型实现
     * @param properties 对话配置
     */
    public ChatModelRouter(List<ChatModelPort> candidates, ChatProperties properties) {
        Map<String, ChatModelPort> indexed = new LinkedHashMap<>();
        candidates.forEach(model -> indexed.put(model.id(), model));
        this.models = Map.copyOf(indexed);
        List<String> configured = properties.providers();
        this.providers = configured == null || configured.isEmpty()
                ? List.of(RULE_MODEL)
                : List.copyOf(configured);
    }

    /**
     * 解析当前应使用的模型。
     *
     * @return 可用的模型实现
     */
    public ChatModelPort resolve() {
        for (String provider : providers) {
            ChatModelPort model = models.get(provider);
            if (model != null && model.available()) {
                log.debug("对话模型路由到 {}", provider);
                return model;
            }
        }
        ChatModelPort fallback = models.get(RULE_MODEL);
        if (fallback == null) {
            throw new IllegalStateException("没有可用的对话模型实现");
        }
        return fallback;
    }

    /**
     * 按标识查找模型。
     *
     * @param id 模型标识
     * @return 模型实现
     */
    public Optional<ChatModelPort> find(String id) {
        return Optional.ofNullable(models.get(id));
    }
}
