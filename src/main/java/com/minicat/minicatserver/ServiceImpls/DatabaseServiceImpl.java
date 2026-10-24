package com.minicat.minicatserver.ServiceImpls;

import com.minicat.minicatserver.Services.DatabaseService;
import com.minicat.minicatserver.Utils.ConnectionPoolManager;
import com.minicat.minicatserver.Utils.PasswordEncryptionService;
import com.minicat.minicatserver.Utils.SqlInjectionValidator;
import com.minicat.minicatserver.dto.*;
import com.minicat.minicatserver.entity.DatabaseConnection;
import com.minicat.minicatserver.entity.DatabaseConnectionEntity;
import com.minicat.minicatserver.repository.DatabaseConnectionRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import com.opencsv.CSVReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
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
        
        // 加密密码（只记录长度，不记录任何密码内容）
        String encrypted = passwordEncryptionService.encrypt(connection.getPassword());
        connection.setPassword(encrypted);
        log.debug("Connection created: name={}, type={}, host={}:{}, encryptedPasswordLength={}",
                connection.getName(), connection.getDatabaseType(), connection.getHost(),
                connection.getPort(), encrypted.length());
        
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
            log.debug("Connection update keeps existing password: id={}", id);
            // 不修改密码，保持原有的加密密码
        } else {
            // 如果密码与已加密的不同,说明是新密码,需要加密
            if (!connection.getPassword().equals(existingEntity.getPassword())) {
                if (!passwordEncryptionService.isEncrypted(connection.getPassword())) {
                    String encrypted = passwordEncryptionService.encrypt(connection.getPassword());
                    existingEntity.setPassword(encrypted);
                    log.debug("Connection password updated: id={}", id);
                } else {
                    log.debug("Connection update received already-encrypted password: id={}", id);
                    existingEntity.setPassword(connection.getPassword());
                }
            } else {
                log.debug("Connection password unchanged: id={}", id);
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
            log.info("Connection deleted: id={}, name={}", id, removed.getName());
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
                log.info("Connection test succeeded: name={}, target={}:{}",
                        conn.getName(), conn.getHost(), conn.getPort());
            } else {
                message = "Connection is null or closed";
                log.warn("Connection test failed (null/closed connection): name={}, target={}:{}",
                        conn.getName(), conn.getHost(), conn.getPort());
            }
        } catch (SQLException e) {
            message = "SQL Error: " + e.getMessage();
            log.error("Connection test failed (SQL error): name={}, target={}:{}",
                    conn.getName(), conn.getHost(), conn.getPort(), e);
        } catch (Exception e) {
            message = "Error: " + e.getMessage();
            log.error("Connection test failed: name={}, target={}:{}",
                    conn.getName(), conn.getHost(), conn.getPort(), e);
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
                log.debug("Connection test result saved: id={}, status={}", id, status);
            }
        } catch (Exception e) {
            log.error("Failed to save connection test result: id={}", id, e);
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
                    log.warn("Failed to count rows: connectionId={}, database={}, table={}",
                            connectionId, databaseName, tableName, e);
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
                // JDBC 规范中 IS_AUTOINCREMENT 的取值是 "YES"/"NO"，这里归一化成布尔值给前端，
                // 避免前端按 MySQL 方言的 "auto_increment" 字符串判断而失效。
                String autoIncrement = rs.getString("IS_AUTOINCREMENT");
                columnInfo.setExtra(autoIncrement);
                columnInfo.setAutoIncrement("YES".equalsIgnoreCase(autoIncrement));
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
            log.debug("Query executed: connectionId={}, database={}, rows={}, cost={}ms",
                    connectionId, databaseName, result.getTotalRows(), result.getExecutionTime());
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
        } else if (upperSql.startsWith("DROP TABLE")) {
            validation = sqlInjectionValidator.validateDropTable(sql);
        } else {
            throw new RuntimeException("不支持的SQL操作类型，仅支持INSERT、UPDATE、DELETE、DROP TABLE");
        }
        
        if (!validation.isValid()) {
            throw new RuntimeException("SQL验证失败: " + validation.getMessage());
        }

        try (Connection c = createJdbcConnection(conn, databaseName);
             Statement stmt = c.createStatement()) {

            int affectedRows = stmt.executeUpdate(sql);
            log.info("DML executed: connectionId={}, database={}, affectedRows={}",
                    connectionId, databaseName, affectedRows);
            return affectedRows;

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

    @Override
    public boolean createTable(String connectionId, String databaseName, CreateTableDTO createTableDTO) {
        DatabaseConnection conn = getConnectionById(connectionId);
        if (conn == null) {
            throw new RuntimeException("Connection not found");
        }

        if (createTableDTO == null || createTableDTO.getTableName() == null || createTableDTO.getTableName().trim().isEmpty()) {
            throw new RuntimeException("表名不能为空");
        }

        if (createTableDTO.getColumns() == null || createTableDTO.getColumns().isEmpty()) {
            throw new RuntimeException("至少需要定义一个列");
        }

        // 构建 CREATE TABLE SQL
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE ").append(createTableDTO.getTableName()).append(" (\n");

        List<CreateTableDTO.ColumnDefinition> columns = createTableDTO.getColumns();
        List<String> primaryKeys = new ArrayList<>();

        // 先收集所有主键列
        for (CreateTableDTO.ColumnDefinition col : columns) {
            if (col.isPrimaryKey()) {
                primaryKeys.add(col.getColumnName());
            }
        }

        // 构建列定义
        for (int i = 0; i < columns.size(); i++) {
            CreateTableDTO.ColumnDefinition col = columns.get(i);
            sql.append("  ").append(col.getColumnName()).append(" ");

            // 数据类型
            String dataType = col.getDataType().toUpperCase();
            if (col.getLength() != null && col.getLength() > 0) {
                sql.append(dataType).append("(").append(col.getLength()).append(")");
            } else {
                sql.append(dataType);
            }

            // 自增
            if (col.isAutoIncrement()) {
                sql.append(" AUTO_INCREMENT");
            }

            // 可空
            if (!col.isNullable()) {
                sql.append(" NOT NULL");
            }

            // 默认值
            if (col.getDefaultValue() != null && !col.getDefaultValue().isEmpty()) {
                sql.append(" DEFAULT ");
                // 如果是字符串类型，需要加引号
                if (dataType.contains("CHAR") || dataType.contains("TEXT")) {
                    sql.append("'").append(col.getDefaultValue()).append("'");
                } else {
                    sql.append(col.getDefaultValue());
                }
            }

            // 注释
            if (col.getComment() != null && !col.getComment().isEmpty()) {
                sql.append(" COMMENT '").append(col.getComment().replace("'", "''")).append("'");
            }

            // 添加逗号：如果不是最后一列，或者后面有主键约束，就添加逗号
            boolean isLastColumn = (i == columns.size() - 1);
            boolean hasPrimaryKeyConstraint = !primaryKeys.isEmpty();
            
            if (!isLastColumn || hasPrimaryKeyConstraint) {
                sql.append(",\n");
            } else {
                sql.append("\n");
            }
        }

        // 主键约束
        if (!primaryKeys.isEmpty()) {
            sql.append("  PRIMARY KEY (");
            for (int i = 0; i < primaryKeys.size(); i++) {
                sql.append(primaryKeys.get(i));
                if (i < primaryKeys.size() - 1) {
                    sql.append(", ");
                }
            }
            sql.append(")\n");
        }

        sql.append(")");

        // 表注释
        if (createTableDTO.getTableComment() != null && !createTableDTO.getTableComment().isEmpty()) {
            sql.append(" COMMENT='").append(createTableDTO.getTableComment().replace("'", "''")).append("'");
        }

        sql.append(";");

        String createSql = sql.toString();
        log.debug("Creating table: connectionId={}, database={}, sql={}", connectionId, databaseName, createSql);

        try (Connection c = createJdbcConnection(conn, databaseName);
             Statement stmt = c.createStatement()) {
            
            stmt.executeUpdate(createSql);
            log.info("Table created: connectionId={}, database={}, table={}",
                    connectionId, databaseName, createTableDTO.getTableName());
            return true;

        } catch (SQLException e) {
            log.error("Failed to create table: connectionId={}, database={}, table={}",
                    connectionId, databaseName, createTableDTO.getTableName(), e);
            throw new RuntimeException("创建表失败: " + e.getMessage(), e);
        }
    }

    @Override
    public ImportResult importData(String connectionId, String databaseName, String tableName, MultipartFile file) {
        DatabaseConnection conn = getConnectionById(connectionId);
        if (conn == null) {
            throw new RuntimeException("连接不存在");
        }

        ImportResult result = new ImportResult();
        result.setTotalRows(0);
        result.setSuccessRows(0);
        result.setFailedRows(0);
        result.setSuccess(false);

        try {
            // 1. 获取表结构
            List<ColumnInfoDTO> columns = getColumns(connectionId, databaseName, tableName);
            if (columns.isEmpty()) {
                throw new RuntimeException("表不存在或没有列定义");
            }

            Map<String, ColumnInfoDTO> columnMap = new LinkedHashMap<>();
            for (ColumnInfoDTO col : columns) {
                columnMap.put(col.getColumnName().toLowerCase(), col);
            }

            // 2. 解析文件
            List<String[]> rows = parseFile(file);
            if (rows.isEmpty()) {
                throw new RuntimeException("文件中没有数据");
            }

            // 3. 校验表头
            String[] headers = rows.get(0);
            List<String> missingColumns = new ArrayList<>();
            List<String> extraColumns = new ArrayList<>();
            Map<Integer, Integer> headerToColumnIndex = new HashMap<>(); // 文件列索引 -> 数据库列索引映射

            for (int i = 0; i < headers.length; i++) {
                String header = headers[i].trim();
                if (columnMap.containsKey(header.toLowerCase())) {
                    headerToColumnIndex.put(i, i); // 暂时用相同索引，后续需要调整
                } else {
                    extraColumns.add(header);
                }
            }

            for (String dbCol : columnMap.keySet()) {
                boolean found = false;
                for (String h : headers) {
                    if (h.trim().equalsIgnoreCase(dbCol)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    missingColumns.add(dbCol);
                }
            }

            if (!missingColumns.isEmpty() || !extraColumns.isEmpty()) {
                StringBuilder errorMsg = new StringBuilder("表头匹配失败：\n");
                if (!missingColumns.isEmpty()) {
                    errorMsg.append("缺失列: ").append(String.join(", ", missingColumns)).append("\n");
                }
                if (!extraColumns.isEmpty()) {
                    errorMsg.append("多余列: ").append(String.join(", ", extraColumns));
                }
                throw new RuntimeException(errorMsg.toString());
            }

            // 4. 批量插入数据
            result.setTotalRows(rows.size() - 1); // 减去表头行
            try (Connection c = createJdbcConnection(conn, databaseName)) {
                c.setAutoCommit(false);
                String insertSql = buildInsertSql(tableName, headers);
                
                try (PreparedStatement ps = c.prepareStatement(insertSql)) {
                    for (int i = 1; i < rows.size(); i++) {
                        String[] row = rows.get(i);
                        try {
                            setParameters(ps, row, headers, columnMap);
                            ps.addBatch();
                            result.setSuccessRows(result.getSuccessRows() + 1);
                        } catch (Exception e) {
                            result.setFailedRows(result.getFailedRows() + 1);
                            log.warn("Import row prepared failed: connectionId={}, table={}, row={}",
                                    connectionId, tableName, i, e);
                        }
                    }
                    ps.executeBatch();
                    c.commit();
                } catch (Exception e) {
                    c.rollback();
                    throw new RuntimeException("批量插入数据失败: " + e.getMessage(), e);
                }
            }

            result.setSuccess(true);
            result.setMessage("导入完成。总计: " + result.getTotalRows() + ", 成功: " + result.getSuccessRows() + ", 失败: " + result.getFailedRows());

        } catch (Exception e) {
            result.setMessage("导入失败: " + e.getMessage());
            throw new RuntimeException(e.getMessage(), e);
        }

        return result;
    }

    @Override
    public boolean dropTable(String connectionId, String databaseName, String tableName) {
        DatabaseConnection conn = getConnectionById(connectionId);
        if (conn == null) {
            throw new RuntimeException("连接不存在");
        }

        if (tableName == null || tableName.trim().isEmpty()) {
            throw new RuntimeException("表名不能为空");
        }

        // 构建 DROP TABLE SQL
        String dropSql = "DROP TABLE " + tableName;
        log.debug("Dropping table: connectionId={}, database={}, sql={}", connectionId, databaseName, dropSql);

        try (Connection c = createJdbcConnection(conn, databaseName);
             Statement stmt = c.createStatement()) {
            
            stmt.executeUpdate(dropSql);
            log.info("Table dropped: connectionId={}, database={}, table={}",
                    connectionId, databaseName, tableName);
            return true;

        } catch (SQLException e) {
            log.error("Failed to drop table: connectionId={}, database={}, table={}",
                    connectionId, databaseName, tableName, e);
            throw new RuntimeException("删除表失败: " + e.getMessage(), e);
        }
    }

    private List<String[]> parseFile(MultipartFile file) throws Exception {
        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            throw new RuntimeException("文件名不能为空");
        }

        if (fileName.endsWith(".csv")) {
            return parseCsv(file);
        } else if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
            return parseExcel(file);
        } else {
            throw new RuntimeException("不支持的文件格式，仅支持 .csv, .xlsx, .xls");
        }
    }

    private List<String[]> parseCsv(MultipartFile file) throws Exception {
        List<String[]> rows = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                rows.add(nextLine);
            }
        }
        return rows;
    }

    private List<String[]> parseExcel(MultipartFile file) throws Exception {
        List<String[]> rows = new ArrayList<>();
        Workbook workbook = null;
        try {
            String fileName = file.getOriginalFilename();
            if (fileName.endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(file.getInputStream());
            } else {
                workbook = new HSSFWorkbook(file.getInputStream());
            }

            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                String[] rowData = new String[row.getLastCellNum()];
                for (int i = 0; i < row.getLastCellNum(); i++) {
                    Cell cell = row.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    switch (cell.getCellType()) {
                        case STRING:
                            rowData[i] = cell.getStringCellValue();
                            break;
                        case NUMERIC:
                            if (DateUtil.isCellDateFormatted(cell)) {
                                rowData[i] = cell.getDateCellValue().toString();
                            } else {
                                rowData[i] = String.valueOf(cell.getNumericCellValue());
                            }
                            break;
                        case BOOLEAN:
                            rowData[i] = String.valueOf(cell.getBooleanCellValue());
                            break;
                        default:
                            rowData[i] = "";
                    }
                }
                rows.add(rowData);
            }
        } finally {
            if (workbook != null) {
                workbook.close();
            }
        }
        return rows;
    }

    private String buildInsertSql(String tableName, String[] headers) {
        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(tableName).append(" (");
        for (int i = 0; i < headers.length; i++) {
            sql.append(headers[i].trim());
            if (i < headers.length - 1) {
                sql.append(", ");
            }
        }
        sql.append(") VALUES (");
        for (int i = 0; i < headers.length; i++) {
            sql.append("?");
            if (i < headers.length - 1) {
                sql.append(", ");
            }
        }
        sql.append(")");
        return sql.toString();
    }

    private void setParameters(PreparedStatement ps, String[] row, String[] headers, Map<String, ColumnInfoDTO> columnMap) throws SQLException {
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i].trim();
            ColumnInfoDTO col = columnMap.get(header.toLowerCase());
            String value = (i < row.length) ? row[i] : null;

            if (value == null || value.trim().isEmpty()) {
                ps.setNull(i + 1, Types.VARCHAR);
            } else {
                // 根据数据类型设置参数
                String dataType = col.getDataType().toUpperCase();
                if (dataType.contains("INT") || dataType.contains("DECIMAL") || dataType.contains("DOUBLE") || dataType.contains("FLOAT")) {
                    try {
                        ps.setDouble(i + 1, Double.parseDouble(value.trim()));
                    } catch (NumberFormatException e) {
                        ps.setString(i + 1, value.trim());
                    }
                } else if (dataType.contains("DATE") || dataType.contains("TIME")) {
                    ps.setString(i + 1, value.trim());
                } else {
                    ps.setString(i + 1, value.trim());
                }
            }
        }
    }

    private Connection createJdbcConnection(DatabaseConnection conn) throws SQLException {
        return createJdbcConnection(conn, conn.getDatabaseName());
    }

    private Connection createJdbcConnection(DatabaseConnection conn, String databaseName) throws SQLException {
        String jdbcUrl = buildJdbcUrl(conn, databaseName);
        String poolKey = buildPoolKey(conn, databaseName);

        // 解密密码（绝不记录密码内容）
        String decryptedPassword = conn.getPassword();
        if (decryptedPassword != null && !decryptedPassword.isEmpty()) {
            if (passwordEncryptionService.isEncrypted(decryptedPassword)) {
                try {
                    decryptedPassword = passwordEncryptionService.decrypt(decryptedPassword);
                } catch (Exception e) {
                    log.error("Failed to decrypt password: connectionId={}, name={}",
                            conn.getId(), conn.getName(), e);
                    throw new SQLException("Failed to decrypt password: " + e.getMessage(), e);
                }
            }
        } else {
            log.error("Connection password is empty: connectionId={}, name={}, target={}:{}",
                    conn.getId(), conn.getName(), conn.getHost(), conn.getPort());
        }

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
