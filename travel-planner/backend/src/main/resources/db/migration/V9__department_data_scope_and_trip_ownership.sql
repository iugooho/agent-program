-- 部门树。
CREATE TABLE sys_department (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    parent_id      BIGINT       NULL,
    code           VARCHAR(50)  NOT NULL,
    name           VARCHAR(100) NOT NULL,
    leader_user_id BIGINT       NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    sort_order     INT          NOT NULL DEFAULT 0,
    create_time    TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    update_time    TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_sys_department_code UNIQUE (code),
    CONSTRAINT fk_sys_department_parent
        FOREIGN KEY (parent_id) REFERENCES sys_department (id) ON DELETE SET NULL,
    CONSTRAINT fk_sys_department_leader
        FOREIGN KEY (leader_user_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_sys_department_status
        CHECK (status IN ('ACTIVE', 'DISABLED', 'DELETED'))
);

CREATE INDEX idx_sys_department_parent ON sys_department (parent_id);

CREATE TABLE sys_user_department (
    user_id        BIGINT       NOT NULL,
    department_id  BIGINT       NOT NULL,
    create_time    TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (user_id, department_id),
    CONSTRAINT fk_user_department_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_department_department
        FOREIGN KEY (department_id) REFERENCES sys_department (id) ON DELETE CASCADE
);

CREATE INDEX idx_user_department_department ON sys_user_department (department_id);

INSERT INTO sys_department (parent_id, code, name, status, sort_order)
VALUES (NULL, 'HEADQUARTERS', '总部', 'ACTIVE', 10);

INSERT INTO sys_department (parent_id, code, name, status, sort_order)
SELECT id, 'TECH', '技术部', 'ACTIVE', 20
FROM sys_department
WHERE code = 'HEADQUARTERS';

INSERT INTO sys_department (parent_id, code, name, status, sort_order)
SELECT id, 'OPERATIONS', '运营部', 'ACTIVE', 30
FROM sys_department
WHERE code = 'HEADQUARTERS';

INSERT INTO sys_user_department (user_id, department_id)
SELECT u.id, d.id
FROM users u
JOIN sys_department d ON d.code = 'HEADQUARTERS'
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_user_department existing
    WHERE existing.user_id = u.id
);

-- 角色数据范围。
ALTER TABLE sys_role
    ADD COLUMN data_scope VARCHAR(30) NOT NULL DEFAULT 'SELF';

ALTER TABLE sys_role
    ADD CONSTRAINT ck_sys_role_data_scope
        CHECK (data_scope IN (
            'ALL',
            'DEPT_CUSTOM',
            'DEPT_ONLY',
            'DEPT_AND_CHILD',
            'SELF'
        ));

UPDATE sys_role
SET data_scope = 'SELF'
WHERE code = 'USER';

UPDATE sys_role
SET data_scope = 'DEPT_AND_CHILD'
WHERE code = 'ADMIN';

UPDATE sys_role
SET data_scope = 'ALL'
WHERE code = 'SUPER_ADMIN';

CREATE TABLE sys_role_data_scope_dept (
    role_id       BIGINT NOT NULL,
    department_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, department_id),
    CONSTRAINT fk_role_data_scope_role
        FOREIGN KEY (role_id) REFERENCES sys_role (id) ON DELETE CASCADE,
    CONSTRAINT fk_role_data_scope_department
        FOREIGN KEY (department_id) REFERENCES sys_department (id) ON DELETE CASCADE
);

-- 持久化旅行计划，作为资源归属和数据权限的实际业务对象。
CREATE TABLE travel_plan (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    owner_id           BIGINT       NOT NULL,
    department_id      BIGINT       NULL,
    request_payload    LONGTEXT     NOT NULL,
    itinerary_payload  LONGTEXT     NOT NULL,
    mode               VARCHAR(30)  NOT NULL,
    create_time        TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    update_time        TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_travel_plan_owner
        FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_travel_plan_department
        FOREIGN KEY (department_id) REFERENCES sys_department (id) ON DELETE SET NULL
);

CREATE INDEX idx_travel_plan_owner ON travel_plan (owner_id);
CREATE INDEX idx_travel_plan_department ON travel_plan (department_id);
