package io.nebula.security.config;

import io.nebula.security.authorization.SecurityAspect;
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
}

