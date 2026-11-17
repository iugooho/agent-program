-- 权限关系已经完全迁移到 sys_user_role / sys_role_permission。
-- users.role 只保留过兼容旧数据的职责，现在移除，避免角色出现两套事实来源。

ALTER TABLE users DROP CONSTRAINT ck_users_role;
ALTER TABLE users DROP INDEX idx_users_role_status;
ALTER TABLE users DROP COLUMN role;
