package com.minicat.minicatserver.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 单行/单元格更新请求。
 *
 * <p>行定位：{@code keys} 必须覆盖服务端判定出的全部行标识列
 * （主键，或无主键时第一个 UNIQUE NOT NULL 索引的列）。
 * {@code expected} 可选：传入被修改列的旧值，用于乐观冲突检测（CAS）。
 */
@Data
@NoArgsConstructor
public class RowUpdateRequest {

    private String table;

    /** 行标识列 -> 值 */
    private Map<String, Object> keys = new LinkedHashMap<>();

    /** 列 -> 新值 */
    private Map<String, Object> changes = new LinkedHashMap<>();

    /** 可选：列 -> 旧值（只允许是 changes 中出现过的列） */
    private Map<String, Object> expected = new LinkedHashMap<>();
}
