package io.nebula.starter;

import io.nebula.core.config.ConfigurationValidator;
import jakarta.validation.Validator;
import io.nebula.core.metrics.DefaultMetricsCollector;
import io.nebula.core.metrics.MetricsCollector;
import io.nebula.core.metrics.aspect.MonitoringAspect;
import io.nebula.web.exception.GlobalExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
// 移除不存在的自动配置类引用

/**
 * Nebula 框架自动配置
 */
@AutoConfiguration
public class NebulaAutoConfiguration {
    
    /**
     * 配置验证器
     */
    @Bean
    @ConditionalOnMissingBean
    public ConfigurationValidator configurationValidator(Validator validator) {
        return new ConfigurationValidator(validator);
    }
    
    /**
     * 指标收集器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "io.micrometer.core.instrument.MeterRegistry")
    public MetricsCollector metricsCollector(io.micrometer.core.instrument.MeterRegistry meterRegistry) {
        return new DefaultMetricsCollector(meterRegistry);
    }
    
    /**
     * 监控切面
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "nebula.metrics.enabled", havingValue = "true", matchIfMissing = true)
    public MonitoringAspect monitoringAspect(MetricsCollector metricsCollector) {
        return new MonitoringAspect(metricsCollector);
    }
}
