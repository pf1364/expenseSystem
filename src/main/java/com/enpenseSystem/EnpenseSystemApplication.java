package com.enpenseSystem;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.enpenseSystem.mapper")
@SpringBootApplication
public class EnpenseSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnpenseSystemApplication.class, args);
    }
}
