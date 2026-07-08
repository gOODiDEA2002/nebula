package io.nebula.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.jackson2.autoconfigure.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson 配置类
 * <p>
 * 配置 Jackson ObjectMapper 以支持 Java 8 日期时间类型。
 * 其他使用 nebula-web 的项目将自动继承此配置。
 * <p>
 * <b>SB4.1 注意事项</b>: 本定制通过 {@link Jackson2ObjectMapperBuilderCustomizer} 作用于 Jackson 2 的 ObjectMapper。
 * Spring Boot 4.1 默认 MVC 消息转换器优先使用 Jackson 3, 会绕过本定制导致脱敏({@code @SensitiveData})等功能静默失效。
 * 需配置 {@code spring.http.converters.preferred-json-mapper=jackson2} 方能生效。
 * Nebula 的 web/all/mcp starter 已通过 {@code nebula-defaults.properties} 自动注入该配置;
 * 裸依赖 nebula-web 的应用需自行在 application.yml 中配置。
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
