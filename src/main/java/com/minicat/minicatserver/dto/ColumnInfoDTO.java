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
    private String columnComment;
}