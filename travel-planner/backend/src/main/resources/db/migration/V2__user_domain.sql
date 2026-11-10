-- 用户域迁移。
--
-- V1 的 users 是早期骨架结构，不能修改已执行过的 V1，因此在 V2 中演进。
-- 旧用户没有 username/email/password_hash，不能安全地自动生成登录凭据；
-- 若检测到旧数据，迁移会在 guard 处失败，先完成人工数据映射后再执行。
-- 空库可以直接按新结构建立用户域。

CREATE TABLE v2_user_migration_guard (
    user_count INT NOT NULL,
    CONSTRAINT ck_v2_user_migration_empty CHECK (user_count = 0)
);

INSERT INTO v2_user_migration_guard (user_count)
SELECT COUNT(*) FROM users;

DROP TABLE v2_user_migration_guard;

ALTER TABLE users CHANGE COLUMN created_at create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE users MODIFY COLUMN create_time TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);

ALTER TABLE users MODIFY COLUMN nickname VARCHAR(50) NULL;
ALTER TABLE users MODIFY COLUMN phone VARCHAR(20) NULL;
ALTER TABLE users DROP CONSTRAINT uk_users_phone;

ALTER TABLE users ADD COLUMN username VARCHAR(50) NOT NULL;
ALTER TABLE users ADD COLUMN email VARCHAR(254) NOT NULL;
ALTER TABLE users ADD COLUMN password_hash VARCHAR(100) NOT NULL;
ALTER TABLE users ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';
ALTER TABLE users ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE users ADD COLUMN last_login_time TIMESTAMP(6) NULL;
ALTER TABLE users ADD COLUMN update_time TIMESTAMP(6) NOT NULL
    DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6);

ALTER TABLE users ADD CONSTRAINT uk_users_username UNIQUE (username);
ALTER TABLE users ADD CONSTRAINT uk_users_email UNIQUE (email);
ALTER TABLE users ADD CONSTRAINT ck_users_role CHECK (role IN ('USER', 'ADMIN'));
ALTER TABLE users ADD CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'DISABLED', 'DELETED'));

CREATE INDEX idx_users_role_status ON users (role, status);
CREATE INDEX idx_users_last_login_time ON users (last_login_time);

CREATE TABLE user_preferences (
    id                       BIGINT       NOT NULL AUTO_INCREMENT,
    user_id                  BIGINT       NOT NULL,
    travel_style_default     VARCHAR(50)  NULL,
    food_habit               VARCHAR(200) NULL,
    forbidden_list           JSON         NOT NULL DEFAULT ('[]'),
    accommodation_preference VARCHAR(200) NULL,
    budget_level             VARCHAR(20)  NULL,
    create_time              TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    update_time              TIMESTAMP(6) NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_user_preferences_user UNIQUE (user_id),
    CONSTRAINT ck_user_preferences_budget_level
        CHECK (budget_level IS NULL OR budget_level IN ('ECONOMY', 'COMFORT', 'LUXURY')),
    CONSTRAINT fk_user_preferences_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
