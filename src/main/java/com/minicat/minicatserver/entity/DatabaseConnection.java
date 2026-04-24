package com.minicat.minicatserver.entity;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DatabaseConnection {
    private String id;
    private String name;
    private String host;
    private Integer port;
    private String username;
    private String password;
    private String databaseType; // mysql, postgresql, oracle, etc.
    private String databaseName;
    private String charset;
    private Boolean active;
    private String connectionStatus; // success, failed, unknown
    private String lastTestTime;
    private String lastTestMessage;
}