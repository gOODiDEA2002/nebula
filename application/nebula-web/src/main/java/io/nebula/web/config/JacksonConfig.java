package io.nebula.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Jackson 配置类
 * <p>
 * 配置 Jackson ObjectMapper 以支持 Java 8 日期时间类型
 * 其他使用 nebula-web 的项目将自动继承此配置
 */
@Configuration
public class JacksonConfig {

    /**
     * 配置 Jackson ObjectMapper 以支持 Java 8 日期时间类型
     * <p>
     * 这个配置会被所有使用 nebula-web 的项目继承，无需重复配置
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.build();

        // 注册 JSR310 模块来支持 Java 8 日期时间类型
        // 支持 LocalDateTime, LocalDate, LocalTime, OffsetDateTime, ZonedDateTime 等
        objectMapper.registerModule(new JavaTimeModule());

        return objectMapper;
    }
}
