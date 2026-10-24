package com.minicat.minicatserver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 表数据分页响应：一次返回当前页数据与总行数，前端分页器直接使用。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TableDataPageDTO {

    private List<String> columns;

    private List<Map<String, Object>> rows;

    private int page;

    private int size;

    private long total;
}
