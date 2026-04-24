package com.minicat.minicatserver.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TableInfoDTO {
    private String tableName;
    private String tableComment;
    private Integer rowCount;
    private String createTime;
    private String updateTime;
}