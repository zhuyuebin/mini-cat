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
    private String columnComment;
}