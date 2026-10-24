package com.minicat.minicatserver;

import com.minicat.minicatserver.Services.DatabaseService;
import com.minicat.minicatserver.Utils.PasswordEncryptionService;
import com.minicat.minicatserver.entity.DatabaseConnection;
import com.minicat.minicatserver.entity.DatabaseConnectionEntity;
import com.minicat.minicatserver.repository.DatabaseConnectionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 连接管理服务集成测试。
 *
 * <p>元数据库使用 application-test.properties 中的 H2 内存库；
 * 类级 @Transactional 保证每个用例结束后数据回滚，测试之间完全隔离。
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DatabaseConnectionServiceTest {

    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private DatabaseConnectionRepository repository;

    @Autowired
    private PasswordEncryptionService passwordEncryptionService;

    @Test
    void addConnectionEncryptsPasswordBeforePersisting() {
        DatabaseConnection saved = databaseService.addConnection(newConnection("it-mysql"));

        assertNotNull(saved.getId());
        DatabaseConnectionEntity entity = repository.findById(saved.getId()).orElseThrow();
        assertTrue(entity.getPassword().startsWith("v2:"), "落库密码必须是加密后的 v2 密文");
        assertNotEquals("secret-123", entity.getPassword());
        assertEquals("secret-123", passwordEncryptionService.decrypt(entity.getPassword()));
        assertNotNull(entity.getCreatedAt());
    }

    @Test
    void updateWithoutPasswordKeepsExistingCiphertext() {
        DatabaseConnection saved = databaseService.addConnection(newConnection("it-keep-password"));
        String originalCiphertext = repository.findById(saved.getId()).orElseThrow().getPassword();

        DatabaseConnection update = newConnection("it-keep-password");
        update.setPassword(null);
        assertTrue(databaseService.updateConnection(saved.getId(), update));

        String afterCiphertext = repository.findById(saved.getId()).orElseThrow().getPassword();
        assertEquals(originalCiphertext, afterCiphertext, "密码留空时不应重新加密或覆盖");
    }

    @Test
    void updateWithNewPasswordReEncrypts() {
        DatabaseConnection saved = databaseService.addConnection(newConnection("it-new-password"));
        String oldCiphertext = repository.findById(saved.getId()).orElseThrow().getPassword();

        DatabaseConnection update = newConnection("it-new-password");
        update.setPassword("new-secret-456");
        assertTrue(databaseService.updateConnection(saved.getId(), update));

        String newCiphertext = repository.findById(saved.getId()).orElseThrow().getPassword();
        assertNotEquals(oldCiphertext, newCiphertext);
        assertEquals("new-secret-456", passwordEncryptionService.decrypt(newCiphertext));
    }

    @Test
    void deleteConnectionRemovesIt() {
        DatabaseConnection saved = databaseService.addConnection(newConnection("it-delete"));

        assertTrue(databaseService.deleteConnection(saved.getId()));
        assertTrue(repository.findById(saved.getId()).isEmpty());
        assertFalse(databaseService.deleteConnection(saved.getId()), "重复删除应返回 false");
    }

    @Test
    void getAllConnectionsReturnsSavedConnections() {
        databaseService.addConnection(newConnection("it-list"));
        List<DatabaseConnection> all = databaseService.getAllConnections();
        assertFalse(all.isEmpty());
        assertTrue(all.stream().anyMatch(c -> "it-list".equals(c.getName())));
    }

    private static DatabaseConnection newConnection(String name) {
        DatabaseConnection connection = new DatabaseConnection();
        connection.setName(name);
        connection.setHost("localhost");
        connection.setPort(3306);
        connection.setUsername("root");
        connection.setPassword("secret-123");
        connection.setDatabaseType("mysql");
        connection.setDatabaseName("demo");
        connection.setActive(true);
        return connection;
    }
}
