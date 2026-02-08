package io.nebula.example.rpc.async.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 数据处理服务启动类
 * 
 * <p>作为RPC服务提供方，提供数据处理能力。
 * 
 * @author Nebula Framework
 */
@Slf4j
@SpringBootApplication
public class DataProcessServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataProcessServiceApplication.class, args);
        log.info("========================================");
        log.info("  Data Process Service 启动成功");
        log.info("  服务名: data-process-service");
        log.info("  端口: 8081");
        log.info("========================================");
    }
}
