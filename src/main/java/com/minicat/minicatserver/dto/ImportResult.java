package com.minicat.minicatserver.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ImportResult {
    private boolean success;
    private int totalRows;
    private int successRows;
    private int failedRows;
    private String message;
}
