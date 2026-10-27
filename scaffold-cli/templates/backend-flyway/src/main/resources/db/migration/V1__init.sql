-- 数据库迁移脚本：表结构变更一律新增 V{版本}__{说明}.sql，
-- 已经执行过的脚本不要修改（Flyway 会校验 checksum）。
-- 生产库是 MySQL 8，本地是 H2 的 MySQL 兼容模式（MODE=MySQL），
-- 所以脚本只用两边都支持的写法：自增用 AUTO_INCREMENT，时间默认值用 CURRENT_TIMESTAMP。
CREATE TABLE IF NOT EXISTS users (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    phone      VARCHAR(32) NOT NULL,
    nickname   VARCHAR(64) NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_phone UNIQUE (phone)
);
