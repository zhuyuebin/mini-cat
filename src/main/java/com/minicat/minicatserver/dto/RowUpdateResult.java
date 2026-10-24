package com.minicat.minicatserver.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 单行更新结果：受影响行数与更新后的整行数据。
 */
@Data
@NoArgsConstructor
public class RowUpdateResult {

    private int affectedRows;

    private Map<String, Object> row = new LinkedHashMap<>();
}
