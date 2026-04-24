package com.minicat.minicatserver.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "database_connections")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DatabaseConnectionEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(nullable = false, length = 255)
    private String host;
    
    @Column(nullable = false)
    private Integer port;
    
    @Column(nullable = false, length = 100)
    private String username;
    
    @Column(columnDefinition = "TEXT")
    private String password;
    
    @Column(nullable = false, length = 50)
    private String databaseType;
    
    @Column(length = 100)
    private String databaseName;
    
    @Column(length = 50)
    private String charset;
    
    @Column(nullable = false)
    private Boolean active = true;
    
    @Column(name = "connection_status", length = 20)
    private String connectionStatus; // success, failed, unknown
    
    @Column(name = "last_test_time")
    private LocalDateTime lastTestTime;
    
    @Column(name = "last_test_message", columnDefinition = "TEXT")
    private String lastTestMessage;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Version
    @Column(name = "version")
    private Long version;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
