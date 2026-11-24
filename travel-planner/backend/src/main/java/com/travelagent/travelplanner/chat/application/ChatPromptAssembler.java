package com.travelagent.travelplanner.chat.application;

import com.travelagent.travelplanner.chat.config.ChatProperties;
import com.travelagent.travelplanner.chat.domain.ChatMessage;
import com.travelagent.travelplanner.chat.domain.ChatMessageRepository;
import com.travelagent.travelplanner.chat.domain.ChatModelPort.ChatPrompt;
import com.travelagent.travelplanner.chat.domain.ChatModelPort.ChatTurn;
import com.travelagent.travelplanner.chat.domain.MessageRole;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 提示词组装：系统提示 + 最近若干轮对话。
 *
 * <p>上下文按“最近优先”装入字符预算，避免历史消息无限增长把模型上下文撑爆；
 * 至少保留当前用户刚发出的这一条，保证追问能接上。</p>
 */
@Component
public class ChatPromptAssembler {

    private static final String DEFAULT_SYSTEM_PROMPT = """
            你是旅游规划智能体，负责把用户的口语化旅行需求整理成可执行方案。
            今天是 {{today}}。
            回答要求：
            1. 用简体中文，先确认已知条件，再给建议；
            2. 缺少目的地、天数、人数、预算等关键条件时，一次追问不超过两个问题；
            3. 不编造实时价格、航班、酒店库存和开放时间，涉及事实时说明“以实时查询为准”；
            4. 行程建议按天拆开，控制每天强度，给交通和休息留余量；
            5. 用户提出修改时，先复述要改什么，再给受影响的部分，不要重写整份行程。
            """;

    private final ChatMessageRepository messageRepository;
    private final ChatProperties properties;

    /**
     * 创建提示词组装器。
     *
     * @param messageRepository 消息仓储
     * @param properties 对话配置
     */
    public ChatPromptAssembler(ChatMessageRepository messageRepository, ChatProperties properties) {
        this.messageRepository = messageRepository;
        this.properties = properties;
    }

    /**
     * 为指定助手消息组装提示词。
     *
     * @param conversationId 会话主键
     * @param replyMessageId 助手消息主键
     * @return 提示词
     */
    public ChatPrompt assemble(long conversationId, long replyMessageId) {
        int messageLimit = Math.max(1, properties.contextMessages());
        List<ChatMessage> history =
                messageRepository.findContext(conversationId, replyMessageId, messageLimit);
        Deque<ChatTurn> turns = new ArrayDeque<>();
        int remainingBudget = Math.max(500, properties.contextCharBudget());
        for (int index = history.size() - 1; index >= 0; index--) {
            ChatMessage message = history.get(index);
            if (message.role() == MessageRole.SYSTEM) {
                continue;
            }
            String content = message.content() == null ? "" : message.content().strip();
            if (content.isEmpty()) {
                continue;
            }
            if (content.length() > remainingBudget) {
                if (!turns.isEmpty()) {
                    break;
                }
                content = content.substring(0, remainingBudget);
            }
            turns.addFirst(new ChatTurn(message.role().name(), content));
            remainingBudget -= content.length();
            if (remainingBudget <= 0) {
                break;
            }
        }
        return new ChatPrompt(systemPrompt(), List.copyOf(turns));
    }

    private String systemPrompt() {
        String configured = properties.systemPrompt();
        String prompt = configured == null || configured.isBlank()
                ? DEFAULT_SYSTEM_PROMPT
                : configured;
        return prompt.replace("{{today}}", LocalDate.now().toString());
    }
}
