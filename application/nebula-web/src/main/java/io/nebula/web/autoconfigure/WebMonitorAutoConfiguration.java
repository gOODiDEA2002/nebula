package io.nebula.web.autoconfigure;

import io.nebula.web.controller.HealthController;
import io.nebula.web.controller.PerformanceController;
import io.nebula.web.health.HealthCheckService;
import io.nebula.web.health.HealthChecker;
import io.nebula.web.health.checkers.ApplicationHealthChecker;
import io.nebula.web.health.checkers.DiskSpaceHealthChecker;
import io.nebula.web.health.checkers.MemoryHealthChecker;
import io.nebula.web.interceptor.PerformanceMonitorInterceptor;
import io.nebula.web.performance.DefaultPerformanceMonitor;
import io.nebula.web.performance.PerformanceMonitor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Web 监控自动配置：性能监控、健康检查
 */
@Configuration(proxyBeanMethods = false)
@Import({HealthController.class, PerformanceController.class})
class WebMonitorAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "nebula.web.performance.enabled", havingValue = "true")
    public PerformanceMonitor performanceMonitor(WebProperties webProperties) {
        WebProperties.Performance config = webProperties.getPerformance();
        return new DefaultPerformanceMonitor(config.getSlowRequestThreshold());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "nebula.web.performance.enabled", havingValue = "true")
    public WebMvcConfigurer performanceWebMvcConfigurer(PerformanceMonitor performanceMonitor,
                                                       WebProperties webProperties) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                String[] ignorePaths = {"/performance/**", "/actuator/**", "/swagger-ui/**", "/v3/api-docs/**"};
                registry.addInterceptor(new PerformanceMonitorInterceptor(performanceMonitor, ignorePaths))
                       .addPathPatterns("/**")
                       .order(0);
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(name = "applicationHealthChecker")
    @ConditionalOnProperty(name = "nebula.web.health.enabled", havingValue = "true", matchIfMissing = true)
    public HealthChecker applicationHealthChecker(ApplicationContext applicationContext) {
        return new ApplicationHealthChecker(applicationContext);
    }

    @Bean
    @ConditionalOnMissingBean(name = "memoryHealthChecker")
    @ConditionalOnProperty(name = "nebula.web.health.enabled", havingValue = "true", matchIfMissing = true)
    public HealthChecker memoryHealthChecker() {
        return new MemoryHealthChecker();
    }

    @Bean
    @ConditionalOnMissingBean(name = "diskSpaceHealthChecker")
    @ConditionalOnProperty(name = "nebula.web.health.enabled", havingValue = "true", matchIfMissing = true)
    public HealthChecker diskSpaceHealthChecker() {
        return new DiskSpaceHealthChecker();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "nebula.web.health.enabled", havingValue = "true", matchIfMissing = true)
    public HealthCheckService healthCheckService(List<HealthChecker> healthCheckers, WebProperties webProperties) {
        WebProperties.Health config = webProperties.getHealth();
        return new HealthCheckService(healthCheckers, config.isShowDetails());
    }
}
