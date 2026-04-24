package com.minicat.minicatserver.Utils;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * SQL注入验证器 - 检测和防止SQL注入攻击
 */
@Component
public class SqlInjectionValidator {
    
    // 危险的SQL关键字和模式
    private static final List<String> DANGEROUS_PATTERNS = Arrays.asList(
        "/\\*", "\\*/",         // 多行注释
        "xp_",                   // SQL Server扩展存储过程
        "sp_",                   // SQL Server存储过程
        "exec\\(", "execute\\(",     // 执行命令
        "union\\s+select",       // UNION注入
        "union\\s+all\\s+select", // UNION ALL注入
        "insert\\s+into",        // INSERT注入
        "update\\s+.*\\bset\\b",       // UPDATE注入
        "drop\\s+table",         // DROP TABLE
        "drop\\s+database",      // DROP DATABASE
        "alter\\s+table",        // ALTER TABLE
        "create\\s+table",       // CREATE TABLE
        "grant\\s+",             // GRANT权限
        "revoke\\s+",            // REVOKE权限
        "load_file",             // MySQL文件读取
        "into\\s+outfile",       // MySQL文件写入
        "into\\s+dumpfile",      // MySQL文件写入
        "benchmark\\(",          // 时间盲注
        "sleep\\(",              // 时间盲注
        "waitfor\\s+delay",      // SQL Server时间盲注
        "information_schema",    // 信息模式访问
        "sys\\.tables",          // 系统表访问
        "pg_catalog",            // PostgreSQL系统目录
        "dbms_\\w+"              // Oracle DBMS函数
    );
    
    private static final Pattern[] DANGEROUS_PATTERNS_COMPILED;
    
    static {
        DANGEROUS_PATTERNS_COMPILED = new Pattern[DANGEROUS_PATTERNS.size()];
        for (int i = 0; i < DANGEROUS_PATTERNS.size(); i++) {
            DANGEROUS_PATTERNS_COMPILED[i] = Pattern.compile(
                DANGEROUS_PATTERNS.get(i), 
                Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
            );
        }
    }
    
    /**
     * 验证SQL是否安全
     * @param sql SQL语句
     * @return 验证结果
     */
    public SqlValidationResult validate(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return new SqlValidationResult(false, "SQL语句不能为空");
        }
        
        String trimmedSql = sql.trim();
        System.out.println("[SQL Validation] Checking SQL: " + trimmedSql.substring(0, Math.min(100, trimmedSql.length())));
        
        // 检查是否包含危险模式
        for (Pattern pattern : DANGEROUS_PATTERNS_COMPILED) {
            if (pattern.matcher(trimmedSql).find()) {
                System.out.println("[SQL Validation] FAILED - Dangerous pattern detected: " + pattern.pattern());
                return new SqlValidationResult(false, 
                    "检测到潜在的SQL注入攻击: 禁止使用危险SQL操作");
            }
        }
        
        // 只允许SELECT查询（用于executeQuery方法）
        String upperSql = trimmedSql.toUpperCase();
        if (!upperSql.startsWith("SELECT")) {
            System.out.println("[SQL Validation] FAILED - Not a SELECT statement. Starts with: " + upperSql.substring(0, Math.min(20, upperSql.length())));
            return new SqlValidationResult(false, 
                "只允许执行SELECT查询语句");
        }
        
        System.out.println("[SQL Validation] PASSED");
        return new SqlValidationResult(true, "SQL验证通过");
    }
    
    /**
     * 验证UPDATE/DELETE语句是否安全
     * @param sql SQL语句
     * @return 验证结果
     */
    public SqlValidationResult validateUpdate(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return new SqlValidationResult(false, "SQL语句不能为空");
        }
        
        String trimmedSql = sql.trim();
        String upperSql = trimmedSql.toUpperCase();
        
        // 只允许UPDATE和DELETE语句
        if (!upperSql.startsWith("UPDATE") && !upperSql.startsWith("DELETE")) {
            return new SqlValidationResult(false, 
                "只允许执行UPDATE或DELETE语句");
        }
        
        // 检查是否包含危险模式（排除update\s+.*set和delete\s+from，因为它们是合法的）
        List<String> updateDangerousPatterns = Arrays.asList(
            "/\\*", "\\*/",         // 多行注释
            "xp_",                   // SQL Server扩展存储过程
            "sp_",                   // SQL Server存储过程
            "exec\\(", "execute\\(",     // 执行命令
            "union\\s+select",       // UNION注入
            "union\\s+all\\s+select", // UNION ALL注入
            "drop\\s+table",         // DROP TABLE
            "drop\\s+database",      // DROP DATABASE
            "alter\\s+table",        // ALTER TABLE
            "create\\s+table",       // CREATE TABLE
            "grant\\s+",             // GRANT权限
            "revoke\\s+",            // REVOKE权限
            "load_file",             // MySQL文件读取
            "into\\s+outfile",       // MySQL文件写入
            "into\\s+dumpfile",      // MySQL文件写入
            "benchmark\\(",          // 时间盲注
            "sleep\\(",              // 时间盲注
            "waitfor\\s+delay",      // SQL Server时间盲注
            "information_schema",    // 信息模式访问
            "sys\\.tables",          // 系统表访问
            "pg_catalog",            // PostgreSQL系统目录
            "dbms_\\w+"              // Oracle DBMS函数
        );
        
        for (String patternStr : updateDangerousPatterns) {
            Pattern pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
            if (pattern.matcher(trimmedSql).find()) {
                return new SqlValidationResult(false, 
                    "检测到潜在的SQL注入攻击: 禁止使用危险SQL操作");
            }
        }
        
        // 警告：UPDATE/DELETE没有WHERE条件
        if ((upperSql.startsWith("UPDATE") && !upperSql.contains("WHERE")) ||
            (upperSql.startsWith("DELETE") && !upperSql.contains("WHERE"))) {
            return new SqlValidationResult(false, 
                "UPDATE/DELETE语句必须包含WHERE条件，以防止误操作");
        }
        
        return new SqlValidationResult(true, "SQL验证通过");
    }
    
    /**
     * 验证INSERT语句是否安全
     * @param sql SQL语句
     * @return 验证结果
     */
    public SqlValidationResult validateInsert(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return new SqlValidationResult(false, "SQL语句不能为空");
        }
        
        String trimmedSql = sql.trim();
        String upperSql = trimmedSql.toUpperCase();
        
        // 只允许INSERT语句
        if (!upperSql.startsWith("INSERT")) {
            return new SqlValidationResult(false, 
                "只允许执行INSERT语句");
        }
        
        // 检查是否包含危险模式
        List<String> insertDangerousPatterns = Arrays.asList(
            "/\\*", "\\*/",         // 多行注释
            "xp_",                   // SQL Server扩展存储过程
            "sp_",                   // SQL Server存储过程
            "exec\\(", "execute\\(",     // 执行命令
            "union\\s+select",       // UNION注入
            "union\\s+all\\s+select", // UNION ALL注入
            "drop\\s+table",         // DROP TABLE
            "drop\\s+database",      // DROP DATABASE
            "alter\\s+table",        // ALTER TABLE
            "create\\s+table",       // CREATE TABLE
            "grant\\s+",             // GRANT权限
            "revoke\\s+",            // REVOKE权限
            "load_file",             // MySQL文件读取
            "into\\s+outfile",       // MySQL文件写入
            "into\\s+dumpfile",      // MySQL文件写入
            "benchmark\\(",          // 时间盲注
            "sleep\\(",              // 时间盲注
            "waitfor\\s+delay",      // SQL Server时间盲注
            "information_schema",    // 信息模式访问
            "sys\\.tables",          // 系统表访问
            "pg_catalog",            // PostgreSQL系统目录
            "dbms_\\w+"              // Oracle DBMS函数
        );
        
        for (String patternStr : insertDangerousPatterns) {
            Pattern pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
            if (pattern.matcher(trimmedSql).find()) {
                return new SqlValidationResult(false, 
                    "检测到潜在的SQL注入攻击: 禁止使用危险SQL操作");
            }
        }
        
        return new SqlValidationResult(true, "SQL验证通过");
    }
    
    /**
     * SQL验证结果
     */
    public static class SqlValidationResult {
        private final boolean valid;
        private final String message;
        
        public SqlValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getMessage() {
            return message;
        }
    }
}
