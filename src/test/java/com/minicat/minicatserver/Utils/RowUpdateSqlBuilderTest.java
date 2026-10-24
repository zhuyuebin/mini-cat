package com.minicat.minicatserver.Utils;

import com.minicat.minicatserver.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 单行更新 SQL 构建器测试：标识符白名单、占位符顺序、CAS 条件。
 */
class RowUpdateSqlBuilderTest {

    @Test
    void quotesValidIdentifiersWithBackticks() {
        assertEquals("`user`", RowUpdateSqlBuilder.quoteIdentifier("user"));
        assertEquals("`a_b$1`", RowUpdateSqlBuilder.quoteIdentifier("a_b$1"));
    }

    @Test
    void rejectsIllegalIdentifiers() {
        assertThrows(ValidationException.class, () -> RowUpdateSqlBuilder.quoteIdentifier("user;drop"));
        assertThrows(ValidationException.class, () -> RowUpdateSqlBuilder.quoteIdentifier("user name"));
        assertThrows(ValidationException.class, () -> RowUpdateSqlBuilder.quoteIdentifier(null));
        assertThrows(ValidationException.class, () -> RowUpdateSqlBuilder.quoteIdentifier("`user`"));
        assertFalse(RowUpdateSqlBuilder.isIdentifier(""));
    }

    @Test
    void buildsParameterizedUpdateWithExpectedCas() {
        String sql = RowUpdateSqlBuilder.buildUpdate(
                "user",
                List.of("username", "beizhu"),
                List.of("id"),
                List.of("username"));
        assertEquals("UPDATE `user` SET `username` = ?, `beizhu` = ? WHERE `id` = ? AND `username` = ?", sql);
    }

    @Test
    void buildsParameterizedUpdateWithoutExpected() {
        String sql = RowUpdateSqlBuilder.buildUpdate("user", List.of("username"), List.of("id"), List.of());
        assertEquals("UPDATE `user` SET `username` = ? WHERE `id` = ?", sql);
    }

    @Test
    void supportsCompositeIdentityColumns() {
        String sql = RowUpdateSqlBuilder.buildUpdate(
                "order_item",
                List.of("qty"),
                List.of("order_id", "line_no"),
                List.of());
        assertEquals("UPDATE `order_item` SET `qty` = ? WHERE `order_id` = ? AND `line_no` = ?", sql);
    }

    @Test
    void buildsSelectAndExistsByKeys() {
        assertEquals("SELECT * FROM `user` WHERE `id` = ?", RowUpdateSqlBuilder.buildSelectByKeys("user", List.of("id")));
        assertEquals("SELECT 1 FROM `user` WHERE `id` = ?", RowUpdateSqlBuilder.buildExistsByKeys("user", List.of("id")));
    }

    @Test
    void rejectsEmptyChangeOrIdentityColumns() {
        assertThrows(ValidationException.class,
                () -> RowUpdateSqlBuilder.buildUpdate("user", List.of(), List.of("id"), List.of()));
        assertThrows(ValidationException.class,
                () -> RowUpdateSqlBuilder.buildUpdate("user", List.of("name"), List.of(), List.of()));
    }
}
