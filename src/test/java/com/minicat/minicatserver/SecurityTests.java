package com.minicat.minicatserver;

import com.minicat.minicatserver.Utils.PasswordEncryptionService;
import com.minicat.minicatserver.Utils.SqlInjectionValidator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
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
        assertTrue(passwordEncryptionService.isEncrypted(encryptedPassword));
        assertNotEquals(rawPassword, encryptedPassword);
        
        // 验证密码匹配
        assertTrue(passwordEncryptionService.matches(rawPassword, encryptedPassword));
        
        // 验证错误密码不匹配
        assertFalse(passwordEncryptionService.matches("wrongPassword", encryptedPassword));
    }
    
    @Test
    public void testSqlInjectionValidation_Select() {
        // 合法的SELECT查询
        SqlInjectionValidator.SqlValidationResult valid = 
            sqlInjectionValidator.validate("SELECT * FROM users WHERE id = 1");
        assertTrue(valid.isValid());
        
        // 包含SQL注释的非法查询
        SqlInjectionValidator.SqlValidationResult invalid1 = 
            sqlInjectionValidator.validate("SELECT * FROM users WHERE id = 1 -- comment");
        assertFalse(invalid1.isValid());
        
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
