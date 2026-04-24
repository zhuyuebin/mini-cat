package com.minicat.minicatserver.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QueryResultDTO {
    private List<String> columns;
    private List<Map<String, Object>> rows;
    private Integer totalRows;
    private Long executionTime;
}
