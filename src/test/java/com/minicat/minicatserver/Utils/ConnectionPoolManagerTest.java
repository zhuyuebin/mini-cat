package com.minicat.minicatserver.Utils;

import com.minicat.minicatserver.config.HikariConfigProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 连接池生命周期单元测试（使用 H2 内存库作为目标数据库，不启动 Spring 上下文）。
 */
class ConnectionPoolManagerTest {

    private final ConnectionPoolManager manager = newManager();

    private static ConnectionPoolManager newManager() {
        HikariConfigProperties properties = new HikariConfigProperties();
        properties.setMaximumPoolSize(5);
        properties.setMinimumIdle(1);
        properties.setConnectionTimeout(5000);
        properties.setLeakDetectionThreshold(0);
        return new ConnectionPoolManager(properties);
    }

    private static String h2Url() {
        return "jdbc:h2:mem:pool_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
    }

    private static ConnectionPoolManager.PoolKey key(String connectionId, String database) {
        return new ConnectionPoolManager.PoolKey(connectionId, database);
    }

    @AfterEach
    void tearDown() {
        manager.closeAllPools();
    }

    @Test
    void poolMaximumSizeIsFive() throws Exception {
        String connectionId = UUID.randomUUID().toString();
        ConnectionPoolManager.PoolKey poolKey = key(connectionId, "db1");

        try (Connection connection = manager.getConnection(poolKey, h2Url(), "sa", "")) {
            assertNotNull(connection);
        }

        assertNotNull(manager.getPoolStatus(poolKey));
        assertEquals(5, manager.getPoolStatus(poolKey).get("maximumPoolSize"),
                "单池上限必须固定为 5");
        assertEquals(1, manager.getPoolStatus(poolKey).get("minimumIdle"));
    }

    @Test
    void closingOneConnectionOnlyClosesItsOwnPools() throws Exception {
        String connectionA = UUID.randomUUID().toString();
        String connectionB = UUID.randomUUID().toString();
        ConnectionPoolManager.PoolKey a1 = key(connectionA, "db1");
        ConnectionPoolManager.PoolKey a2 = key(connectionA, "db2");
        ConnectionPoolManager.PoolKey b1 = key(connectionB, "db1");

        manager.getConnection(a1, h2Url(), "sa", "").close();
        manager.getConnection(a2, h2Url(), "sa", "").close();
        manager.getConnection(b1, h2Url(), "sa", "").close();
        assertEquals(3, manager.getActivePoolCount());

        manager.closePoolsForConnection(connectionA);

        assertNull(manager.getPoolStatus(a1), "连接 A 的 db1 池应被关闭");
        assertNull(manager.getPoolStatus(a2), "连接 A 的 db2 池应被关闭");
        assertNotNull(manager.getPoolStatus(b1), "连接 B 的池不应受影响");
        assertEquals(1, manager.getActivePoolCount());
    }

    @Test
    void concurrentBorrowAndCloseDoesNotDeadlockAndManagerStaysUsable() throws Exception {
        String connectionId = UUID.randomUUID().toString();
        ConnectionPoolManager.PoolKey poolKey = key(connectionId, "db");
        String jdbcUrl = h2Url();

        ExecutorService executor = Executors.newFixedThreadPool(5);
        AtomicInteger borrowSuccess = new AtomicInteger();
        AtomicInteger closeRuns = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        // 4 个线程持续借还连接
        for (int t = 0; t < 4; t++) {
            futures.add(executor.submit(() -> {
                for (int i = 0; i < 40; i++) {
                    try (Connection connection = manager.getConnection(poolKey, jdbcUrl, "sa", "")) {
                        if (connection.isValid(1)) {
                            borrowSuccess.incrementAndGet();
                        }
                    } catch (SQLException e) {
                        // 关池瞬间与正在使用的连接存在竞争属于可接受现象；
                        // 此用例只验证"不死锁、不崩溃、管理器仍可用"
                    }
                }
                return null;
            }));
        }

        // 1 个线程模拟"修改连接配置"反复关池
        futures.add(executor.submit(() -> {
            for (int i = 0; i < 15; i++) {
                manager.closePoolsForConnection(connectionId);
                closeRuns.incrementAndGet();
                Thread.sleep(5);
            }
            return null;
        }));

        for (Future<?> future : futures) {
            future.get(60, TimeUnit.SECONDS);
        }
        executor.shutdownNow();

        assertEquals(15, closeRuns.get());
        assertTrue(borrowSuccess.get() > 0, "并发关池期间仍应有成功的借还连接");

        // 并发关闭之后，管理器应仍然可用（懒重建）
        try (Connection connection = manager.getConnection(poolKey, jdbcUrl, "sa", "")) {
            assertTrue(connection.isValid(1));
        }
        assertNotNull(manager.getPoolStatus(poolKey));
    }
}
