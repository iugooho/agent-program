-- 区分内置权限和后台新增的自定义权限。内置权限用于保护接口依赖的基础权限点。
ALTER TABLE sys_permission
    ADD COLUMN built_in BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE sys_permission
SET built_in = TRUE;
