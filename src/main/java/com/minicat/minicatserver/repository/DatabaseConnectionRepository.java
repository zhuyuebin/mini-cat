package com.minicat.minicatserver.repository;

import com.minicat.minicatserver.entity.DatabaseConnectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DatabaseConnectionRepository extends JpaRepository<DatabaseConnectionEntity, String> {
    
    List<DatabaseConnectionEntity> findByActiveTrue();
    
    List<DatabaseConnectionEntity> findByDatabaseType(String databaseType);
}
