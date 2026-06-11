package com.enpenseSystem;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 差旅报销系统后端启动类。
 *
 * <p>MapperScan 扫描 MyBatis-Plus Mapper 接口，SpringBootApplication
 * 启动组件扫描、自动配置和内嵌 Web 容器。</p>
 */
@MapperScan("com.enpenseSystem.mapper")
@SpringBootApplication
public class EnpenseSystemApplication {

    /** 启动 Spring Boot 应用。 */
    public static void main(String[] args) {
        SpringApplication.run(EnpenseSystemApplication.class, args);
    }
}
