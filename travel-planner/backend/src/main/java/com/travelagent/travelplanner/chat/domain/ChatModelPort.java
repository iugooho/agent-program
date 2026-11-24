package com.travelagent.travelplanner.chat.domain;

import java.util.List;

/**
 * 对话模型端口。
 *
 * <p>application 层只依赖这个接口，具体实现放到 infrastructure：
 * 真实模型、规则降级、后续接入的其它厂商都只是新增一个实现，不需要改业务流程。</p>
 */
public interface ChatModelPort {

    /**
     * 稳定的模型标识，会随消息一起落库，便于排查问题。
     *
     * @return 模型标识
     */
    String id();

    /**
     * 当前实现是否可用。不可用的实现会被路由跳过。
     *
     * @return 可用时返回 true
     */
    boolean available();

    /**
     * 以流的方式生成回复。
     *
     * @param prompt 系统提示与多轮上下文
     * @return 增量文本流，调用方负责关闭
     */
    java.util.stream.Stream<String> stream(ChatPrompt prompt);

    /**
     * 一轮对话内容。
     *
     * @param systemPrompt 系统提示
     * @param turns 按时间正序排列的对话轮次
     */
    record ChatPrompt(String systemPrompt, List<ChatTurn> turns) {
    }

    /**
     * 单轮对话。
     *
     * @param role 角色：USER 或 ASSISTANT
     * @param content 内容
     */
    record ChatTurn(String role, String content) {
    }
}
