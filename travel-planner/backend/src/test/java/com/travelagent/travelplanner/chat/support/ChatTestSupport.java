package com.travelagent.travelplanner.chat.support;

import com.travelagent.travelplanner.chat.config.ChatProperties;
import java.time.Duration;
import java.util.List;

/** 对话模块测试用的固定配置。 */
public final class ChatTestSupport {

    private ChatTestSupport() {
    }

    /**
     * 构造一份可预测的配置。
     *
     * @return 对话配置
     */
    public static ChatProperties properties() {
        return new ChatProperties(
                List.of("rule"),
                "新的旅行对话",
                "今天是 {{today}}，请用简体中文回答。",
                20,
                12_000,
                30,
                60,
                4_000,
                Duration.ofMinutes(5),
                Duration.ofSeconds(15),
                Duration.ofMinutes(2),
                800,
                20_000,
                true,
                Duration.ZERO,
                new ChatProperties.Executor(2, 4, 20));
    }
}
