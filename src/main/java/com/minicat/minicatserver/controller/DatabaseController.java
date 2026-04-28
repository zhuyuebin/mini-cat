package com.minicat.minicatserver.controller;

import com.minicat.minicatserver.Services.DatabaseService;
import com.minicat.minicatserver.dto.ApiResponseDTO;
import com.minicat.minicatserver.dto.QueryResultDTO;
import com.minicat.minicatserver.dto.TableInfoDTO;
import com.minicat.minicatserver.dto.ColumnInfoDTO;
import com.minicat.minicatserver.dto.CreateTableDTO;
import com.minicat.minicatserver.dto.ImportResult;
import com.minicat.minicatserver.entity.DatabaseConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/database")
@CrossOrigin(origins = "*")
public class DatabaseController {

    @Autowired
    private DatabaseService databaseService;

    @GetMapping("/connections")
    public ApiResponseDTO<List<DatabaseConnection>> getAllConnections() {
        List<DatabaseConnection> connections = databaseService.getAllConnections();
        // 移除密码字段，不返回给前端
        connections.forEach(conn -> conn.setPassword(null));
        return ApiResponseDTO.success(connections);
    }

    @GetMapping("/connections/{id}")
    public ApiResponseDTO<DatabaseConnection> getConnection(@PathVariable String id) {
        DatabaseConnection connection = databaseService.getConnectionById(id);
        if (connection == null) {
            return ApiResponseDTO.error("Connection not found");
        }
        // 移除密码字段，不返回给前端
        connection.setPassword(null);
        return ApiResponseDTO.success(connection);
    }

    @PostMapping("/connections")
    public ApiResponseDTO<DatabaseConnection> addConnection(@RequestBody DatabaseConnection connection) {
        DatabaseConnection saved = databaseService.addConnection(connection);
        return ApiResponseDTO.success(saved);
    }

    @PutMapping("/connections/{id}")
    public ApiResponseDTO<Void> updateConnection(@PathVariable String id,
                                                 @RequestBody DatabaseConnection connection) {
        boolean updated = databaseService.updateConnection(id, connection);
        if (!updated) {
            return ApiResponseDTO.error("Connection not found");
        }
        return ApiResponseDTO.success();
    }

    @DeleteMapping("/connections/{id}")
    public ApiResponseDTO<Void> deleteConnection(@PathVariable String id) {
        boolean deleted = databaseService.deleteConnection(id);
        if (!deleted) {
            return ApiResponseDTO.error("Connection not found");
        }
        return ApiResponseDTO.success();
    }

    @PostMapping("/connections/{id}/test")
    public ApiResponseDTO<Map<String, Object>> testConnection(@PathVariable String id) {
        DatabaseConnection conn = databaseService.getConnectionById(id);
        if (conn == null) {
            return ApiResponseDTO.error(404, "Connection not found");
        }
        
        Map<String, Object> result = new java.util.HashMap<>();
        try {
            boolean success = databaseService.testConnection(id);
            result.put("success", success);
            result.put("message", success ? "Connection successful" : "Connection failed");
            result.put("host", conn.getHost());
            result.put("port", conn.getPort());
            result.put("database", conn.getDatabaseName());
            
            if (!success) {
                return ApiResponseDTO.error(500, "Connection test failed. Check backend logs for details.");
            }
            return ApiResponseDTO.success(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Error: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            return ApiResponseDTO.error(500, "Connection test error: " + e.getMessage());
        }
    }

    @GetMapping("/connections/{id}/databases")
    public ApiResponseDTO<List<String>> getDatabases(@PathVariable String id) {
        return ApiResponseDTO.success(databaseService.getDatabases(id));
    }

    @GetMapping("/connections/{id}/tables")
    public ApiResponseDTO<List<TableInfoDTO>> getTables(@PathVariable String id,
                                                        @RequestParam String database) {
        return ApiResponseDTO.success(databaseService.getTables(id, database));
    }

    @GetMapping("/connections/{id}/columns")
    public ApiResponseDTO<List<ColumnInfoDTO>> getColumns(@PathVariable String id,
                                                          @RequestParam String database,
                                                          @RequestParam String table) {
        return ApiResponseDTO.success(databaseService.getColumns(id, database, table));
    }

    @PostMapping(value = "/connections/{id}/query", consumes = "text/plain")
    public ApiResponseDTO<QueryResultDTO> executeQuery(@PathVariable String id,
                                                       @RequestParam String database,
                                                       @RequestBody String sql) {
        System.out.println("[DEBUG] Received SQL query: " + sql);
        return ApiResponseDTO.success(databaseService.executeQuery(id, database, sql));
    }

    @PostMapping(value = "/connections/{id}/update", consumes = "text/plain")
    public ApiResponseDTO<Integer> executeUpdate(@PathVariable String id,
                                                 @RequestParam String database,
                                                 @RequestBody String sql) {
        System.out.println("[DEBUG] Received SQL update: " + sql);
        int affectedRows = databaseService.executeUpdate(id, database, sql);
        return ApiResponseDTO.success(affectedRows);
    }

    @GetMapping("/connections/{id}/info")
    public ApiResponseDTO<Map<String, Object>> getDatabaseInfo(@PathVariable String id,
                                                               @RequestParam String database) {
        return ApiResponseDTO.success(databaseService.getDatabaseInfo(id, database));
    }

    @GetMapping("/connections/{id}/pool-status")
    public ApiResponseDTO<Map<String, Object>> getConnectionPoolStatus(@PathVariable String id,
                                                                       @RequestParam String database) {
        return ApiResponseDTO.success(databaseService.getConnectionPoolStatus(id, database));
    }

    @PostMapping("/connections/{id}/create-table")
    public ApiResponseDTO<Boolean> createTable(@PathVariable String id,
                                               @RequestParam String database,
                                               @RequestBody CreateTableDTO createTableDTO) {
        boolean success = databaseService.createTable(id, database, createTableDTO);
        return ApiResponseDTO.success(success);
    }

    @PostMapping("/connections/{id}/import-data")
    public ApiResponseDTO<ImportResult> importData(@PathVariable String id,
                                                   @RequestParam String database,
                                                   @RequestParam String table,
                                                   @RequestParam("file") MultipartFile file) {
        ImportResult result = databaseService.importData(id, database, table, file);
        return ApiResponseDTO.success(result);
    }

    @DeleteMapping("/connections/{id}/tables")
    public ApiResponseDTO<Boolean> dropTable(@PathVariable String id,
                                             @RequestParam String database,
                                             @RequestParam String table) {
        boolean success = databaseService.dropTable(id, database, table);
        return ApiResponseDTO.success(success);
    }
}
