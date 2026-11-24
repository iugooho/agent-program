package com.travelagent.travelplanner.chat.infrastructure.persistence;

/** 会话插入参数，id 由数据库回填。 */
public class ConversationInsert {

    private Long id;
    private final long userId;
    private final String title;

    /**
     * 创建插入参数。
     *
     * @param userId 所属用户
     * @param title 标题
     */
    public ConversationInsert(long userId, String title) {
        this.userId = userId;
        this.title = title;
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
     * 获取所属用户。
     *
     * @return 用户主键
     */
    public long getUserId() {
        return userId;
    }

    /**
     * 获取标题。
     *
     * @return 标题
     */
    public String getTitle() {
        return title;
    }
}
