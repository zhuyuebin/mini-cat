package com.minicat.minicatserver.Utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SQL 校验器单元测试（纯 JUnit，不启动 Spring 上下文）。
 *
 * <p>注意：本项目是数据库管理工具，SQL 校验是"危险操作提醒"而非安全边界，
 * 因此这里同时锁定"该拦的拦住"与"已知误伤/漏网行为"，避免后续重构悄悄改变语义。
 */
class SqlInjectionValidatorTest {

    private final SqlInjectionValidator validator = new SqlInjectionValidator();

    @Test
    void allowsPlainSelect() {
        assertTrue(validator.validate("SELECT * FROM users WHERE id = 1").isValid());
        assertTrue(validator.validate("SELECT COUNT(*) AS total FROM users").isValid());
        // 行注释是数据库管理场景的合法用法（已知不拦截，作为契约锁定）
        assertTrue(validator.validate("SELECT * FROM users WHERE id = 1 -- comment").isValid());
    }

    @Test
    void blocksSelectBasedInjectionPatterns() {
        assertFalse(validator.validate("SELECT * FROM users UNION SELECT * FROM passwords").isValid());
        assertFalse(validator.validate("SELECT * FROM users /* comment */").isValid());
        assertFalse(validator.validate("SELECT load_file('/etc/passwd')").isValid());
        assertFalse(validator.validate("SELECT sleep(1)").isValid());
        assertFalse(validator.validate("SELECT * FROM information_schema.tables").isValid());
    }

    @Test
    void blocksNonSelectStatementsOnQueryApi() {
        assertFalse(validator.validate("DROP TABLE users").isValid());
        assertFalse(validator.validate("INSERT INTO users VALUES (1)").isValid());
        assertFalse(validator.validate("").isValid());
    }

    @Test
    void updateAndDeleteRequireWhereClause() {
        assertTrue(validator.validateUpdate("UPDATE users SET name = 'test' WHERE id = 1").isValid());
        assertFalse(validator.validateUpdate("UPDATE users SET name = 'test'").isValid());
        assertTrue(validator.validateUpdate("DELETE FROM users WHERE id = 1").isValid());
        assertFalse(validator.validateUpdate("DELETE FROM users").isValid());
        // 多语句注入
        assertFalse(validator.validateUpdate("UPDATE users SET name = 'test'; DROP TABLE users").isValid());
        // 非 UPDATE/DELETE
        assertFalse(validator.validateUpdate("SELECT * FROM users").isValid());
    }

    @Test
    void insertValidationRejectsDangerousPayloads() {
        assertTrue(validator.validateInsert("INSERT INTO users (name) VALUES ('a')").isValid());
        assertFalse(validator.validateInsert("INSERT INTO users (name) VALUES ('a'); DROP TABLE users").isValid());
        assertFalse(validator.validateInsert("UPDATE users SET name = 'a'").isValid());
    }

    @Test
    void dropTableValidationRejectsMultiStatement() {
        assertTrue(validator.validateDropTable("DROP TABLE users").isValid());
        assertFalse(validator.validateDropTable("DROP TABLE users; DROP TABLE orders").isValid());
        assertFalse(validator.validateDropTable("DELETE FROM users").isValid());
    }
}
