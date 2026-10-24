package com.minicat.minicatserver.Utils;

import com.minicat.minicatserver.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 单元格值归一化测试：JSON 值 -> JDBC 绑定值。
 */
class SqlValueNormalizerTest {

    @Test
    void normalizesNumericValuesToBigDecimal() {
        assertEquals(new BigDecimal("42"), SqlValueNormalizer.normalize("INT", "42"));
        assertEquals(new BigDecimal("42.50"), SqlValueNormalizer.normalize("DECIMAL", "42.50"));
        assertEquals(new BigDecimal("-7"), SqlValueNormalizer.normalize("BIGINT", -7));
        assertThrows(ValidationException.class, () -> SqlValueNormalizer.normalize("INT", "abc"));
    }

    @Test
    void normalizesDateAndTimestampValues() {
        assertEquals(Date.valueOf("2026-10-24"), SqlValueNormalizer.normalize("DATE", "2026-10-24"));
        assertEquals(Timestamp.valueOf("2026-10-24 18:00:00"),
                SqlValueNormalizer.normalize("DATETIME", "2026-10-24T18:00:00"));
        assertEquals(Timestamp.valueOf("2026-10-24 00:00:00"),
                SqlValueNormalizer.normalize("TIMESTAMP", "2026-10-24"));
        assertThrows(ValidationException.class, () -> SqlValueNormalizer.normalize("DATETIME", "not-a-date"));
    }

    @Test
    void normalizesBooleanValues() {
        assertEquals(Boolean.TRUE, SqlValueNormalizer.normalize("BOOLEAN", "1"));
        assertEquals(Boolean.FALSE, SqlValueNormalizer.normalize("BOOL", "false"));
        assertThrows(ValidationException.class, () -> SqlValueNormalizer.normalize("BOOLEAN", "maybe"));
    }

    @Test
    void passesThroughStringsAndNulls() {
        assertNull(SqlValueNormalizer.normalize("VARCHAR", null));
        assertEquals("张三", SqlValueNormalizer.normalize("VARCHAR", "张三"));
        assertEquals("", SqlValueNormalizer.normalize("VARCHAR", ""));
    }

    @Test
    void rejectsBinaryJsonAndSpatialTypes() {
        assertFalse(SqlValueNormalizer.isEditableType("BLOB"));
        assertFalse(SqlValueNormalizer.isEditableType("JSON"));
        assertFalse(SqlValueNormalizer.isEditableType("BINARY"));
        assertFalse(SqlValueNormalizer.isEditableType("GEOMETRY"));
        assertTrue(SqlValueNormalizer.isEditableType("VARCHAR"));
        assertTrue(SqlValueNormalizer.isEditableType("DATETIME"));
        assertThrows(ValidationException.class, () -> SqlValueNormalizer.normalize("BLOB", "abc"));
    }
}
