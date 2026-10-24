package com.minicat.minicatserver.Utils;

import com.minicat.minicatserver.exception.ValidationException;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 单行更新 SQL 构建器。
 *
 * <p>表名/列名必须通过白名单校验后加反引号引用（MySQL 方言），
 * 所有值一律使用 {@code ?} 占位符，由 PreparedStatement 绑定，杜绝拼接注入。
 */
public final class RowUpdateSqlBuilder {

    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[A-Za-z0-9_$]+$");

    private RowUpdateSqlBuilder() {
    }

    public static boolean isIdentifier(String identifier) {
        return identifier != null && IDENTIFIER_PATTERN.matcher(identifier).matches();
    }

    public static String quoteIdentifier(String identifier) {
        if (!isIdentifier(identifier)) {
            throw new ValidationException("非法标识符: " + identifier);
        }
        return "`" + identifier + "`";
    }

    /**
     * UPDATE `table` SET `c1` = ?, ... WHERE `k1` = ? AND ... [AND `e1` = ? ...]
     */
    public static String buildUpdate(String table,
                                     List<String> changeColumns,
                                     List<String> identityColumns,
                                     List<String> expectedColumns) {
        return buildUpdate(table, changeColumns, identityColumns, expectedColumns, Set.of());
    }

    /**
     * 同 {@link #buildUpdate(String, List, List, List)}，但旧值为 NULL 的 CAS 列使用 {@code IS NULL}
     * （SQL 中 {@code col = NULL} 永远不成立，必须单独处理）。
     */
    public static String buildUpdate(String table,
                                     List<String> changeColumns,
                                     List<String> identityColumns,
                                     List<String> expectedColumns,
                                     Set<String> expectedNullColumns) {
        if (changeColumns == null || changeColumns.isEmpty()) {
            throw new ValidationException("没有需要修改的列");
        }
        if (identityColumns == null || identityColumns.isEmpty()) {
            throw new ValidationException("缺少行标识列");
        }

        StringBuilder sql = new StringBuilder("UPDATE ")
                .append(quoteIdentifier(table))
                .append(" SET ");
        for (int i = 0; i < changeColumns.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append(quoteIdentifier(changeColumns.get(i))).append(" = ?");
        }

        sql.append(" WHERE ");
        for (int i = 0; i < identityColumns.size(); i++) {
            if (i > 0) {
                sql.append(" AND ");
            }
            sql.append(quoteIdentifier(identityColumns.get(i))).append(" = ?");
        }

        if (expectedColumns != null) {
            for (String expectedColumn : expectedColumns) {
                if (expectedNullColumns != null && expectedNullColumns.contains(expectedColumn)) {
                    sql.append(" AND ").append(quoteIdentifier(expectedColumn)).append(" IS NULL");
                } else {
                    sql.append(" AND ").append(quoteIdentifier(expectedColumn)).append(" = ?");
                }
            }
        }
        return sql.toString();
    }

    public static String buildSelectByKeys(String table, List<String> identityColumns) {
        return "SELECT * FROM " + quoteIdentifier(table) + buildWhereByKeys(identityColumns);
    }

    public static String buildExistsByKeys(String table, List<String> identityColumns) {
        return "SELECT 1 FROM " + quoteIdentifier(table) + buildWhereByKeys(identityColumns);
    }

    private static String buildWhereByKeys(List<String> identityColumns) {
        if (identityColumns == null || identityColumns.isEmpty()) {
            throw new ValidationException("缺少行标识列");
        }
        StringBuilder where = new StringBuilder(" WHERE ");
        for (int i = 0; i < identityColumns.size(); i++) {
            if (i > 0) {
                where.append(" AND ");
            }
            where.append(quoteIdentifier(identityColumns.get(i))).append(" = ?");
        }
        return where.toString();
    }
}
