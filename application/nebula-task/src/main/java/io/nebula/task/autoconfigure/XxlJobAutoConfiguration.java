package io.nebula.task.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nebula.task.xxljob.service.XxlJobRegistryService;
import io.nebula.task.xxljob.service.XxlJobTaskService;
import io.nebula.task.xxljob.util.XxlJobHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * XXL-JOB 自动配置类
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties({TaskProperties.class})
@ConditionalOnProperty(prefix = "nebula.task.xxl-job", name = "admin-addresses")
public class XxlJobAutoConfiguration {
    
    /**
     * XXL-JOB HTTP 客户端
     */
    @Bean
    @ConditionalOnMissingBean
    public XxlJobHttpClient xxlJobHttpClient(ObjectMapper objectMapper) {
        return new XxlJobHttpClient(objectMapper);
    }
    
    /**
     * XXL-JOB 任务服务
     */
    @Bean
    @ConditionalOnMissingBean
    public XxlJobTaskService xxlJobTaskService() {
        return new XxlJobTaskService();
    }
    
    /**
     * XXL-JOB 注册服务
     */
    @Bean
    @ConditionalOnMissingBean
    public XxlJobRegistryService xxlJobRegistryService() {
        return new XxlJobRegistryService();
    }
}
