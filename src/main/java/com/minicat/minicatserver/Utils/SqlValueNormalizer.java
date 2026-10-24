package com.minicat.minicatserver.Utils;

import com.minicat.minicatserver.exception.ValidationException;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Locale;
import java.util.Set;

/**
 * 把前端 JSON 传来的值按数据库列类型归一化为 JDBC 可绑定的 Java 值。
 *
 * <p>纯函数、无外部依赖，便于单元测试。日期时间同时兼容
 * {@code yyyy-MM-dd HH:mm:ss} 与 ISO 的 {@code yyyy-MM-ddTHH:mm:ss}。
 */
public final class SqlValueNormalizer {

    /** 暂不支持单元格编辑的列类型（二进制/JSON/空间类型等） */
    private static final Set<String> NON_EDITABLE_TYPES = Set.of(
            "BLOB", "TINYBLOB", "MEDIUMBLOB", "LONGBLOB",
            "BINARY", "VARBINARY", "BIT",
            "JSON", "XML",
            "GEOMETRY", "POINT", "LINESTRING", "POLYGON",
            "MULTIPOINT", "MULTILINESTRING", "MULTIPOLYGON", "GEOMETRYCOLLECTION");

    private SqlValueNormalizer() {
    }

    public static boolean isEditableType(String dataType) {
        if (dataType == null || dataType.isBlank()) {
            return true;
        }
        return !NON_EDITABLE_TYPES.contains(normalizeType(dataType));
    }

    /**
     * @param dataType 列的 TYPE_NAME（如 INT / VARCHAR / DATETIME）
     * @param value    前端 JSON 反序列化后的值（String / Number / Boolean / null）
     * @return 可直接交给 {@link java.sql.PreparedStatement#setObject(int, Object)} 的值
     */
    public static Object normalize(String dataType, Object value) {
        if (value == null) {
            return null;
        }
        String type = normalizeType(dataType);
        if (!isEditableType(type)) {
            throw new ValidationException("该列类型不支持编辑: " + dataType);
        }
        if (isBooleanType(type)) {
            return toBoolean(value);
        }
        if (isDateTimeType(type)) {
            return toTimestamp(value);
        }
        if (isDateType(type)) {
            return toDate(value);
        }
        if (isTimeType(type)) {
            return toTime(value);
        }
        if (isNumericType(type)) {
            return toBigDecimal(value);
        }
        return String.valueOf(value);
    }

    static String normalizeType(String dataType) {
        return dataType == null ? "" : dataType.trim().toUpperCase(Locale.ROOT);
    }

    static boolean isBooleanType(String type) {
        return "BOOLEAN".equals(type) || "BOOL".equals(type);
    }

    static boolean isDateTimeType(String type) {
        return "DATETIME".equals(type) || "TIMESTAMP".equals(type);
    }

    static boolean isDateType(String type) {
        return "DATE".equals(type);
    }

    static boolean isTimeType(String type) {
        return "TIME".equals(type);
    }

    static boolean isNumericType(String type) {
        return type.contains("INT")
                || type.contains("DECIMAL")
                || type.contains("NUMERIC")
                || type.contains("DOUBLE")
                || type.contains("FLOAT")
                || type.contains("REAL");
    }

    private static Boolean toBoolean(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        String text = String.valueOf(value).trim().toLowerCase(Locale.ROOT);
        if (text.equals("true") || text.equals("1") || text.equals("yes") || text.equals("y")) {
            return Boolean.TRUE;
        }
        if (text.equals("false") || text.equals("0") || text.equals("no") || text.equals("n")) {
            return Boolean.FALSE;
        }
        throw new ValidationException("布尔值不合法: " + value);
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal decimalValue) {
            return decimalValue;
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            throw new ValidationException("数值不能为空");
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            throw new ValidationException("数值格式不合法: " + value);
        }
    }

    private static Timestamp toTimestamp(Object value) {
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            throw new ValidationException("时间不能为空");
        }
        text = text.replace('T', ' ');
        if (text.endsWith("Z")) {
            text = text.substring(0, text.length() - 1);
        }
        if (text.length() == 10) {
            text = text + " 00:00:00";
        }
        try {
            return Timestamp.valueOf(text);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("时间格式不合法: " + value);
        }
    }

    private static Date toDate(Object value) {
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            throw new ValidationException("日期不能为空");
        }
        if (text.contains(" ")) {
            text = text.substring(0, text.indexOf(' '));
        }
        if (text.contains("T")) {
            text = text.substring(0, text.indexOf('T'));
        }
        try {
            return Date.valueOf(text);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("日期格式不合法: " + value);
        }
    }

    private static Time toTime(Object value) {
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            throw new ValidationException("时间不能为空");
        }
        if (text.contains(" ")) {
            text = text.substring(text.indexOf(' ') + 1);
        }
        if (text.contains("T")) {
            text = text.substring(text.indexOf('T') + 1);
        }
        int dotIndex = text.indexOf('.');
        if (dotIndex > 0) {
            text = text.substring(0, dotIndex);
        }
        if (text.length() == 5) {
            text = text + ":00";
        }
        try {
            return Time.valueOf(text);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("时间格式不合法: " + value);
        }
    }
}
