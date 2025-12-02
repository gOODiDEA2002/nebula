package io.nebula.security.config;

import io.nebula.security.authorization.SecurityAspect;
import io.nebula.security.jwt.DefaultJwtService;
import io.nebula.security.jwt.JwtService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 安全自动配置
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
@Configuration
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
}

