package io.nebula.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson 配置类
 * <p>
 * 配置 Jackson ObjectMapper 以支持 Java 8 日期时间类型
 * 其他使用 nebula-web 的项目将自动继承此配置
 */
@Configuration
public class JacksonConfig {

    /**
     * 定制 Jackson ObjectMapper 以支持 Java 8 日期时间类型
     * <p>
     * 使用 Jackson2ObjectMapperBuilderCustomizer 来定制 Spring Boot 自动配置的 ObjectMapper
     * 这样可以避免多个 @Primary ObjectMapper Bean 冲突
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            // 注册 JSR310 模块来支持 Java 8 日期时间类型
            // 支持 LocalDateTime, LocalDate, LocalTime, OffsetDateTime, ZonedDateTime 等
            builder.modules(new JavaTimeModule());
        };
    }
}
