package com.minicat.minicatserver.ServiceImpls;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 表数据分页参数的边界处理（纯单测）。
 */
class TableDataPaginationTest {

    @Test
    void pageIsClampedToOne() {
        assertEquals(1, DatabaseServiceImpl.clampPage(0));
        assertEquals(1, DatabaseServiceImpl.clampPage(-5));
        assertEquals(3, DatabaseServiceImpl.clampPage(3));
    }

    @Test
    void sizeIsClampedToAllowedRange() {
        assertEquals(50, DatabaseServiceImpl.clampSize(0), "缺省/非法 size 使用默认值 50");
        assertEquals(10, DatabaseServiceImpl.clampSize(5), "小于下限时提升到 10");
        assertEquals(50, DatabaseServiceImpl.clampSize(50));
        assertEquals(200, DatabaseServiceImpl.clampSize(999), "大于上限时压到 200");
    }
}
