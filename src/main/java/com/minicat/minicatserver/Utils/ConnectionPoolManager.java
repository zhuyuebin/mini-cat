package com.minicat.minicatserver.Utils;

import com.minicat.minicatserver.config.HikariConfigProperties;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ConnectionPoolManager {

    private final Map<String, HikariDataSource> connectionPools = new ConcurrentHashMap<>();
    
    @Autowired
    private HikariConfigProperties hikariConfigProperties;

    /**
     * 获取或创建连接池
     */
    public HikariDataSource getOrCreatePool(String poolKey, String jdbcUrl, String username, String password) {
        return connectionPools.computeIfAbsent(poolKey, key -> createConnectionPool(jdbcUrl, username, password));
    }

    /**
     * 从连接池获取连接
     */
    public Connection getConnection(String poolKey, String jdbcUrl, String username, String password) throws SQLException {
        HikariDataSource dataSource = getOrCreatePool(poolKey, jdbcUrl, username, password);
        return dataSource.getConnection();
    }

    /**
     * 关闭指定的连接池
     */
    public void closePool(String poolKey) {
        HikariDataSource dataSource = connectionPools.remove(poolKey);
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    /**
     * 关闭所有连接池
     */
    public void closeAllPools() {
        connectionPools.forEach((key, dataSource) -> {
            if (!dataSource.isClosed()) {
                dataSource.close();
            }
        });
        connectionPools.clear();
    }

    /**
     * 应用关闭时清理所有连接池
     */
    @PreDestroy
    public void destroy() {
        closeAllPools();
    }

    /**
     * 获取连接池状态信息
     */
    public Map<String, Object> getPoolStatus(String poolKey) {
        HikariDataSource dataSource = connectionPools.get(poolKey);
        if (dataSource == null) {
            return null;
        }

        Map<String, Object> status = new ConcurrentHashMap<>();
        status.put("poolName", dataSource.getPoolName());
        status.put("activeConnections", dataSource.getHikariPoolMXBean().getActiveConnections());
        status.put("idleConnections", dataSource.getHikariPoolMXBean().getIdleConnections());
        status.put("totalConnections", dataSource.getHikariPoolMXBean().getTotalConnections());
        status.put("threadsAwaitingConnection", dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection());
        status.put("maximumPoolSize", dataSource.getMaximumPoolSize());
        status.put("minimumIdle", dataSource.getMinimumIdle());

        return status;
    }

    /**
     * 创建新的连接池
     */
    private HikariDataSource createConnectionPool(String jdbcUrl, String username, String password) {
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
        config.setPoolName(hikariConfigProperties.getPoolName());
        
        // 泄漏检测
        config.setLeakDetectionThreshold(hikariConfigProperties.getLeakDetectionThreshold());
        
        // 连接测试 - 根据数据库类型自动选择
        if (jdbcUrl.contains("mysql")) {
            config.setConnectionTestQuery("SELECT 1");
        } else if (jdbcUrl.contains("postgresql")) {
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
            
            // 设置连接初始化SQL,确保使用utf8mb4字符集
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
