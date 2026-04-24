package com.minicat.minicatserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class MiniCatApplication {

    public static void main(String[] args) {
        SpringApplication.run(MiniCatApplication.class, args);
    }

}
