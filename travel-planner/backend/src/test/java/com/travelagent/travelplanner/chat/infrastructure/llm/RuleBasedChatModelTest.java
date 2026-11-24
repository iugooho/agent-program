package com.travelagent.travelplanner.chat.infrastructure.llm;

import static org.assertj.core.api.Assertions.assertThat;

import com.travelagent.travelplanner.chat.domain.ChatModelPort.ChatPrompt;
import com.travelagent.travelplanner.chat.domain.ChatModelPort.ChatTurn;
import com.travelagent.travelplanner.chat.support.ChatTestSupport;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 规则降级模型的抽取与追问行为。 */
class RuleBasedChatModelTest {

    private final RuleBasedChatModel model = new RuleBasedChatModel(ChatTestSupport.properties());

    @Test
    @DisplayName("从多轮消息中抽取目的地、天数、人数和预算")
    void extractsTravelRequirementsAcrossTurns() {
        ChatPrompt prompt = new ChatPrompt("system", List.of(
                new ChatTurn("USER", "我想去杭州玩"),
                new ChatTurn("ASSISTANT", "打算玩几天？"),
                new ChatTurn("USER", "4 天，2 个人，预算 8000 元，不要购物")));

        String reply = model.stream(prompt).collect(Collectors.joining());

        assertThat(reply).contains("杭州").contains("4 天").contains("2 人")
                .contains("8000 元").contains("购物");
    }

    @Test
    @DisplayName("信息不足时一次追问关键条件")
    void asksForMissingInformation() {
        ChatPrompt prompt = new ChatPrompt("system", List.of(new ChatTurn("USER", "你好")));

        String reply = model.stream(prompt).collect(Collectors.joining());

        assertThat(reply).contains("旅行规划助手");
    }
}
