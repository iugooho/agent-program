package com.travelagent.travelplanner.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 迁移脚本冒烟测试：在 H2 的 MySQL 兼容模式上完整执行 db/migration。
 *
 * <p>生产库是 MySQL 8，本地是 H2(MODE=MySQL)，两边执行同一份脚本。这个测试保证
 * 切库之后脚本仍然可执行，并且自增主键与时间默认值在两边语义一致。</p>
 */
class DatabaseMigrationTest {

    private static final String URL = "jdbc:h2:mem:migration_test;MODE=MySQL;DB_CLOSE_DELAY=-1";
    private static final String USER = "sa";
    private static final String PASSWORD = "";

    @Test
    @DisplayName("Flyway 在 H2(MySQL 模式) 上完整执行迁移脚本")
    void migrationsApplyCleanly() throws Exception {
        Flyway.configure()
                .dataSource(URL, USER, PASSWORD)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO users (phone, nickname) VALUES ('13800000000', 'tester')");

            try (ResultSet row = statement.executeQuery(
                    "SELECT id, created_at FROM users WHERE phone = '13800000000'")) {
                assertTrue(row.next(), "应该能查到刚插入的用户");
                assertEquals(1L, row.getLong("id"), "自增主键应从 1 开始");
                assertNotNull(row.getTimestamp("created_at"), "created_at 应由默认值填充");
            }
        }
    }
}
