package com.travelagent.travelplanner.chat.infrastructure.llm;

import com.travelagent.travelplanner.chat.config.ChatProperties;
import com.travelagent.travelplanner.chat.domain.ChatModelPort;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/**
 * 规则降级模型。
 *
 * <p>没有配置大模型密钥时仍然提供可用的对话闭环：从最近几轮用户消息里抽取
 * 目的地、天数、人数、预算和禁忌，缺什么问什么，信息齐了给出可执行的推进建议。
 * 它不冒充实时事实，也不编造价格和开放时间。</p>
 */
@Component
public class RuleBasedChatModel implements ChatModelPort {

    /** 稳定标识，会随消息落库。 */
    public static final String MODEL_ID = "rule";

    private static final Pattern DESTINATION = Pattern.compile(
            "(?:去|到|目的地(?:是|为)?[：:]?|想去|准备去|计划去)\\s*([\\u4e00-\\u9fa5]{2,10}?)"
                    + "(?=玩|旅游|旅行|出差|度假|自由行|，|,|。|；|;|\\s|\\d|$)");
    private static final Pattern DAYS = Pattern.compile("(\\d{1,2})\\s*(?:天|日)");
    private static final Pattern TRAVELERS = Pattern.compile("(\\d{1,2})\\s*(?:人|个人|位)");
    private static final Pattern BUDGET = Pattern.compile(
            "(?:预算|总预算|花费|大概|预计)\\s*(?:是|为|在)?\\s*[：:]?\\s*(\\d{3,7})"
                    + "|(\\d{3,7})\\s*(?:元|块|rmb|RMB)");
    private static final Pattern PREFERENCE = Pattern.compile(
            "(?:喜欢|偏好|想|希望|风格是?)\\s*([^。；;\\n]{1,20})");
    private static final Pattern FORBIDDEN = Pattern.compile(
            "(?:不要|不吃|不想|不去|避开|忌口|不能)\\s*([^。；;\\n]{1,20})");
    private static final Pattern GREETING = Pattern.compile(
            "^(你好|您好|hi|hello|在吗|哈喽)[!！。,.，\\s]*$", Pattern.CASE_INSENSITIVE);

    private final long chunkDelayMillis;

    /**
     * 创建规则模型。
     *
     * @param properties 对话配置
     */
    public RuleBasedChatModel(ChatProperties properties) {
        Duration delay = properties.ruleChunkDelay();
        this.chunkDelayMillis = delay == null ? 0L : Math.max(0L, delay.toMillis());
    }

    @Override
    public String id() {
        return MODEL_ID;
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public Stream<String> stream(ChatPrompt prompt) {
        List<String> userMessages = prompt.turns().stream()
                .filter(turn -> "USER".equalsIgnoreCase(turn.role()))
                .map(ChatTurn::content)
                .toList();
        String latest = userMessages.isEmpty() ? "" : userMessages.getLast();
        String reply = reply(userMessages, latest);
        List<String> chunks = chunk(reply);
        if (chunkDelayMillis <= 0L) {
            return chunks.stream();
        }
        return chunks.stream().map(chunk -> {
            sleep();
            return chunk;
        });
    }

    private void sleep() {
        try {
            Thread.sleep(chunkDelayMillis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("规则回复生成被中断", exception);
        }
    }

    private String reply(List<String> userMessages, String latest) {
        if (latest.isBlank()) {
            return "先告诉我你想去哪里、玩几天、几个人、预算多少，我就能帮你把行程拆开。";
        }
        String combined = String.join("。", userMessages.subList(Math.max(0, userMessages.size() - 6),
                userMessages.size()));
        if (GREETING.matcher(latest.strip()).matches()) {
            return """
                    你好，我是旅行规划助手。你可以直接说“从成都去云南 5 天，2 个人，预算 8000，休闲一点，不要购物”，
                    我会先把需求拆成结构化条件，再给出可执行的行程节奏。

                    如果已经想好了目的地，也可以只告诉我城市和天数，缺的信息我会逐项追问。""";
        }

        String destination = firstGroup(DESTINATION, combined);
        Integer days = firstInt(DAYS, combined);
        Integer travelers = firstInt(TRAVELERS, combined);
        Integer budget = firstInt(BUDGET, combined);
        String preference = firstGroup(PREFERENCE, combined);
        String forbidden = firstGroup(FORBIDDEN, combined);

        StringBuilder reply = new StringBuilder();
        List<String> missing = new ArrayList<>();
        if (destination == null) {
            missing.add("目的地");
        }
        if (days == null) {
            missing.add("天数");
        }
        if (travelers == null) {
            missing.add("人数");
        }
        if (budget == null) {
            missing.add("预算");
        }

        if (missing.size() >= 3) {
            reply.append("可以，我先把这次旅行拆成需求。还差")
                    .append(String.join("、", missing))
                    .append("，你补充一下我就能继续。\n\n");
            reply.append("例如：从成都去云南 5 天，2 个人，预算 8000，休闲一点，不要购物。");
            return reply.toString();
        }

        reply.append("收到，我把这次旅行需求整理如下：\n");
        if (destination != null) {
            reply.append("- 目的地：").append(destination).append('\n');
        }
        if (days != null) {
            reply.append("- 天数：").append(days).append(" 天\n");
        }
        if (travelers != null) {
            reply.append("- 人数：").append(travelers).append(" 人\n");
        }
        if (budget != null) {
            reply.append("- 预算：").append(budget).append(" 元\n");
        }
        if (preference != null) {
            reply.append("- 偏好：").append(preference.strip()).append('\n');
        }
        if (forbidden != null) {
            reply.append("- 需要避开：").append(forbidden.strip()).append('\n');
        }

        reply.append('\n');
        if (!missing.isEmpty()) {
            reply.append("还差").append(String.join("、", missing)).append("，补齐后我可以把节奏排得更准。\n");
        }
        reply.append("""
                可以先按下面的节奏推进：
                1. 先定往返交通和住宿区域，预算优先留给这两项；
                2. 每天安排 2 个主景点 + 1 个机动时段，避免排得过满；
                3. 餐饮和门票按当地实时价格复核，不把估算当成最终报价；
                4. 需要按天、分上午/下午/晚上输出完整行程时，进入「智能规划」生成结构化方案。

                说明：当前使用本地规则回复，信息来自你刚才的描述；配置模型密钥后会自动切换为大模型对话。""");
        return reply.toString();
    }

    private List<String> chunk(String reply) {
        List<String> chunks = new ArrayList<>();
        for (String part : reply.split("(?<=[。！？；\\n])")) {
            if (!part.isEmpty()) {
                chunks.add(part);
            }
        }
        return chunks;
    }

    private String firstGroup(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String value = matcher.group(1);
            if (value != null && !value.isBlank()) {
                return value.strip();
            }
        }
        return null;
    }

    private Integer firstInt(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            Set<String> groups = new LinkedHashSet<>();
            for (int index = 1; index <= matcher.groupCount(); index++) {
                if (matcher.group(index) != null) {
                    groups.add(matcher.group(index));
                }
            }
            for (String value : groups) {
                try {
                    return Integer.valueOf(value);
                } catch (NumberFormatException ignored) {
                    // 继续尝试下一个候选分组
                }
            }
        }
        return null;
    }
}
