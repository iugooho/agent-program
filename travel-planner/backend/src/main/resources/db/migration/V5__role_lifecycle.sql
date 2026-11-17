-- 角色生命周期：ACTIVE 可用、DISABLED 暂停授权、DELETED 软删除。
-- 内置的 USER / ADMIN / SUPER_ADMIN 保持 ACTIVE，且由应用层禁止修改状态。

ALTER TABLE sys_role ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE sys_role ADD CONSTRAINT ck_sys_role_status
    CHECK (status IN ('ACTIVE', 'DISABLED', 'DELETED'));
CREATE INDEX idx_sys_role_status ON sys_role (status);
