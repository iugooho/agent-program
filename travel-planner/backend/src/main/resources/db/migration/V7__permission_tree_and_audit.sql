-- 权限点树、类型和软删除状态。
ALTER TABLE sys_permission
    ADD COLUMN parent_id BIGINT NULL;

ALTER TABLE sys_permission
    ADD COLUMN type VARCHAR(20) NOT NULL DEFAULT 'API';

ALTER TABLE sys_permission
    ADD COLUMN route_path VARCHAR(200) NULL;

ALTER TABLE sys_permission
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

INSERT INTO sys_permission (
    code, name, module, description, sort_order, built_in, type, status
) VALUES
    ('catalog.user', '用户中心', '用户中心', '个人资料与偏好权限目录', 1, TRUE, 'CATALOG', 'ACTIVE'),
    ('catalog.trip', '行程规划', '行程规划', '行程相关操作权限目录', 2, TRUE, 'CATALOG', 'ACTIVE'),
    ('catalog.system', '系统管理', '系统管理', '后台入口与系统管理权限目录', 3, TRUE, 'CATALOG', 'ACTIVE'),
    ('catalog.user-admin', '用户管理', '用户管理', '后台用户管理权限目录', 4, TRUE, 'CATALOG', 'ACTIVE'),
    ('catalog.audit', '审计日志', '审计日志', '审计与操作记录权限目录', 5, TRUE, 'CATALOG', 'ACTIVE'),
    ('catalog.rbac', '权限管理', '权限管理', '角色与权限配置目录', 6, TRUE, 'CATALOG', 'ACTIVE');

UPDATE sys_permission
SET parent_id = (SELECT id FROM sys_permission WHERE code = 'catalog.user')
WHERE code IN ('user.profile.read', 'user.profile.update');

UPDATE sys_permission
SET parent_id = (SELECT id FROM sys_permission WHERE code = 'catalog.trip')
WHERE code IN ('trip.create', 'trip.read', 'trip.delete');

UPDATE sys_permission
SET parent_id = (SELECT id FROM sys_permission WHERE code = 'catalog.system'),
    type = 'MENU',
    route_path = '/admin/access'
WHERE code = 'admin.console.access';

UPDATE sys_permission
SET parent_id = (SELECT id FROM sys_permission WHERE code = 'catalog.user-admin'),
    route_path = '/admin/users'
WHERE code = 'admin.user.read';

UPDATE sys_permission
SET parent_id = (SELECT id FROM sys_permission WHERE code = 'catalog.audit'),
    route_path = '/admin/audit'
WHERE code = 'admin.audit.read';

UPDATE sys_permission
SET parent_id = (SELECT id FROM sys_permission WHERE code = 'catalog.rbac'),
    type = 'PAGE',
    route_path = '/admin/rbac'
WHERE code = 'admin.rbac.manage';

UPDATE sys_permission
SET parent_id = (SELECT id FROM sys_permission WHERE code = 'catalog.user'),
    route_path = '/profile'
WHERE code IN ('user.profile.read', 'user.profile.update');

UPDATE sys_permission
SET parent_id = (SELECT id FROM sys_permission WHERE code = 'catalog.trip'),
    route_path = '/trips'
WHERE code IN ('trip.create', 'trip.read', 'trip.delete');

ALTER TABLE sys_permission
    ADD CONSTRAINT fk_sys_permission_parent
        FOREIGN KEY (parent_id) REFERENCES sys_permission (id) ON DELETE SET NULL;

ALTER TABLE sys_permission
    ADD CONSTRAINT ck_sys_permission_type
        CHECK (type IN ('CATALOG', 'MENU', 'PAGE', 'BUTTON', 'API'));

ALTER TABLE sys_permission
    ADD CONSTRAINT ck_sys_permission_status
        CHECK (status IN ('ACTIVE', 'DISABLED', 'DELETED'));

CREATE INDEX idx_sys_permission_parent ON sys_permission (parent_id);
CREATE INDEX idx_sys_permission_status ON sys_permission (status);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.type = 'CATALOG'
WHERE r.code = 'SUPER_ADMIN'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
  );

-- 权限授予和回收的审计记录。
CREATE TABLE sys_auth_audit_log (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    operator_id   BIGINT        NULL,
    operator_name VARCHAR(100)  NULL,
    target_type   VARCHAR(30)   NOT NULL,
    target_id     BIGINT        NULL,
    action        VARCHAR(50)   NOT NULL,
    before_value  TEXT          NULL,
    after_value   TEXT          NULL,
    client_ip     VARCHAR(64)   NULL,
    create_time   TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id)
);

CREATE INDEX idx_auth_audit_target ON sys_auth_audit_log (target_type, target_id);
CREATE INDEX idx_auth_audit_create_time ON sys_auth_audit_log (create_time);
