package com.minicat.minicatserver.ServiceImpls;

import com.minicat.minicatserver.Services.DatabaseService;
import com.minicat.minicatserver.Utils.ConnectionPoolManager;
import com.minicat.minicatserver.Utils.PasswordEncryptionService;
import com.minicat.minicatserver.Utils.SqlInjectionValidator;
import com.minicat.minicatserver.dto.QueryResultDTO;
import com.minicat.minicatserver.dto.TableInfoDTO;
import com.minicat.minicatserver.dto.ColumnInfoDTO;
import com.minicat.minicatserver.entity.DatabaseConnection;
import com.minicat.minicatserver.entity.DatabaseConnectionEntity;
import com.minicat.minicatserver.repository.DatabaseConnectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DatabaseServiceImpl implements DatabaseService {
    
    @Autowired
    private DatabaseConnectionRepository connectionRepository;
    
    @Autowired
    private ConnectionPoolManager connectionPoolManager;
    
    @Autowired
    private PasswordEncryptionService passwordEncryptionService;
    
    @Autowired
    private SqlInjectionValidator sqlInjectionValidator;

    // Entity to DTO conversion
    private DatabaseConnection convertToDTO(DatabaseConnectionEntity entity) {
        if (entity == null) return null;
        DatabaseConnection dto = new DatabaseConnection();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setHost(entity.getHost());
        dto.setPort(entity.getPort());
        dto.setUsername(entity.getUsername());
        dto.setPassword(entity.getPassword());
        dto.setDatabaseType(entity.getDatabaseType());
        dto.setDatabaseName(entity.getDatabaseName());
        dto.setCharset(entity.getCharset());
        dto.setActive(entity.getActive());
        dto.setConnectionStatus(entity.getConnectionStatus());
        if (entity.getLastTestTime() != null) {
            dto.setLastTestTime(entity.getLastTestTime().toString());
        }
        dto.setLastTestMessage(entity.getLastTestMessage());
        return dto;
    }

    // DTO to Entity conversion
    private DatabaseConnectionEntity convertToEntity(DatabaseConnection dto) {
        if (dto == null) return null;
        DatabaseConnectionEntity entity = new DatabaseConnectionEntity();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setHost(dto.getHost());
        entity.setPort(dto.getPort());
        entity.setUsername(dto.getUsername());
        entity.setPassword(dto.getPassword());
        entity.setDatabaseType(dto.getDatabaseType());
        entity.setDatabaseName(dto.getDatabaseName());
        entity.setCharset(dto.getCharset());
        entity.setActive(dto.getActive());
        return entity;
    }

    @Override
    public List<DatabaseConnection> getAllConnections() {
        return connectionRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public DatabaseConnection getConnectionById(String id) {
        return connectionRepository.findById(id)
                .map(this::convertToDTO)
                .orElse(null);
    }

    @Override
    public DatabaseConnection addConnection(DatabaseConnection connection) {
        // 参数验证
        if (connection == null) {
            throw new IllegalArgumentException("Connection cannot be null");
        }
        if (connection.getName() == null || connection.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Connection name is required");
        }
        if (connection.getHost() == null || connection.getHost().trim().isEmpty()) {
            throw new IllegalArgumentException("Host is required");
        }
        if (connection.getPort() == null || connection.getPort() <= 0) {
            throw new IllegalArgumentException("Valid port is required");
        }
        if (connection.getUsername() == null || connection.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (connection.getPassword() == null || connection.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }
        if (connection.getDatabaseType() == null || connection.getDatabaseType().trim().isEmpty()) {
            throw new IllegalArgumentException("Database type is required");
        }
        
        // 确保不设置 ID，让 JPA 自动生成
        connection.setId(null);
        connection.setActive(true);
        
        // 加密密码
        System.out.println("[DEBUG] Adding connection - Password before encryption: PLAIN TEXT");
        String encrypted = passwordEncryptionService.encrypt(connection.getPassword());
        connection.setPassword(encrypted);
        System.out.println("[DEBUG] Password encrypted successfully, length: " + encrypted.length());
        
        DatabaseConnectionEntity entity = convertToEntity(connection);
        DatabaseConnectionEntity savedEntity = connectionRepository.save(entity);
        return convertToDTO(savedEntity);
    }

    @Override
    public boolean updateConnection(String id, DatabaseConnection connection) {
        DatabaseConnectionEntity existingEntity = connectionRepository.findById(id).orElse(null);
        if (existingEntity == null) {
            return false;
        }
        
        // 更新现有实体的字段，而不是创建新实体
        existingEntity.setName(connection.getName());
        existingEntity.setHost(connection.getHost());
        existingEntity.setPort(connection.getPort());
        existingEntity.setUsername(connection.getUsername());
        existingEntity.setDatabaseType(connection.getDatabaseType());
        existingEntity.setDatabaseName(connection.getDatabaseName());
        existingEntity.setCharset(connection.getCharset());
        existingEntity.setActive(connection.getActive());
        
        // 如果密码为空或null,保留原有密码
        if (connection.getPassword() == null || connection.getPassword().isEmpty()) {
            System.out.println("[DEBUG] Updating connection - Password is empty, keeping existing encrypted password");
            // 不修改密码，保持原有的加密密码
        } else {
            // 如果密码与已加密的不同,说明是新密码,需要加密
            if (!connection.getPassword().equals(existingEntity.getPassword())) {
                System.out.println("[DEBUG] Updating connection - New password detected");
                if (!passwordEncryptionService.isEncrypted(connection.getPassword())) {
                    String encrypted = passwordEncryptionService.encrypt(connection.getPassword());
                    existingEntity.setPassword(encrypted);
                    System.out.println("[DEBUG] New password encrypted successfully, length: " + encrypted.length());
                } else {
                    System.out.println("[DEBUG] New password is already encrypted");
                    existingEntity.setPassword(connection.getPassword());
                }
            } else {
                System.out.println("[DEBUG] Updating connection - Password unchanged");
            }
        }
        
        connectionRepository.save(existingEntity);
        return true;
    }

    @Override
    public boolean deleteConnection(String id) {
        Optional<DatabaseConnectionEntity> optionalEntity = connectionRepository.findById(id);
        if (optionalEntity.isPresent()) {
            DatabaseConnectionEntity entity = optionalEntity.get();
            DatabaseConnection removed = convertToDTO(entity);
            
            // 关闭对应的连接池
            String poolKey = buildPoolKey(removed, removed.getDatabaseName());
            connectionPoolManager.closePool(poolKey);
            
            connectionRepository.deleteById(id);
            return true;
        }
        return false;
    }

    @Override
    public boolean testConnection(String id) {
        DatabaseConnection conn = getConnectionById(id);
        if (conn == null) {
            return false;
        }

        boolean success = false;
        String message = "";
        
        try (Connection c = createJdbcConnection(conn)) {
            success = c != null && !c.isClosed();
            if (success) {
                message = "Connection to " + conn.getHost() + ":" + conn.getPort() + " is valid";
                System.out.println("[Connection Test] SUCCESS - " + message);
            } else {
                message = "Connection is null or closed";
                System.out.println("[Connection Test] FAILED - " + message);
            }
        } catch (SQLException e) {
            message = "SQL Error: " + e.getMessage();
            System.err.println("[Connection Test] FAILED - " + message);
            e.printStackTrace();
        } catch (Exception e) {
            message = "Error: " + e.getMessage();
            System.err.println("[Connection Test] FAILED - " + message);
            e.printStackTrace();
        }
        
        // 保存测试结果到数据库
        saveConnectionTestResult(id, success ? "success" : "failed", message);
        
        return success;
    }
    
    private void saveConnectionTestResult(String id, String status, String message) {
        try {
            Optional<DatabaseConnectionEntity> optional = connectionRepository.findById(id);
            if (optional.isPresent()) {
                DatabaseConnectionEntity entity = optional.get();
                entity.setConnectionStatus(status);
                entity.setLastTestTime(LocalDateTime.now());
                entity.setLastTestMessage(message);
                connectionRepository.save(entity);
                System.out.println("[Connection Status] Saved - ID: " + id + ", Status: " + status);
            }
        } catch (Exception e) {
            System.err.println("[Connection Status] Failed to save test result: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public List<String> getDatabases(String connectionId) {
        DatabaseConnection conn = getConnectionById(connectionId);
        if (conn == null) {
            throw new RuntimeException("Connection not found");
        }

        List<String> databases = new ArrayList<>();
        String sql = "SHOW DATABASES";

        try (Connection c = createJdbcConnection(conn);
             Statement stmt = c.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                databases.add(rs.getString(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get databases: " + e.getMessage(), e);
        }

        return databases;
    }

    @Override
    public List<TableInfoDTO> getTables(String connectionId, String databaseName) {
        DatabaseConnection conn = getConnectionById(connectionId);
        if (conn == null) {
            throw new RuntimeException("Connection not found");
        }

        List<TableInfoDTO> tables = new ArrayList<>();

        try (Connection c = createJdbcConnection(conn, databaseName)) {
            DatabaseMetaData metaData = c.getMetaData();
            ResultSet rs = metaData.getTables(databaseName, null, "%", new String[]{"TABLE"});

            while (rs.next()) {
                TableInfoDTO tableInfo = new TableInfoDTO();
                String tableName = rs.getString("TABLE_NAME");
                tableInfo.setTableName(tableName);
                tableInfo.setTableComment(rs.getString("REMARKS"));
                
                // 查询表的行数
                try {
                    String countSql = "SELECT COUNT(*) FROM " + tableName;
                    try (Statement stmt = c.createStatement();
                         ResultSet countRs = stmt.executeQuery(countSql)) {
                        if (countRs.next()) {
                            tableInfo.setRowCount(countRs.getInt(1));
                        }
                    }
                } catch (SQLException e) {
                    // 如果查询失败，设置行数为0
                    System.err.println("Failed to get row count for table " + tableName + ": " + e.getMessage());
                    tableInfo.setRowCount(0);
                }
                
                tables.add(tableInfo);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get tables: " + e.getMessage(), e);
        }

        return tables;
    }

    @Override
    public List<ColumnInfoDTO> getColumns(String connectionId, String databaseName, String tableName) {
        DatabaseConnection conn = getConnectionById(connectionId);
        if (conn == null) {
            throw new RuntimeException("Connection not found");
        }

        List<ColumnInfoDTO> columns = new ArrayList<>();

        try (Connection c = createJdbcConnection(conn, databaseName)) {
            DatabaseMetaData metaData = c.getMetaData();
            ResultSet rs = metaData.getColumns(databaseName, null, tableName, "%");

            while (rs.next()) {
                ColumnInfoDTO columnInfo = new ColumnInfoDTO();
                columnInfo.setColumnName(rs.getString("COLUMN_NAME"));
                columnInfo.setDataType(rs.getString("TYPE_NAME"));
                columnInfo.setNullable(rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
                columnInfo.setDefaultValue(rs.getString("COLUMN_DEF"));
                columnInfo.setExtra(rs.getString("IS_AUTOINCREMENT"));
                columnInfo.setColumnComment(rs.getString("REMARKS"));
                columns.add(columnInfo);
            }

            // Get primary keys
            ResultSet pkRs = metaData.getPrimaryKeys(databaseName, null, tableName);
            Set<String> primaryKeys = new HashSet<>();
            while (pkRs.next()) {
                primaryKeys.add(pkRs.getString("COLUMN_NAME"));
            }

            // Mark primary keys
            for (ColumnInfoDTO col : columns) {
                if (primaryKeys.contains(col.getColumnName())) {
                    col.setColumnKey("PRI");
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to get columns: " + e.getMessage(), e);
        }

        return columns;
    }

    @Override
    public QueryResultDTO executeQuery(String connectionId, String databaseName, String sql) {
        DatabaseConnection conn = getConnectionById(connectionId);
        if (conn == null) {
            throw new RuntimeException("Connection not found");
        }

        // SQL注入验证
        SqlInjectionValidator.SqlValidationResult validation = sqlInjectionValidator.validate(sql);
        if (!validation.isValid()) {
            throw new RuntimeException("SQL验证失败: " + validation.getMessage());
        }

        QueryResultDTO result = new QueryResultDTO();
        long startTime = System.currentTimeMillis();

        try (Connection c = createJdbcConnection(conn, databaseName);
             Statement stmt = c.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            List<String> columns = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                columns.add(metaData.getColumnName(i));
            }
            result.setColumns(columns);

            List<Map<String, Object>> rows = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (String column : columns) {
                    row.put(column, rs.getObject(column));
                }
                rows.add(row);
            }
            result.setRows(rows);
            result.setTotalRows(rows.size());

        } catch (SQLException e) {
            throw new RuntimeException("Query execution failed: " + e.getMessage(), e);
        } finally {
            long endTime = System.currentTimeMillis();
            result.setExecutionTime(endTime - startTime);
        }

        return result;
    }

    @Override
    public int executeUpdate(String connectionId, String databaseName, String sql) {
        DatabaseConnection conn = getConnectionById(connectionId);
        if (conn == null) {
            throw new RuntimeException("Connection not found");
        }

        // 根据SQL类型选择不同的验证方法
        String upperSql = sql.trim().toUpperCase();
        SqlInjectionValidator.SqlValidationResult validation;
        
        if (upperSql.startsWith("INSERT")) {
            validation = sqlInjectionValidator.validateInsert(sql);
        } else if (upperSql.startsWith("UPDATE") || upperSql.startsWith("DELETE")) {
            validation = sqlInjectionValidator.validateUpdate(sql);
        } else {
            throw new RuntimeException("不支持的SQL操作类型，仅支持INSERT、UPDATE、DELETE");
        }
        
        if (!validation.isValid()) {
            throw new RuntimeException("SQL验证失败: " + validation.getMessage());
        }

        try (Connection c = createJdbcConnection(conn, databaseName);
             Statement stmt = c.createStatement()) {

            return stmt.executeUpdate(sql);

        } catch (SQLException e) {
            throw new RuntimeException("Update execution failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> getDatabaseInfo(String connectionId, String databaseName) {
        DatabaseConnection conn = getConnectionById(connectionId);
        if (conn == null) {
            throw new RuntimeException("Connection not found");
        }

        Map<String, Object> info = new HashMap<>();

        try (Connection c = createJdbcConnection(conn, databaseName)) {
            DatabaseMetaData metaData = c.getMetaData();

            info.put("databaseProduct", metaData.getDatabaseProductName());
            info.put("databaseVersion", metaData.getDatabaseProductVersion());
            info.put("driverName", metaData.getDriverName());
            info.put("driverVersion", metaData.getDriverVersion());
            info.put("url", metaData.getURL());

        } catch (SQLException e) {
            throw new RuntimeException("Failed to get database info: " + e.getMessage(), e);
        }

        return info;
    }

    @Override
    public Map<String, Object> getConnectionPoolStatus(String connectionId, String databaseName) {
        DatabaseConnection conn = getConnectionById(connectionId);
        if (conn == null) {
            throw new RuntimeException("Connection not found");
        }

        String poolKey = buildPoolKey(conn, databaseName);
        Map<String, Object> status = connectionPoolManager.getPoolStatus(poolKey);
        
        if (status == null) {
            Map<String, Object> emptyStatus = new HashMap<>();
            emptyStatus.put("message", "Connection pool not initialized yet");
            return emptyStatus;
        }
        
        return status;
    }

    private Connection createJdbcConnection(DatabaseConnection conn) throws SQLException {
        return createJdbcConnection(conn, conn.getDatabaseName());
    }

    private Connection createJdbcConnection(DatabaseConnection conn, String databaseName) throws SQLException {
        String jdbcUrl = buildJdbcUrl(conn, databaseName);
        String poolKey = buildPoolKey(conn, databaseName);
        
        // 解密密码
        String decryptedPassword = conn.getPassword();
        System.out.println("[DEBUG] Original password from connection: " + (decryptedPassword == null ? "NULL" : (decryptedPassword.isEmpty() ? "EMPTY" : "PRESENT (length=" + decryptedPassword.length() + ")")));
        
        if (decryptedPassword != null && !decryptedPassword.isEmpty()) {
            System.out.println("[DEBUG] Password is encrypted: " + passwordEncryptionService.isEncrypted(decryptedPassword));
            if (passwordEncryptionService.isEncrypted(decryptedPassword)) {
                try {
                    decryptedPassword = passwordEncryptionService.decrypt(decryptedPassword);
                    System.out.println("[DEBUG] Password decrypted successfully");
                } catch (Exception e) {
                    System.err.println("[ERROR] Failed to decrypt password: " + e.getMessage());
                    e.printStackTrace();
                    throw new SQLException("Failed to decrypt password: " + e.getMessage(), e);
                }
            } else {
                System.out.println("[DEBUG] Password is not encrypted, using as-is");
            }
        } else {
            System.err.println("[ERROR] Password is null or empty for connection: " + conn.getName() + " (" + conn.getHost() + ":" + conn.getPort() + ")");
        }
        
        System.out.println("[DEBUG] Final password to use: " + (decryptedPassword == null ? "NULL" : (decryptedPassword.isEmpty() ? "EMPTY" : "PRESENT")));
        return connectionPoolManager.getConnection(poolKey, jdbcUrl, conn.getUsername(), decryptedPassword);
    }

    private String buildPoolKey(DatabaseConnection conn, String databaseName) {
        return String.format("%s_%s_%d_%s", 
            conn.getDatabaseType().toLowerCase(),
            conn.getHost(), 
            conn.getPort(), 
            databaseName != null ? databaseName : "");
    }

    private String buildJdbcUrl(DatabaseConnection conn, String databaseName) {
        String host = conn.getHost();
        int port = conn.getPort();
        String dbType = conn.getDatabaseType().toLowerCase();
        String dbName = (databaseName != null && !databaseName.trim().isEmpty()) ? databaseName.trim() : "";

        return switch (dbType) {
            case "mysql" -> {
                if (dbName.isEmpty()) {
                    // 不指定数据库,连接到MySQL服务器
                    yield String.format("jdbc:mysql://%s:%d/?useUnicode=true&characterEncoding=utf8&connectionCollation=utf8mb4_unicode_ci&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
                            host, port);
                } else {
                    yield String.format("jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=utf8&connectionCollation=utf8mb4_unicode_ci&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
                            host, port, dbName);
                }
            }
            case "postgresql" -> {
                if (dbName.isEmpty()) {
                    // PostgreSQL必须指定数据库，默认为postgres
                    yield String.format("jdbc:postgresql://%s:%d/postgres", host, port);
                } else {
                    yield String.format("jdbc:postgresql://%s:%d/%s", host, port, dbName);
                }
            }
            default -> throw new IllegalArgumentException("Unsupported database type: " + dbType);
        };
    }
}
