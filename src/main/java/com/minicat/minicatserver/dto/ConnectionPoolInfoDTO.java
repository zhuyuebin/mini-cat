package com.minicat.minicatserver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConnectionPoolInfoDTO {
    private String poolName;
    private Integer activeConnections;
    private Integer idleConnections;
    private Integer totalConnections;
    private Integer threadsAwaitingConnection;
    private Integer maximumPoolSize;
    private Integer minimumIdle;
    private Double utilizationRate; // 使用率百分比
}
