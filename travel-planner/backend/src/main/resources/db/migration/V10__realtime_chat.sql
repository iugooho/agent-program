-- 实时对话：会话与消息。
--
-- 设计约定与需求文档一致：
-- 1. 消息先落库再推送，SSE 只承担传输，不承担状态；
-- 2. 会话、消息都按 user_id / conversation_id 做归属校验，避免改 ID 越权；
-- 3. assistant 消息先插入 PENDING 占位，流式生成过程中更新为 STREAMING，
--    正常结束写 COMPLETED，失败写 FAILED 并保留已生成内容与错误原因；
-- 4. msg_type 预留 STREAM，当前用户消息写 TEXT、助手回复写 STREAM。

CREATE TABLE conversations (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    user_id           BIGINT       NOT NULL,
    title             VARCHAR(200) NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    last_message_time TIMESTAMP(6) NULL,
    create_time       TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    update_time       TIMESTAMP(6) NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_conversations_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_conversations_status
        CHECK (status IN ('ACTIVE', 'ARCHIVED', 'DELETED'))
);

CREATE INDEX idx_conversations_user_status ON conversations (user_id, status, update_time);

CREATE TABLE messages (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    conversation_id BIGINT       NOT NULL,
    role            VARCHAR(20)  NOT NULL,
    content         TEXT         NOT NULL,
    msg_type        VARCHAR(20)  NOT NULL DEFAULT 'TEXT',
    status          VARCHAR(20)  NOT NULL DEFAULT 'COMPLETED',
    model           VARCHAR(64)  NULL,
    error_message   VARCHAR(1000) NULL,
    create_time     TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    update_time     TIMESTAMP(6) NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_messages_conversation
        FOREIGN KEY (conversation_id) REFERENCES conversations (id) ON DELETE CASCADE,
    CONSTRAINT ck_messages_role CHECK (role IN ('USER', 'ASSISTANT', 'SYSTEM')),
    CONSTRAINT ck_messages_msg_type CHECK (msg_type IN ('TEXT', 'STREAM')),
    CONSTRAINT ck_messages_status
        CHECK (status IN ('PENDING', 'STREAMING', 'COMPLETED', 'FAILED'))
);

CREATE INDEX idx_messages_conversation_id_id ON messages (conversation_id, id);

-- 对话权限点：普通用户可以发起对话，也可以删除自己的对话。
INSERT INTO sys_permission (code, name, module, description, sort_order) VALUES
    ('chat.use', '使用实时对话', '智能对话', '创建会话、发送消息并订阅流式回复', 95),
    ('chat.delete', '删除实时对话', '智能对话', '删除本人的对话及消息记录', 96);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
CROSS JOIN sys_permission p
WHERE r.code IN ('USER', 'ADMIN', 'SUPER_ADMIN')
  AND p.code IN ('chat.use', 'chat.delete');
