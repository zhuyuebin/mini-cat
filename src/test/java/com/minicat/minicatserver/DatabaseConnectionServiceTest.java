package com.minicat.minicatserver;

import com.minicat.minicatserver.Services.DatabaseService;
import com.minicat.minicatserver.Utils.ConnectionPoolManager;
import com.minicat.minicatserver.Utils.PasswordEncryptionService;
import com.minicat.minicatserver.entity.DatabaseConnection;
import com.minicat.minicatserver.entity.DatabaseConnectionEntity;
import com.minicat.minicatserver.exception.ConnectionNotFoundException;
import com.minicat.minicatserver.exception.SqlInjectionException;
import com.minicat.minicatserver.exception.ValidationException;
import com.minicat.minicatserver.repository.DatabaseConnectionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Autowired
    private ConnectionPoolManager connectionPoolManager;

    @AfterEach
    void tearDown() {
        connectionPoolManager.closeAllPools();
    }

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

    @Test
    void addConnectionValidationUsesSemanticException() {
        DatabaseConnection invalid = newConnection("it-invalid");
        invalid.setHost("  ");

        ValidationException ex = assertThrows(ValidationException.class,
                () -> databaseService.addConnection(invalid));
        assertTrue(ex.getMessage().contains("主机"));
    }

    @Test
    void unknownConnectionOperationsThrowNotFound() {
        assertThrows(ConnectionNotFoundException.class, () -> databaseService.getDatabases("not-exists"));
        assertThrows(ConnectionNotFoundException.class,
                () -> databaseService.executeQuery("not-exists", "demo", "SELECT 1"));
        assertThrows(ConnectionNotFoundException.class,
                () -> databaseService.dropTable("not-exists", "demo", "t"));
        assertThrows(ConnectionNotFoundException.class,
                () -> databaseService.getColumns("not-exists", "demo", "t"));
    }

    @Test
    void blockedSqlThrowsSqlInjectionException() {
        DatabaseConnection saved = databaseService.addConnection(newConnection("it-blocked-sql"));

        assertThrows(SqlInjectionException.class,
                () -> databaseService.executeQuery(saved.getId(), "demo",
                        "SELECT * FROM a UNION SELECT * FROM b"));
        assertThrows(SqlInjectionException.class,
                () -> databaseService.executeUpdate(saved.getId(), "demo", "DELETE FROM a"));
    }

    @Test
    void unsupportedUpdateTypeThrowsValidationException() {
        DatabaseConnection saved = databaseService.addConnection(newConnection("it-unsupported-update"));

        ValidationException ex = assertThrows(ValidationException.class,
                () -> databaseService.executeUpdate(saved.getId(), "demo", "ALTER TABLE a ADD COLUMN b INT"));
        assertTrue(ex.getMessage().contains("不支持的SQL操作类型"));
    }

    @Test
    void updateAndDeleteRecycleConnectionPools() throws Exception {
        DatabaseConnection saved = databaseService.addConnection(newConnection("it-pool-lifecycle"));
        ConnectionPoolManager.PoolKey poolKey =
                new ConnectionPoolManager.PoolKey(saved.getId(), "demo");
        String jdbcUrl = "jdbc:h2:mem:pool_lifecycle_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";

        try (Connection connection = connectionPoolManager.getConnection(poolKey, jdbcUrl, "sa", "")) {
            assertNotNull(connection);
        }
        assertNotNull(connectionPoolManager.getPoolStatus(poolKey), "获取连接后应创建连接池");

        // 修改密码后，旧池必须被关闭（下次访问会用新密码重建）
        DatabaseConnection update = newConnection("it-pool-lifecycle");
        update.setPassword("new-secret-456");
        assertTrue(databaseService.updateConnection(saved.getId(), update));
        assertNull(connectionPoolManager.getPoolStatus(poolKey), "修改连接配置后旧连接池必须被关闭");

        // 重建池后删除连接，池同样要被关闭
        connectionPoolManager.getConnection(poolKey, jdbcUrl, "sa", "").close();
        assertNotNull(connectionPoolManager.getPoolStatus(poolKey));
        assertTrue(databaseService.deleteConnection(saved.getId()));
        assertNull(connectionPoolManager.getPoolStatus(poolKey), "删除连接后连接池必须被关闭");
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
