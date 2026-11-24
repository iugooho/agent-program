-- 高风险权限标记。
ALTER TABLE sys_permission
    ADD COLUMN risk_level VARCHAR(20) NOT NULL DEFAULT 'LOW';

UPDATE sys_permission
SET risk_level = 'HIGH'
WHERE code IN ('admin.rbac.manage', 'admin.user.read');

UPDATE sys_permission
SET risk_level = 'MEDIUM'
WHERE code IN ('admin.console.access', 'admin.audit.read');

ALTER TABLE sys_permission
    ADD CONSTRAINT ck_sys_permission_risk
        CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH'));

-- 权限包用于快速给角色套用一组常用权限。
CREATE TABLE sys_permission_package (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    code        VARCHAR(50)  NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500) NULL,
    built_in    BOOLEAN      NOT NULL DEFAULT TRUE,
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    sort_order  INT          NOT NULL DEFAULT 0,
    create_time TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    update_time TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_sys_permission_package_code UNIQUE (code)
);

CREATE TABLE sys_permission_package_item (
    package_id    BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (package_id, permission_id),
    CONSTRAINT fk_package_item_package
        FOREIGN KEY (package_id) REFERENCES sys_permission_package (id) ON DELETE CASCADE,
    CONSTRAINT fk_package_item_permission
        FOREIGN KEY (permission_id) REFERENCES sys_permission (id) ON DELETE CASCADE
);

INSERT INTO sys_permission_package (
    code, name, description, sort_order
) VALUES
    ('USER_BASIC', '用户基础包', '个人资料查看和修改权限', 10),
    ('TRIP_OPERATOR', '行程操作包', '创建、查看和删除本人行程', 20),
    ('ADMIN_CONSOLE', '后台基础包', '进入后台并查看用户与审计数据', 30),
    ('AUDITOR', '审计观察包', '后台入口与审计日志查看', 40),
    ('RBAC_MANAGER', '权限管理包', '后台入口和角色权限管理', 50);

INSERT INTO sys_permission_package_item (package_id, permission_id)
SELECT pp.id, p.id
FROM sys_permission_package pp
JOIN sys_permission p ON p.code IN ('user.profile.read', 'user.profile.update')
WHERE pp.code = 'USER_BASIC';

INSERT INTO sys_permission_package_item (package_id, permission_id)
SELECT pp.id, p.id
FROM sys_permission_package pp
JOIN sys_permission p ON p.code IN ('trip.create', 'trip.read', 'trip.delete')
WHERE pp.code = 'TRIP_OPERATOR';

INSERT INTO sys_permission_package_item (package_id, permission_id)
SELECT pp.id, p.id
FROM sys_permission_package pp
JOIN sys_permission p
  ON p.code IN ('admin.console.access', 'admin.user.read', 'admin.audit.read')
WHERE pp.code = 'ADMIN_CONSOLE';

INSERT INTO sys_permission_package_item (package_id, permission_id)
SELECT pp.id, p.id
FROM sys_permission_package pp
JOIN sys_permission p ON p.code IN ('admin.console.access', 'admin.audit.read')
WHERE pp.code = 'AUDITOR';

INSERT INTO sys_permission_package_item (package_id, permission_id)
SELECT pp.id, p.id
FROM sys_permission_package pp
JOIN sys_permission p ON p.code IN ('admin.console.access', 'admin.rbac.manage')
WHERE pp.code = 'RBAC_MANAGER';

-- 权限依赖用于自动补齐前置权限。
CREATE TABLE sys_permission_dependency (
    permission_id          BIGINT NOT NULL,
    required_permission_id BIGINT NOT NULL,
    PRIMARY KEY (permission_id, required_permission_id),
    CONSTRAINT fk_permission_dependency_permission
        FOREIGN KEY (permission_id) REFERENCES sys_permission (id) ON DELETE CASCADE,
    CONSTRAINT fk_permission_dependency_required
        FOREIGN KEY (required_permission_id) REFERENCES sys_permission (id) ON DELETE CASCADE
);

INSERT INTO sys_permission_dependency (permission_id, required_permission_id)
SELECT p.id, required.id
FROM sys_permission p
JOIN sys_permission required ON required.code = 'admin.console.access'
WHERE p.code IN ('admin.user.read', 'admin.audit.read', 'admin.rbac.manage');

INSERT INTO sys_permission_dependency (permission_id, required_permission_id)
SELECT p.id, required.id
FROM sys_permission p
JOIN sys_permission required ON required.code = 'trip.read'
WHERE p.code = 'trip.delete';

INSERT INTO sys_permission_dependency (permission_id, required_permission_id)
SELECT p.id, required.id
FROM sys_permission p
JOIN sys_permission required ON required.code = 'user.profile.read'
WHERE p.code = 'user.profile.update';
