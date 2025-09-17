package io.nebula.example.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 示例应用配置（简化版本）
 */
@Slf4j
@Configuration
public class ExampleConfiguration implements ApplicationRunner {
    
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    
    @Bean
    public Executor taskExecutor() {
        return Executors.newFixedThreadPool(10);
    }
    
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Nebula示例应用启动完成！");
        log.info("应用已成功启动，可以通过 http://localhost:8080 访问");
    }
}
