package com.minicat.minicatserver;

import com.minicat.minicatserver.Utils.PasswordEncryptionService;
import com.minicat.minicatserver.Utils.SqlInjectionValidator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 安全相关单元测试。
 *
 * <p>使用独立的密钥文件路径，避免测试污染运行目录下的 data/secret.key。
 */
@SpringBootTest(properties = "minicat.security.key-file=target/test-data/secret.key")
public class SecurityTests {

    @Autowired
    private PasswordEncryptionService passwordEncryptionService;

    @Autowired
    private SqlInjectionValidator sqlInjectionValidator;

    @Test
    public void testPasswordEncryption() {
        String rawPassword = "mySecretPassword123";

        // 加密密码
        String encryptedPassword = passwordEncryptionService.encrypt(rawPassword);
        assertNotNull(encryptedPassword);
        assertTrue(encryptedPassword.startsWith("v2:"), "新密文必须带版本前缀，便于未来密钥轮换");
        assertTrue(passwordEncryptionService.isEncrypted(encryptedPassword));
        assertNotEquals(rawPassword, encryptedPassword);

        // 验证密码匹配
        assertTrue(passwordEncryptionService.matches(rawPassword, encryptedPassword));

        // 验证错误密码不匹配
        assertFalse(passwordEncryptionService.matches("wrongPassword", encryptedPassword));
    }

    @Test
    public void testIsEncryptedDoesNotMisjudgePlainText() {
        // 32 位、恰好是合法 Base64 形态的明文密码不应被误判为密文（旧实现会误判）
        assertFalse(passwordEncryptionService.isEncrypted("abcdefghijklmnopqrstuvwxyz123456"));
        assertFalse(passwordEncryptionService.isEncrypted("mySecretPassword123"));
        assertFalse(passwordEncryptionService.isEncrypted(null));
        assertFalse(passwordEncryptionService.isEncrypted(""));
    }

    @Test
    public void testSqlInjectionValidation_Select() {
        // 合法的SELECT查询
        SqlInjectionValidator.SqlValidationResult valid =
            sqlInjectionValidator.validate("SELECT * FROM users WHERE id = 1");
        assertTrue(valid.isValid());

        // 多行/块注释会被拦截
        SqlInjectionValidator.SqlValidationResult invalid1 =
            sqlInjectionValidator.validate("SELECT * FROM users /* comment */ WHERE id = 1");
        assertFalse(invalid1.isValid());

        // 说明：行注释（--）目前不在拦截列表中——本项目是数据库管理工具，
        // SQL 中的行注释是合法用法，强行拦截会误伤正常查询。
        // SQL 校验策略的重新设计放在 M1，此处不把它当作"已防护"来断言。

        // 包含UNION注入的非法查询
        SqlInjectionValidator.SqlValidationResult invalid2 =
            sqlInjectionValidator.validate("SELECT * FROM users UNION SELECT * FROM passwords");
        assertFalse(invalid2.isValid());

        // 非SELECT语句
        SqlInjectionValidator.SqlValidationResult invalid3 =
            sqlInjectionValidator.validate("DROP TABLE users");
        assertFalse(invalid3.isValid());
    }

    @Test
    public void testSqlInjectionValidation_Update() {
        // 合法的UPDATE查询（带WHERE条件）
        SqlInjectionValidator.SqlValidationResult valid =
            sqlInjectionValidator.validateUpdate("UPDATE users SET name = 'test' WHERE id = 1");
        assertTrue(valid.isValid());

        // 没有WHERE条件的UPDATE
        SqlInjectionValidator.SqlValidationResult invalid1 =
            sqlInjectionValidator.validateUpdate("UPDATE users SET name = 'test'");
        assertFalse(invalid1.isValid());

        // 包含危险操作的UPDATE
        SqlInjectionValidator.SqlValidationResult invalid2 =
            sqlInjectionValidator.validateUpdate("UPDATE users SET name = 'test'; DROP TABLE users");
        assertFalse(invalid2.isValid());

        // 合法的DELETE查询（带WHERE条件）
        SqlInjectionValidator.SqlValidationResult valid2 =
            sqlInjectionValidator.validateUpdate("DELETE FROM users WHERE id = 1");
        assertTrue(valid2.isValid());

        // 没有WHERE条件的DELETE
        SqlInjectionValidator.SqlValidationResult invalid3 =
            sqlInjectionValidator.validateUpdate("DELETE FROM users");
        assertFalse(invalid3.isValid());
    }
}
