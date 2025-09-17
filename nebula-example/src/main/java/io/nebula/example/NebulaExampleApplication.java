package io.nebula.example;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Nebula 框架示例应用
 */
@SpringBootApplication
@MapperScan("io.nebula.example.mapper")
public class NebulaExampleApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(NebulaExampleApplication.class, args);
    }
}
