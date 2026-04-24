package com.minicat.minicatserver.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableJpaRepositories(
    basePackages = "com.minicat.minicatserver.repository",
    entityManagerFactoryRef = "configEntityManagerFactory",
    transactionManagerRef = "configTransactionManager"
)
public class H2ConfigConfiguration {
    
    @Value("${spring.datasource.url:jdbc:h2:file:./data/minicat_db;AUTO_SERVER=TRUE}")
    private String datasourceUrl;
    
    @Value("${spring.datasource.username:sa}")
    private String datasourceUsername;
    
    @Value("${spring.datasource.password:}")
    private String datasourcePassword;
    
    @Bean(name = "configDataSource")
    public DataSource configDataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(datasourceUrl);
        dataSource.setUsername(datasourceUsername);
        dataSource.setPassword(datasourcePassword);
        dataSource.setDriverClassName("org.h2.Driver");
        
        // HikariCP 配置
        dataSource.setMaximumPoolSize(10);
        dataSource.setMinimumIdle(2);
        dataSource.setConnectionTimeout(30000);
        dataSource.setIdleTimeout(600000);
        dataSource.setMaxLifetime(1800000);
        dataSource.setPoolName("MiniCatConfigHikariPool");
        
        return dataSource;
    }
    
    @Bean(name = "configEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean configEntityManagerFactory() {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(configDataSource());
        em.setPackagesToScan("com.minicat.minicatserver.entity");
        
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        
        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.hbm2ddl.auto", "update");
        properties.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        properties.put("hibernate.show_sql", "false");
        properties.put("hibernate.format_sql", "true");
        
        em.setJpaPropertyMap(properties);
        
        return em;
    }
    
    @Bean(name = "configTransactionManager")
    public PlatformTransactionManager configTransactionManager() {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(configEntityManagerFactory().getObject());
        return transactionManager;
    }
}
