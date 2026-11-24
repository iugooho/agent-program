package com.travelagent.travelplanner.chat.infrastructure.persistence;

/** 消息插入参数，id 由数据库回填。 */
public class ChatMessageInsert {

    private Long id;
    private final long conversationId;
    private final String role;
    private final String content;
    private final String msgType;
    private final String status;
    private final String model;
    private final String errorMessage;

    /**
     * 创建插入参数。
     *
     * @param conversationId 所属会话
     * @param role 角色
     * @param content 内容
     * @param msgType 消息类型
     * @param status 投递状态
     * @param model 生成模型
     * @param errorMessage 失败原因
     */
    public ChatMessageInsert(
            long conversationId,
            String role,
            String content,
            String msgType,
            String status,
            String model,
            String errorMessage) {
        this.conversationId = conversationId;
        this.role = role;
        this.content = content;
        this.msgType = msgType;
        this.status = status;
        this.model = model;
        this.errorMessage = errorMessage;
    }

    /**
     * 获取回填主键。
     *
     * @return 主键
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置回填主键。
     *
     * @param id 主键
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取所属会话。
     *
     * @return 会话主键
     */
    public long getConversationId() {
        return conversationId;
    }

    /**
     * 获取角色。
     *
     * @return 角色
     */
    public String getRole() {
        return role;
    }

    /**
     * 获取内容。
     *
     * @return 内容
     */
    public String getContent() {
        return content;
    }

    /**
     * 获取消息类型。
     *
     * @return 消息类型
     */
    public String getMsgType() {
        return msgType;
    }

    /**
     * 获取投递状态。
     *
     * @return 状态
     */
    public String getStatus() {
        return status;
    }

    /**
     * 获取生成模型。
     *
     * @return 模型标识
     */
    public String getModel() {
        return model;
    }

    /**
     * 获取失败原因。
     *
     * @return 失败原因
     */
    public String getErrorMessage() {
        return errorMessage;
    }
}
