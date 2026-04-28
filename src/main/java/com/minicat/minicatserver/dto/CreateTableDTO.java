package com.minicat.minicatserver.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateTableDTO {
    private String tableName;
    private String tableComment;
    private List<ColumnDefinition> columns;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ColumnDefinition {
        private String columnName;
        private String dataType;
        private boolean nullable = true;
        private String defaultValue;
        private boolean primaryKey = false;
        private boolean autoIncrement = false;
        private Integer length;
        private String comment;
    }
}
