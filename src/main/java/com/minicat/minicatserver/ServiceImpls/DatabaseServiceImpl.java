package com.minicat.minicatserver.ServiceImpls;

import com.minicat.minicatserver.Services.DatabaseService;
import com.minicat.minicatserver.Utils.ConnectionPoolManager;
import com.minicat.minicatserver.Utils.PasswordEncryptionService;
import com.minicat.minicatserver.Utils.RowUpdateSqlBuilder;
import com.minicat.minicatserver.Utils.SqlInjectionValidator;
import com.minicat.minicatserver.Utils.SqlValueNormalizer;
import com.minicat.minicatserver.dto.*;
import com.minicat.minicatserver.entity.DatabaseConnection;
import com.minicat.minicatserver.entity.DatabaseConnectionEntity;
import com.minicat.minicatserver.exception.ConnectionNotFoundException;
import com.minicat.minicatserver.exception.DatabaseException;
import com.minicat.minicatserver.exception.QueryExecutionException;
import com.minicat.minicatserver.exception.RowConflictException;
import com.minicat.minicatserver.exception.RowNotFoundException;
import com.minicat.minicatserver.exception.SqlInjectionException;
import com.minicat.minicatserver.exception.ValidationException;
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
import java.util.regex.Pattern;
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

    private static final int TABLE_DATA_DEFAULT_SIZE = 50;
    private static final int TABLE_DATA_MIN_SIZE = 10;
    private static final int TABLE_DATA_MAX_SIZE = 200;
    /** 表名只允许常规标识符字符，防止分页 SQL 注入 */
    private static final Pattern TABLE_NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_$]+$");

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
            throw new ValidationException("连接信息不能为空");
        }
        if (connection.getName() == null || connection.getName().trim().isEmpty()) {
            throw new ValidationException("连接名称不能为空");
        }
        if (connection.getHost() == null || connection.getHost().trim().isEmpty()) {
            throw new ValidationException("主机地址不能为空");
        }
        if (connection.getPort() == null || connection.getPort() <= 0) {
            throw new ValidationException("端口号必须大于 0");
        }
        if (connection.getUsername() == null || connection.getUsername().trim().isEmpty()) {
            throw new ValidationException("用户名不能为空");
        }
        if (connection.getPassword() == null || connection.getPassword().trim().isEmpty()) {
            throw new ValidationException("密码不能为空");
        }
        if (connection.getDatabaseType() == null || connection.getDatabaseType().trim().isEmpty()) {
            throw new ValidationException("数据库类型不能为空");
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
        
        // 判断连接参数是否变化；变化后需要回收旧连接池，避免继续使用旧凭据
        boolean connectionSettingsChanged =
                !Objects.equals(existingEntity.getHost(), connection.getHost())
                || !Objects.equals(existingEntity.getPort(), connection.getPort())
                || !Objects.equals(existingEntity.getUsername(), connection.getUsername())
                || !Objects.equals(existingEntity.getDatabaseType(), connection.getDatabaseType())
                || !Objects.equals(existingEntity.getDatabaseName(), connection.getDatabaseName())
                || !Objects.equals(existingEntity.getCharset(), connection.getCharset());

        boolean passwordChanged = false;
        if (connection.getPassword() != null && !connection.getPassword().isEmpty()
                && !connection.getPassword().equals(existingEntity.getPassword())) {
            passwordChanged = true;
            if (!passwordEncryptionService.isEncrypted(connection.getPassword())) {
                existingEntity.setPassword(passwordEncryptionService.encrypt(connection.getPassword()));
                log.debug("Connection password updated: id={}", id);
            } else {
                existingEntity.setPassword(connection.getPassword());
                log.debug("Connection update received already-encrypted password: id={}", id);
            }
        }

        // 先关旧池，再写库：让后续请求不会取到旧凭据的连接
        if (connectionSettingsChanged || passwordChanged) {
            connectionPoolManager.closePoolsForConnection(id);
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

        connectionRepository.save(existingEntity);

        if (connectionSettingsChanged || passwordChanged) {
            // 保存后再关一次：覆盖"并发请求读取旧实体并重建旧池"的窗口
            connectionPoolManager.closePoolsForConnection(id);
            log.info("Connection pools recycled after update: id={}, settingsChanged={}, passwordChanged={}",
                    id, connectionSettingsChanged, passwordChanged);
        }
        return true;
    }

    @Override
    public boolean deleteConnection(String id) {
        Optional<DatabaseConnectionEntity> optionalEntity = connectionRepository.findById(id);
        if (optionalEntity.isPresent()) {
            DatabaseConnectionEntity entity = optionalEntity.get();
            DatabaseConnection removed = convertToDTO(entity);
            
            // 关闭该连接配置下的所有连接池（可能浏览过多个数据库）
            connectionPoolManager.closePoolsForConnection(id);

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
            throw new ConnectionNotFoundException(connectionId);
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
            throw new QueryExecutionException("获取数据库列表失败", e);
        }

        return databases;
    }

    @Override
    public List<TableInfoDTO> getTables(String connectionId, String databaseName) {
        DatabaseConnection conn = getConnectionById(connectionId);
        if (conn == null) {
            throw new ConnectionNotFoundException(connectionId);
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
            throw new QueryExecutionException("获取表列表失败", e);
        }

        return tables;
    }

    @Override
    public List<ColumnInfoDTO> getColumns(String connectionId, String databaseName, String tableName) {
        DatabaseConnection conn = getConnectionById(connectionId);
        if (conn == null) {
            throw new ConnectionNotFoundException(connectionId);
        }

        List<ColumnInfoDTO> columns = new ArrayList<>();

        try (Connection c = createJdbcConnection(conn, databaseName)) {
            DatabaseMetaData metaData = c.getMetaData();
            try (ResultSet rs = metaData.getColumns(databaseName, null, tableName, "%")) {
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
                    columnInfo.setRowIdentityPart(false);
                    columnInfo.setColumnComment(rs.getString("REMARKS"));
                    columns.add(columnInfo);
                }
            }

            // 主键列
            Set<String> primaryKeys = new HashSet<>();
            try (ResultSet pkRs = metaData.getPrimaryKeys(databaseName, null, tableName)) {
                while (pkRs.next()) {
                    primaryKeys.add(pkRs.getString("COLUMN_NAME"));
                }
            }

            // 行标识列：优先主键；无主键时使用第一个「UNIQUE 且所有列 NOT NULL」索引
            Set<String> rowIdentityColumns = findRowIdentityColumns(metaData, databaseName, tableName, columns, primaryKeys);

            for (ColumnInfoDTO column : columns) {
                String columnName = column.getColumnName();
                if (primaryKeys.contains(columnName)) {
                    column.setColumnKey("PRI");
                    column.setRowIdentityPart(true);
                } else if (rowIdentityColumns.contains(columnName)) {
                    column.setColumnKey("UNI");
                    column.setRowIdentityPart(true);
                } else {
                    column.setRowIdentityPart(false);
                }
            }

        } catch (SQLException e) {
            throw new QueryExecutionException("获取列信息失败", e);
        }

        return columns;
    }

    /**
     * 计算可唯一定位数据行的列：
     * 有主键 → 主键列；无主键 → 第一个「唯一索引且所有列 NOT NULL」的索引列（按索引列顺序）。
     */
    private Set<String> findRowIdentityColumns(DatabaseMetaData metaData,
                                               String catalog,
                                               String tableName,
                                               List<ColumnInfoDTO> columns,
                                               Set<String> primaryKeys) {
        if (!primaryKeys.isEmpty()) {
            return primaryKeys;
        }

        Map<String, Boolean> notNullByColumn = new HashMap<>();
        for (ColumnInfoDTO column : columns) {
            notNullByColumn.put(column.getColumnName(), Boolean.FALSE.equals(column.getNullable()));
        }

        Map<String, Map<Short, String>> uniqueIndexes = new LinkedHashMap<>();
        try (ResultSet rs = metaData.getIndexInfo(catalog, null, tableName, true, true)) {
            while (rs.next()) {
                String indexName = rs.getString("INDEX_NAME");
                String columnName = rs.getString("COLUMN_NAME");
                if (indexName == null || columnName == null) {
                    continue; // 统计信息行
                }
                if (rs.getBoolean("NON_UNIQUE")) {
                    continue;
                }
                short ordinal = rs.getShort("ORDINAL_POSITION");
                uniqueIndexes.computeIfAbsent(indexName, key -> new TreeMap<>()).put(ordinal, columnName);
            }
        } catch (SQLException e) {
            log.warn("Failed to read unique indexes: table={}", tableName, e);
            return Set.of();
        }

        for (Map<Short, String> indexColumns : uniqueIndexes.values()) {
            List<String> names = new ArrayList<>(indexColumns.values());
            boolean allNotNull = names.stream().allMatch(name -> Boolean.TRUE.equals(notNullByColumn.get(name)));
            if (allNotNull) {
                return new LinkedHashSet<>(names);
            }
        }
        return Set.of();
    }

    @Override
    public QueryResultDTO executeQuery(String connectionId, String databaseName, String sql) {
        DatabaseConnection conn = getConnectionById(connectionId);
        if (conn == null) {
            throw new ConnectionNotFoundException(connectionId);
        }

        // SQL注入验证
        SqlInjectionValidator.SqlValidationResult validation = sqlInjectionValidator.validate(sql);
        if (!validation.isValid()) {
            throw new SqlInjectionException(validation.getMessage());
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
            throw QueryExecutionException.fromSql(sql, e);
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
            throw new ConnectionNotFoundException(connectionId);
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
            throw new ValidationException("不支持的SQL操作类型，仅支持 INSERT、UPDATE、DELETE、DROP TABLE");
        }
        
        if (!validation.isValid()) {
            throw new SqlInjectionException(validation.getMessage());
        }

        try (Connection c = createJdbcConnection(conn, databaseName);
             Statement stmt = c.createStatement()) {

            int affectedRows = stmt.executeUpdate(sql);
            log.info("DML executed: connectionId={}, database={}, affectedRows={}",
                    connectionId, databaseName, affectedRows);
            return affectedRows;

        } catch (SQLException e) {
            throw QueryExecutionException.fromSql(sql, e);
        }
    }

    @Override
    public RowUpdateResult updateRow(String connectionId, String databaseName, RowUpdateRequest request) {
        DatabaseConnection conn = getConnectionById(connectionId);
        if (conn == null) {
            throw new ConnectionNotFoundException(connectionId);
        }
        if (request == null) {
            throw new ValidationException("更新请求不能为空");
        }
        if (databaseName == null || databaseName.isBlank()) {
            throw new ValidationException("数据库名不能为空");
        }
        if (!RowUpdateSqlBuilder.isIdentifier(request.getTable())) {
            throw new ValidationException("表名不合法");
        }
        if (request.getChanges() == null || request.getChanges().isEmpty()) {
            throw new ValidationException("没有需要修改的列");
        }
        if (request.getKeys() == null || request.getKeys().isEmpty()) {
            throw new ValidationException("缺少行标识值");
        }

        // 列元数据（含行标识列判定），同时校验表是否存在
        List<ColumnInfoDTO> columns = getColumns(connectionId, databaseName, request.getTable());
        if (columns.isEmpty()) {
            throw new ValidationException("表不存在或没有列定义");
        }
        Map<String, ColumnInfoDTO> columnMap = new LinkedHashMap<>();
        for (ColumnInfoDTO column : columns) {
            columnMap.put(column.getColumnName(), column);
        }

        List<String> identityColumns = new ArrayList<>();
        for (ColumnInfoDTO column : columns) {
            if (Boolean.TRUE.equals(column.getRowIdentityPart())) {
                identityColumns.add(column.getColumnName());
            }
        }
        if (identityColumns.isEmpty()) {
            throw new ValidationException("该表没有主键或唯一非空索引，无法安全定位数据行");
        }

        List<String> changeColumns = new ArrayList<>(request.getChanges().keySet());
        for (String columnName : changeColumns) {
            ColumnInfoDTO column = columnMap.get(columnName);
            if (column == null) {
                throw new ValidationException("列不存在: " + columnName);
            }
            if (Boolean.TRUE.equals(column.getRowIdentityPart())) {
                throw new ValidationException("行标识列不允许直接修改: " + columnName);
            }
            if (Boolean.TRUE.equals(column.getAutoIncrement())) {
                throw new ValidationException("自增列不允许直接修改: " + columnName);
            }
            if (!SqlValueNormalizer.isEditableType(column.getDataType())) {
                throw new ValidationException("该列类型不支持编辑: " + columnName + " (" + column.getDataType() + ")");
            }
        }

        for (String identityColumn : identityColumns) {
            if (!request.getKeys().containsKey(identityColumn)) {
                throw new ValidationException("缺少行标识列: " + identityColumn);
            }
        }
        for (String keyColumn : request.getKeys().keySet()) {
            if (!identityColumns.contains(keyColumn)) {
                throw new ValidationException("非法的行标识列: " + keyColumn);
            }
        }

        Map<String, Object> expected = request.getExpected() == null ? Map.of() : request.getExpected();
        List<String> expectedColumns = new ArrayList<>(expected.keySet());
        Set<String> expectedNullColumns = new LinkedHashSet<>();
        for (String expectedColumn : expectedColumns) {
            if (!request.getChanges().containsKey(expectedColumn)) {
                throw new ValidationException("expected 只能是本次修改的列: " + expectedColumn);
            }
            if (expected.get(expectedColumn) == null) {
                expectedNullColumns.add(expectedColumn);
            }
        }

        String sql = RowUpdateSqlBuilder.buildUpdate(
                request.getTable(), changeColumns, identityColumns, expectedColumns, expectedNullColumns);

        try (Connection c = createJdbcConnection(conn, databaseName)) {
            int affectedRows;
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                int index = 1;
                for (String columnName : changeColumns) {
                    index = bindValue(ps, index, columnMap.get(columnName), request.getChanges().get(columnName));
                }
                for (String identityColumn : identityColumns) {
                    index = bindValue(ps, index, columnMap.get(identityColumn), request.getKeys().get(identityColumn));
                }
                for (String expectedColumn : expectedColumns) {
                    if (expectedNullColumns.contains(expectedColumn)) {
                        continue; // IS NULL 不占位
                    }
                    index = bindValue(ps, index, columnMap.get(expectedColumn), expected.get(expectedColumn));
                }
                affectedRows = ps.executeUpdate();
            }

            if (affectedRows == 0) {
                // 区分「行不存在」与「乐观冲突」：0 行且带 expected 时，行还在说明是被别人改过
                boolean rowExists = existsByKeys(c, request.getTable(), identityColumns, request.getKeys(), columnMap);
                if (rowExists && !expectedColumns.isEmpty()) {
                    throw new RowConflictException("数据行已被修改，请刷新后重试");
                }
                throw new RowNotFoundException("数据行不存在或已被删除");
            }

            Map<String, Object> updatedRow = selectRowByKeys(c, request.getTable(), identityColumns, request.getKeys(), columnMap);
            log.info("Row updated: connectionId={}, database={}, table={}, identity={}, changed={}, affectedRows={}",
                    connectionId, databaseName, request.getTable(), identityColumns, changeColumns, affectedRows);

            RowUpdateResult result = new RowUpdateResult();
            result.setAffectedRows(affectedRows);
            result.setRow(updatedRow);
            return result;
        } catch (SQLException e) {
            throw QueryExecutionException.fromSql(sql, e);
        }
    }

    private int bindValue(PreparedStatement ps, int index, ColumnInfoDTO column, Object value) throws SQLException {
        Object normalized = SqlValueNormalizer.normalize(column == null ? null : column.getDataType(), value);
        ps.setObject(index, normalized);
        return index + 1;
    }

    private boolean existsByKeys(Connection connection,
                                 String table,
                                 List<String> identityColumns,
                                 Map<String, Object> keys,
                                 Map<String, ColumnInfoDTO> columnMap) throws SQLException {
        String sql = RowUpdateSqlBuilder.buildExistsByKeys(table, identityColumns);
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int index = 1;
            for (String identityColumn : identityColumns) {
                index = bindValue(ps, index, columnMap.get(identityColumn), keys.get(identityColumn));
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private Map<String, Object> selectRowByKeys(Connection connection,
                                                String table,
                                                List<String> identityColumns,
                                                Map<String, Object> keys,
                                                Map<String, ColumnInfoDTO> columnMap) throws SQLException {
        String sql = RowUpdateSqlBuilder.buildSelectByKeys(table, identityColumns);
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int index = 1;
            for (String identityColumn : identityColumns) {
                index = bindValue(ps, index, columnMap.get(identityColumn), keys.get(identityColumn));
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new RowNotFoundException("数据行不存在或已被删除");
                }
                ResultSetMetaData metaData = rs.getMetaData();
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= metaData.getColumnCount(); i++) {
                    row.put(metaData.getColumnLabel(i), rs.getObject(i));
                }
                return row;
            }
        }
    }

    @Override
    public Map<String, Object> getDatabaseInfo(String connectionId, String databaseName) {
        DatabaseConnection conn = getConnectionById(connectionId);
        if (conn == null) {
            throw new ConnectionNotFoundException(connectionId);
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
            throw new QueryExecutionException("获取数据库信息失败", e);
        }

        return info;
    }

    @Override
    public Map<String, Object> getConnectionPoolStatus(String connectionId, String databaseName) {
        DatabaseConnection conn = getConnectionById(connectionId);
        if (conn == null) {
            throw new ConnectionNotFoundException(connectionId);
        }

        ConnectionPoolManager.PoolKey poolKey = new ConnectionPoolManager.PoolKey(connectionId, databaseName);
        Map<String, Object> status = connectionPoolManager.getPoolStatus(poolKey);
        
        if (status == null) {
            Map<String, Object> emptyStatus = new HashMap<>();
            emptyStatus.put("message", "Connection pool not initialized yet");
            return emptyStatus;
        }
        
        return status;
    }

    @Override
    public TableDataPageDTO getTableDataPage(String connectionId, String databaseName,
                                             String tableName, int page, int size) {
        DatabaseConnection conn = getConnectionById(connectionId);
        if (conn == null) {
            throw new ConnectionNotFoundException(connectionId);
        }
        if (tableName == null || tableName.isBlank() || !TABLE_NAME_PATTERN.matcher(tableName).matches()) {
            throw new ValidationException("表名不合法: " + tableName);
        }

        int safePage = clampPage(page);
        int safeSize = clampSize(size);
        int offset = (safePage - 1) * safeSize;
        String quotedTable = "`" + tableName + "`";

        TableDataPageDTO result = new TableDataPageDTO();
        result.setPage(safePage);
        result.setSize(safeSize);

        try (Connection c = createJdbcConnection(conn, databaseName)) {
            // 总行数（供前端分页器使用；大表 COUNT(*) 会较慢，M2 可做缓存/估算）
            try (Statement stmt = c.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + quotedTable)) {
                if (rs.next()) {
                    result.setTotal(rs.getLong(1));
                }
            }

            List<String> columns = new ArrayList<>();
            List<Map<String, Object>> rows = new ArrayList<>();
            String pageSql = "SELECT * FROM " + quotedTable + " LIMIT " + safeSize + " OFFSET " + offset;
            try (Statement stmt = c.createStatement();
                 ResultSet rs = stmt.executeQuery(pageSql)) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                for (int i = 1; i <= columnCount; i++) {
                    columns.add(metaData.getColumnLabel(i));
                }
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (String column : columns) {
                        row.put(column, rs.getObject(column));
                    }
                    rows.add(row);
                }
            }

            result.setColumns(columns);
            result.setRows(rows);
            log.debug("Table data page loaded: connectionId={}, database={}, table={}, page={}, size={}, total={}",
                    connectionId, databaseName, tableName, safePage, safeSize, result.getTotal());
        } catch (SQLException e) {
            throw new QueryExecutionException("加载表数据失败: " + e.getMessage(), e);
        }

        return result;
    }

    static int clampPage(int page) {
        return Math.max(page, 1);
    }

    static int clampSize(int size) {
        if (size <= 0) {
            return TABLE_DATA_DEFAULT_SIZE;
        }
        return Math.min(Math.max(size, TABLE_DATA_MIN_SIZE), TABLE_DATA_MAX_SIZE);
    }

    @Override
    public boolean createTable(String connectionId, String databaseName, CreateTableDTO createTableDTO) {
        DatabaseConnection conn = getConnectionById(connectionId);
        if (conn == null) {
            throw new ConnectionNotFoundException(connectionId);
        }

        if (createTableDTO == null || createTableDTO.getTableName() == null || createTableDTO.getTableName().trim().isEmpty()) {
            throw new ValidationException("表名不能为空");
        }

        if (createTableDTO.getColumns() == null || createTableDTO.getColumns().isEmpty()) {
            throw new ValidationException("至少需要定义一个列");
        }

        // 列定义基础校验，避免拼接 SQL 时出现 NPE 或注入
        for (CreateTableDTO.ColumnDefinition col : createTableDTO.getColumns()) {
            if (col == null || col.getColumnName() == null || col.getColumnName().trim().isEmpty()) {
                throw new ValidationException("列名不能为空");
            }
            if (col.getDataType() == null || col.getDataType().trim().isEmpty()) {
                throw new ValidationException("列 " + col.getColumnName() + " 的数据类型不能为空");
            }
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
            throw new QueryExecutionException("创建表失败: " + e.getMessage(), e);
        }
    }

    @Override
    public ImportResult importData(String connectionId, String databaseName, String tableName, MultipartFile file) {
        DatabaseConnection conn = getConnectionById(connectionId);
        if (conn == null) {
            throw new ConnectionNotFoundException(connectionId);
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
                throw new ValidationException("表不存在或没有列定义");
            }

            Map<String, ColumnInfoDTO> columnMap = new LinkedHashMap<>();
            for (ColumnInfoDTO col : columns) {
                columnMap.put(col.getColumnName().toLowerCase(), col);
            }

            // 2. 解析文件
            List<String[]> rows = parseFile(file);
            if (rows.isEmpty()) {
                throw new ValidationException("文件中没有数据");
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
                throw new ValidationException(errorMsg.toString());
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
                    throw new QueryExecutionException("批量插入数据失败: " + e.getMessage(), e);
                }
            }

            result.setSuccess(true);
            result.setMessage("导入完成。总计: " + result.getTotalRows() + ", 成功: " + result.getSuccessRows() + ", 失败: " + result.getFailedRows());

        } catch (DatabaseException e) {
            result.setMessage("导入失败: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            result.setMessage("导入失败: " + e.getMessage());
            throw new QueryExecutionException("导入失败: " + e.getMessage(), e);
        }

        return result;
    }

    @Override
    public boolean dropTable(String connectionId, String databaseName, String tableName) {
        DatabaseConnection conn = getConnectionById(connectionId);
        if (conn == null) {
            throw new ConnectionNotFoundException(connectionId);
        }

        if (tableName == null || tableName.trim().isEmpty()) {
            throw new ValidationException("表名不能为空");
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
            throw new QueryExecutionException("删除表失败: " + e.getMessage(), e);
        }
    }

    private List<String[]> parseFile(MultipartFile file) throws Exception {
        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            throw new ValidationException("文件名不能为空");
        }

        if (fileName.endsWith(".csv")) {
            return parseCsv(file);
        } else if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
            return parseExcel(file);
        } else {
            throw new ValidationException("不支持的文件格式，仅支持 .csv, .xlsx, .xls");
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
        ConnectionPoolManager.PoolKey poolKey = new ConnectionPoolManager.PoolKey(conn.getId(), databaseName);

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
