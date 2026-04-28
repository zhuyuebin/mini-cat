package com.minicat.minicatserver.Services;

import com.minicat.minicatserver.dto.QueryResultDTO;
import com.minicat.minicatserver.dto.TableInfoDTO;
import com.minicat.minicatserver.dto.ColumnInfoDTO;
import com.minicat.minicatserver.dto.CreateTableDTO;
import com.minicat.minicatserver.dto.ImportResult;
import com.minicat.minicatserver.entity.DatabaseConnection;

import java.util.List;
import java.util.Map;

public interface DatabaseService {

    List<DatabaseConnection> getAllConnections();

    DatabaseConnection getConnectionById(String id);

    DatabaseConnection addConnection(DatabaseConnection connection);

    boolean updateConnection(String id, DatabaseConnection connection);

    boolean deleteConnection(String id);

    boolean testConnection(String id);

    List<String> getDatabases(String connectionId);

    List<TableInfoDTO> getTables(String connectionId, String databaseName);

    List<ColumnInfoDTO> getColumns(String connectionId, String databaseName, String tableName);

    QueryResultDTO executeQuery(String connectionId, String databaseName, String sql);

    int executeUpdate(String connectionId, String databaseName, String sql);

    Map<String, Object> getDatabaseInfo(String connectionId, String databaseName);

    Map<String, Object> getConnectionPoolStatus(String connectionId, String databaseName);

    boolean createTable(String connectionId, String databaseName, CreateTableDTO createTableDTO);

    ImportResult importData(String connectionId, String databaseName, String tableName, org.springframework.web.multipart.MultipartFile file);

    boolean dropTable(String connectionId, String databaseName, String tableName);
}
