package com.minicat.minicatserver.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ColumnInfoDTO {
    private String columnName;
    private String dataType;
    private Boolean nullable;
    private String columnKey; // PRI, UNI, MUL
    private String defaultValue;
    private String extra;
    /** 是否为自增列（由 JDBC 元数据 IS_AUTOINCREMENT 归一化为布尔值，前端直接使用） */
    private Boolean autoIncrement;
    /**
     * 是否属于可唯一定位数据行的标识列：
     * 有主键时为主键列；无主键时为第一个「UNIQUE 且所有列 NOT NULL」索引的列。
     * 用于单元格编辑/按行删除时构造 WHERE 条件。
     */
    private Boolean rowIdentityPart;
    private String columnComment;
}