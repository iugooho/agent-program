package com.travelagent.travelplanner.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 迁移脚本冒烟测试：在 H2 的 MySQL 兼容模式上完整执行 db/migration。
 *
 * <p>生产库是 MySQL 8，本地是 H2(MODE=MySQL)，两边执行同一份脚本。这个测试保证
 * 切库之后脚本仍然可执行，并且自增主键与时间默认值在两边语义一致。</p>
 */
class DatabaseMigrationTest {

    private static final String USER = "sa";
    private static final String PASSWORD = "";

    @Test
    @DisplayName("Flyway 在 H2(MySQL 模式) 上完整执行迁移脚本")
    void migrationsApplyCleanly() throws Exception {
        String url = "jdbc:h2:mem:migration_test;MODE=MySQL;DB_CLOSE_DELAY=-1";
        Flyway.configure()
                .dataSource(url, USER, PASSWORD)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(url, USER, PASSWORD);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO users (username, email, password_hash, nickname)
                    VALUES ('tester', 'tester@example.com', '$2a$10$placeholder', 'tester')
                    """);

            try (ResultSet row = statement.executeQuery("SELECT COUNT(*) FROM sys_role")) {
                assertTrue(row.next());
                assertEquals(3, row.getInt(1), "应初始化三个基础角色");
            }

            try (ResultSet row = statement.executeQuery("SELECT COUNT(*) FROM sys_permission")) {
                assertTrue(row.next());
                assertEquals(9, row.getInt(1), "应初始化九个基础权限");
            }

            try (ResultSet row = statement.executeQuery(
                    "SELECT id, role, status, create_time, update_time FROM users WHERE username = 'tester'")) {
                assertTrue(row.next(), "应该能查到刚插入的用户");
                assertEquals(1L, row.getLong("id"), "自增主键应从 1 开始");
                assertEquals("USER", row.getString("role"), "角色应使用默认值");
                assertEquals("ACTIVE", row.getString("status"), "状态应使用默认值");
                assertNotNull(row.getTimestamp("create_time"), "create_time 应由默认值填充");
                assertNotNull(row.getTimestamp("update_time"), "update_time 应由默认值填充");
            }

            statement.executeUpdate("""
                    INSERT INTO user_preferences (user_id, forbidden_list)
                    VALUES (1, '["不购物"]')
                    """);
            statement.executeUpdate("DELETE FROM users WHERE id = 1");

            try (ResultSet row = statement.executeQuery("SELECT COUNT(*) FROM user_preferences")) {
                assertTrue(row.next());
                assertEquals(0, row.getInt(1), "删除用户时应级联删除长期偏好");
            }
        }
    }

    @Test
    @DisplayName("已有旧用户时拒绝生成不可信的登录凭据")
    void migrationRejectsLegacyUsersWithoutAccountData() throws Exception {
        String url = "jdbc:h2:mem:migration_guard_test;MODE=MySQL;DB_CLOSE_DELAY=-1";
        Flyway.configure()
                .dataSource(url, USER, PASSWORD)
                .locations("classpath:db/migration")
                .target("1")
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(url, USER, PASSWORD);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO users (phone, nickname) VALUES ('13800000000', 'legacy')");
        }

        Flyway v2 = Flyway.configure()
                .dataSource(url, USER, PASSWORD)
                .locations("classpath:db/migration")
                .target("2")
                .load();
        assertThrows(FlywayException.class, v2::migrate, "存在旧用户时必须阻止自动迁移");
    }

    @Test
    @DisplayName("V3 将已有管理员迁移到最小 RBAC 并保留超级管理员入口")
    void rbacMigrationBootstrapsExistingAdmin() throws Exception {
        String url = "jdbc:h2:mem:migration_rbac_admin_test;MODE=MySQL;DB_CLOSE_DELAY=-1";
        Flyway.configure()
                .dataSource(url, USER, PASSWORD)
                .locations("classpath:db/migration")
                .target("2")
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(url, USER, PASSWORD);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO users (username, email, password_hash, role, status, nickname)
                    VALUES ('legacy_admin', 'legacy-admin@example.com', '$2a$10$placeholder',
                            'ADMIN', 'ACTIVE', 'legacy admin')
                    """);
        }

        Flyway.configure()
                .dataSource(url, USER, PASSWORD)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(url, USER, PASSWORD);
             Statement statement = connection.createStatement();
             ResultSet row = statement.executeQuery("""
                     SELECT COUNT(*)
                     FROM sys_user_role ur
                     JOIN sys_role r ON r.id = ur.role_id
                     JOIN users u ON u.id = ur.user_id
                     WHERE u.username = 'legacy_admin'
                       AND r.code IN ('ADMIN', 'SUPER_ADMIN')
                     """)) {
            assertTrue(row.next());
            assertEquals(2, row.getInt(1), "旧管理员应同时拥有 ADMIN 与 SUPER_ADMIN");
        }
    }
}
