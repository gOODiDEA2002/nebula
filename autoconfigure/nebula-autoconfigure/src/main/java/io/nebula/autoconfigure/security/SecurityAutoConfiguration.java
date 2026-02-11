package io.nebula.autoconfigure.security;

import io.nebula.core.common.diagnostic.NebulaComponentSummary;
import io.nebula.core.common.diagnostic.SimpleComponentSummary;
import io.nebula.security.authorization.SecurityAspect;
import io.nebula.security.config.SecurityProperties;
import io.nebula.security.jwt.DefaultJwtService;
import io.nebula.security.jwt.JwtService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 安全自动配置
 * 
 * 条件加载：仅当 nebula-security 模块被引入时生效
 * 使用 @ConditionalOnClass(name = "...") 字符串形式，避免类不存在时抛出异常
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(name = {
        "io.nebula.security.jwt.JwtService",
        "io.nebula.security.config.SecurityProperties"
})
@ConditionalOnProperty(prefix = "nebula.security", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(SecurityProperties.class)
@EnableAspectJAutoProxy
public class SecurityAutoConfiguration {

    /**
     * 配置安全注解切面
     */
    @Bean
    @ConditionalOnMissingBean(SecurityAspect.class)
    public SecurityAspect securityAspect() {
        log.info("初始化Security注解切面");
        return new SecurityAspect();
    }

    /**
     * 配置JWT服务
     * 
     * 当启用JWT功能时自动创建JwtService Bean
     */
    @Bean
    @ConditionalOnMissingBean(JwtService.class)
    @ConditionalOnProperty(prefix = "nebula.security.jwt", name = "enabled", havingValue = "true", matchIfMissing = true)
    public JwtService jwtService(SecurityProperties properties) {
        log.info("初始化JWT服务");
        return new DefaultJwtService(properties);
    }

    /**
     * 组件摘要: 安全
     */
    @Bean
    NebulaComponentSummary securitySummary(SecurityProperties properties) {
        var details = new java.util.LinkedHashMap<String, String>();

        // JWT
        boolean jwtEnabled = properties.getJwt().isEnabled();
        details.put("JWT", jwtEnabled ? "ENABLED" : "DISABLED");
        if (jwtEnabled) {
            details.put("Token Prefix", properties.getJwt().getTokenPrefix());
            details.put("Header", properties.getJwt().getHeaderName());
        }

        // RBAC
        details.put("RBAC", properties.getRbac().isEnabled() ? "ENABLED" : "DISABLED");

        return new SimpleComponentSummary("Security", "Security", true, 700, details);
    }
}
