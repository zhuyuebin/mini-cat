package com.minicat.minicatserver.Utils;

import com.minicat.minicatserver.config.HikariConfigProperties;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 目标数据库连接池管理器。
 *
 * <p>池的生命周期以"连接配置（connectionId）+ 目标数据库"为粒度：
 * <ul>
 *   <li>同一个 connectionId 下的所有池可以一起关闭，用于"修改连接配置/删除连接"后回收旧凭据；</li>
 *   <li>取连接 / 建池 / 关池 对同一个 connectionId 加锁串行化，
 *       避免 update 关闭旧池的同时有查询线程把旧池重新创建出来。</li>
 * </ul>
 */
@Slf4j
@Component
public class ConnectionPoolManager {

    /** 连接池标识：一个连接配置下的一个目标数据库对应一个池。 */
    public record PoolKey(String connectionId, String databaseName) {
        public PoolKey {
            if (connectionId == null || connectionId.isBlank()) {
                throw new IllegalArgumentException("connectionId must not be blank");
            }
            databaseName = databaseName == null ? "" : databaseName;
        }
    }

    private final Map<PoolKey, HikariDataSource> connectionPools = new ConcurrentHashMap<>();
    private final Map<String, Object> connectionLocks = new ConcurrentHashMap<>();
    private final HikariConfigProperties hikariConfigProperties;

    public ConnectionPoolManager(HikariConfigProperties hikariConfigProperties) {
        this.hikariConfigProperties = hikariConfigProperties;
    }

    private Object lockFor(String connectionId) {
        return connectionLocks.computeIfAbsent(connectionId, key -> new Object());
    }

    /**
     * 从指定池获取连接；池不存在或已关闭时按需创建。
     */
    public Connection getConnection(PoolKey poolKey, String jdbcUrl, String username, String password)
            throws SQLException {
        synchronized (lockFor(poolKey.connectionId())) {
            HikariDataSource dataSource = connectionPools.get(poolKey);
            if (dataSource == null || dataSource.isClosed()) {
                dataSource = createConnectionPool(poolKey, jdbcUrl, username, password);
                connectionPools.put(poolKey, dataSource);
                log.info("Connection pool created: connectionId={}, database={}",
                        poolKey.connectionId(), poolKey.databaseName());
            }
            return dataSource.getConnection();
        }
    }

    /**
     * 关闭指定的单个连接池。
     */
    public void closePool(PoolKey poolKey) {
        synchronized (lockFor(poolKey.connectionId())) {
            HikariDataSource dataSource = connectionPools.remove(poolKey);
            closeQuietly(dataSource, poolKey);
        }
    }

    /**
     * 关闭某个连接配置下的所有连接池（修改配置、删除连接时调用）。
     */
    public void closePoolsForConnection(String connectionId) {
        if (connectionId == null || connectionId.isBlank()) {
            return;
        }
        synchronized (lockFor(connectionId)) {
            int closed = 0;
            Iterator<Map.Entry<PoolKey, HikariDataSource>> iterator = connectionPools.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<PoolKey, HikariDataSource> entry = iterator.next();
                if (entry.getKey().connectionId().equals(connectionId)) {
                    iterator.remove();
                    closeQuietly(entry.getValue(), entry.getKey());
                    closed++;
                }
            }
            if (closed > 0) {
                log.info("Connection pools closed: connectionId={}, count={}", connectionId, closed);
            }
        }
    }

    /**
     * 关闭所有连接池（应用关闭 / 测试清理）。
     */
    public void closeAllPools() {
        for (PoolKey poolKey : connectionPools.keySet()) {
            closePool(poolKey);
        }
    }

    @PreDestroy
    public void destroy() {
        closeAllPools();
    }

    /**
     * 当前活跃池数量（监控/测试用）。
     */
    public int getActivePoolCount() {
        return connectionPools.size();
    }

    /**
     * 获取连接池状态信息；池不存在时返回 null。
     */
    public Map<String, Object> getPoolStatus(PoolKey poolKey) {
        HikariDataSource dataSource = connectionPools.get(poolKey);
        if (dataSource == null || dataSource.isClosed()) {
            return null;
        }

        Map<String, Object> status = new ConcurrentHashMap<>();
        status.put("connectionId", poolKey.connectionId());
        status.put("database", poolKey.databaseName());
        status.put("poolName", dataSource.getPoolName());
        status.put("maximumPoolSize", dataSource.getMaximumPoolSize());
        status.put("minimumIdle", dataSource.getMinimumIdle());

        HikariPoolMXBean mxBean = dataSource.getHikariPoolMXBean();
        if (mxBean != null) {
            status.put("activeConnections", mxBean.getActiveConnections());
            status.put("idleConnections", mxBean.getIdleConnections());
            status.put("totalConnections", mxBean.getTotalConnections());
            status.put("threadsAwaitingConnection", mxBean.getThreadsAwaitingConnection());
        }
        return status;
    }

    private void closeQuietly(HikariDataSource dataSource, PoolKey poolKey) {
        if (dataSource == null || dataSource.isClosed()) {
            return;
        }
        try {
            dataSource.close();
            log.info("Connection pool closed: connectionId={}, database={}",
                    poolKey.connectionId(), poolKey.databaseName());
        } catch (Exception e) {
            log.warn("Failed to close connection pool: connectionId={}, database={}",
                    poolKey.connectionId(), poolKey.databaseName(), e);
        }
    }

    /**
     * 创建新的连接池。单池上限由 {@code spring.datasource.hikari.maximum-pool-size} 控制（默认 5）。
     */
    private HikariDataSource createConnectionPool(PoolKey poolKey, String jdbcUrl,
                                                  String username, String password) {
        HikariConfig config = new HikariConfig();

        // 基本配置
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);

        // 从配置文件读取连接池配置
        config.setMaximumPoolSize(hikariConfigProperties.getMaximumPoolSize());
        config.setMinimumIdle(hikariConfigProperties.getMinimumIdle());
        config.setConnectionTimeout(hikariConfigProperties.getConnectionTimeout());
        config.setIdleTimeout(hikariConfigProperties.getIdleTimeout());
        config.setMaxLifetime(hikariConfigProperties.getMaxLifetime());
        config.setAutoCommit(hikariConfigProperties.isAutoCommit());
        config.setLeakDetectionThreshold(hikariConfigProperties.getLeakDetectionThreshold());

        // 唯一池名（JMX/日志可区分不同连接和数据库）
        String uniqueSuffix = poolKey.connectionId().length() > 8
                ? poolKey.connectionId().substring(0, 8)
                : poolKey.connectionId();
        String databasePart = poolKey.databaseName().isEmpty()
                ? "default"
                : poolKey.databaseName().replaceAll("[^A-Za-z0-9_.-]", "_");
        config.setPoolName(hikariConfigProperties.getPoolName() + "-" + uniqueSuffix + "-" + databasePart);
        config.setRegisterMbeans(true);

        // 连接测试 - 根据数据库类型自动选择
        if (jdbcUrl.contains("mysql") || jdbcUrl.contains("postgresql")) {
            config.setConnectionTestQuery("SELECT 1");
        }

        // 性能优化配置
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        // MySQL 特定优化
        if (jdbcUrl.contains("mysql")) {
            config.addDataSourceProperty("useLocalSessionState", "true");
            config.addDataSourceProperty("rewriteBatchedStatements", "true");
            config.addDataSourceProperty("cacheResultSetMetadata", "true");
            config.addDataSourceProperty("cacheServerConfiguration", "true");
            config.addDataSourceProperty("elideSetAutoCommits", "true");
            config.addDataSourceProperty("maintainTimeStats", "false");
            config.setConnectionInitSql("SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci");
        }

        // PostgreSQL 特定优化
        if (jdbcUrl.contains("postgresql")) {
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepareThreshold", "5");
        }

        return new HikariDataSource(config);
    }
}
