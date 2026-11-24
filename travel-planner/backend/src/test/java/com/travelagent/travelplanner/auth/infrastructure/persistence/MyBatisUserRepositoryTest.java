package com.travelagent.travelplanner.auth.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.travelagent.travelplanner.auth.domain.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** 新用户默认角色与部门归属测试。 */
@SpringBootTest(properties = {
        "app.auth.redis.enabled=false",
        "app.email.mode=disabled",
        "app.bootstrap-admin.enabled=false"
})
@Transactional
class MyBatisUserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("新注册用户自动获得 USER 角色并归入总部")
    void newUserGetsDefaultRoleAndDepartment() {
        userRepository.createUser(
                "department-default",
                "department-default@example.com",
                "$2a$10$placeholder",
                "默认部门用户");

        Integer roleCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM sys_user_role ur
                JOIN users u ON u.id = ur.user_id
                JOIN sys_role r ON r.id = ur.role_id
                WHERE u.username = 'department-default'
                  AND r.code = 'USER'
                """, Integer.class);
        Integer departmentCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM sys_user_department ud
                JOIN users u ON u.id = ud.user_id
                JOIN sys_department d ON d.id = ud.department_id
                WHERE u.username = 'department-default'
                  AND d.code = 'HEADQUARTERS'
                """, Integer.class);

        assertEquals(1, roleCount);
        assertEquals(1, departmentCount);
    }
}
