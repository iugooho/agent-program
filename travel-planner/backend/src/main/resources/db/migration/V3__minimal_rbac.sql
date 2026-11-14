-- 最小可用 RBAC：角色、权限点、用户角色和角色权限。
--
-- 第一版只放最基础的三个角色和九个权限，后续通过后台可视化界面继续分配。
-- users.role 暂时保留用于兼容旧 JWT 与旧数据，V3 会把已有账号同步到新关系表。

CREATE TABLE sys_role (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    code        VARCHAR(50)  NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500) NULL,
    built_in    BOOLEAN      NOT NULL DEFAULT FALSE,
    create_time TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    update_time TIMESTAMP(6) NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_sys_role_code UNIQUE (code)
);

CREATE TABLE sys_permission (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    code        VARCHAR(100)  NOT NULL,
    name        VARCHAR(100)  NOT NULL,
    module      VARCHAR(100)  NOT NULL,
    description VARCHAR(500)  NULL,
    sort_order  INT           NOT NULL DEFAULT 0,
    create_time TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_sys_permission_code UNIQUE (code)
);

CREATE TABLE sys_user_role (
    user_id     BIGINT       NOT NULL,
    role_id     BIGINT       NOT NULL,
    create_time TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_sys_user_role_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_sys_user_role_role
        FOREIGN KEY (role_id) REFERENCES sys_role (id) ON DELETE CASCADE
);

CREATE TABLE sys_role_permission (
    role_id       BIGINT       NOT NULL,
    permission_id BIGINT       NOT NULL,
    create_time   TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_sys_role_permission_role
        FOREIGN KEY (role_id) REFERENCES sys_role (id) ON DELETE CASCADE,
    CONSTRAINT fk_sys_role_permission_permission
        FOREIGN KEY (permission_id) REFERENCES sys_permission (id) ON DELETE CASCADE
);

CREATE INDEX idx_sys_user_role_role ON sys_user_role (role_id);
CREATE INDEX idx_sys_role_permission_permission ON sys_role_permission (permission_id);

INSERT INTO sys_role (code, name, description, built_in) VALUES
    ('USER', '普通用户', '使用个人资料、行程规划等基础功能', TRUE),
    ('ADMIN', '管理员', '使用后台基础查询与审计功能', TRUE),
    ('SUPER_ADMIN', '超级管理员', '管理角色权限和用户角色分配', TRUE);

INSERT INTO sys_permission (code, name, module, description, sort_order) VALUES
    ('user.profile.read', '查看个人资料', '用户中心', '查看当前用户的个人资料', 10),
    ('user.profile.update', '修改个人资料', '用户中心', '修改当前用户的个人资料', 20),
    ('trip.create', '创建行程', '行程规划', '发起新的旅行规划', 30),
    ('trip.read', '查看行程', '行程规划', '查看本人行程', 40),
    ('trip.delete', '删除行程', '行程规划', '删除本人行程', 50),
    ('admin.console.access', '进入管理后台', '系统管理', '进入企业后台基础页面', 60),
    ('admin.user.read', '查看用户列表', '用户管理', '查看后台用户数据', 70),
    ('admin.audit.read', '查看审计日志', '审计日志', '查看后台操作审计日志', 80),
    ('admin.rbac.manage', '管理角色权限', '权限管理', '分配用户角色和角色权限', 90);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
CROSS JOIN sys_permission p
WHERE r.code = 'USER'
  AND p.code IN (
      'user.profile.read',
      'user.profile.update',
      'trip.create',
      'trip.read',
      'trip.delete'
  );

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
CROSS JOIN sys_permission p
WHERE r.code = 'ADMIN'
  AND p.code <> 'admin.rbac.manage';

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
CROSS JOIN sys_permission p
WHERE r.code = 'SUPER_ADMIN';

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN sys_role r ON r.code = CASE WHEN u.role = 'ADMIN' THEN 'ADMIN' ELSE 'USER' END;

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN sys_role r ON r.code = 'SUPER_ADMIN'
WHERE u.username = 'admin'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_user_role ur
      WHERE ur.user_id = u.id
        AND ur.role_id = r.id
  );

-- 如果项目没有使用默认 admin 账号，则把最早创建的管理员提升为超级管理员，
-- 保证首次启用 RBAC 后仍有人可以进入权限分配页面。
INSERT INTO sys_user_role (user_id, role_id)
SELECT first_admin.id, r.id
FROM users first_admin
JOIN sys_role r ON r.code = 'SUPER_ADMIN'
WHERE first_admin.id = (
        SELECT MIN(candidate.id)
        FROM users candidate
        WHERE candidate.role = 'ADMIN'
    )
  AND NOT EXISTS (
      SELECT 1
      FROM sys_user_role existing_super
      WHERE existing_super.role_id = r.id
  );
