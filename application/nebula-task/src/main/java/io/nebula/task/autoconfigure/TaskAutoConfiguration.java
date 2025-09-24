package io.nebula.task.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.nebula.task.core.TaskExecutor;
import io.nebula.task.core.TaskHandler;
import io.nebula.task.execution.TaskEngine;
import io.nebula.task.execution.TaskRegistry;
import io.nebula.task.xxljob.service.XxlJobRegistryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.util.StringUtils;

import jakarta.annotation.PreDestroy;
import java.util.Map;

/**
 * 任务自动配置类
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties({TaskProperties.class})
@ConditionalOnProperty(prefix = "nebula.task", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TaskAutoConfiguration {
    
    @Autowired
    private TaskProperties taskProperties;
    
    /**
     * 任务注册器
     */
    @Bean
    @ConditionalOnMissingBean
    public TaskRegistry taskRegistry() {
        return new TaskRegistry();
    }
    
    /**
     * 任务执行引擎
     */
    @Bean
    @ConditionalOnMissingBean
    public TaskEngine taskEngine() {
        return new TaskEngine();
    }
    
    /**
     * ObjectMapper
     */
    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
    
    
    /**
     * 任务执行器注册监听器
     */
    @Bean
    public TaskExecutorRegistryListener taskExecutorRegistryListener() {
        return new TaskExecutorRegistryListener();
    }
    
    /**
     * 任务执行器注册监听器实现
     */
    @Slf4j
    static class TaskExecutorRegistryListener implements ApplicationListener<ContextRefreshedEvent> {
        
        @Autowired
        private TaskRegistry taskRegistry;
        
        @Autowired(required = false)
        private XxlJobRegistryService xxlJobRegistryService;
        
        @Override
        public void onApplicationEvent(ContextRefreshedEvent event) {
            ApplicationContext applicationContext = event.getApplicationContext();
            
            // 自动注册所有 TaskExecutor Bean
            Map<String, TaskExecutor> executors = applicationContext.getBeansOfType(TaskExecutor.class);
            log.info("发现 {} 个任务执行器", executors.size());
            
            for (Map.Entry<String, TaskExecutor> entry : executors.entrySet()) {
                String beanName = entry.getKey();
                TaskExecutor executor = entry.getValue();
                
                try {
                    taskRegistry.registerExecutor(executor);
                    log.info("注册任务执行器: {} -> {}", beanName, executor.getExecutorName());
                } catch (Exception e) {
                    log.error("注册任务执行器失败: {} -> {}", beanName, executor.getExecutorName(), e);
                }
            }
            
            // 自动注册带有 @TaskHandler 注解的 Bean
            Map<String, Object> handlerBeans = applicationContext.getBeansWithAnnotation(TaskHandler.class);
            log.info("发现 {} 个任务处理器", handlerBeans.size());
            
            for (Map.Entry<String, Object> entry : handlerBeans.entrySet()) {
                String beanName = entry.getKey();
                Object handler = entry.getValue();
                
                if (handler instanceof TaskExecutor) {
                    // 已经通过 TaskExecutor 接口注册了
                    continue;
                }
                
                try {
                    // 为非 TaskExecutor 的处理器创建适配器
                    TaskExecutor executor = createExecutorAdapter(handler);
                    if (executor != null) {
                        taskRegistry.registerExecutor(executor);
                        log.info("注册任务处理器: {} -> {}", beanName, executor.getExecutorName());
                    }
                } catch (Exception e) {
                    log.error("注册任务处理器失败: {}", beanName, e);
                }
            }
            
            // 启动 XXL-JOB 注册服务
            if (xxlJobRegistryService != null) {
                xxlJobRegistryService.start();
            }
            
            log.info("任务模块初始化完成，共注册 {} 个执行器", taskRegistry.getExecutorCount());
        }
        
        /**
         * 为处理器创建执行器适配器
         */
        private TaskExecutor createExecutorAdapter(Object handler) {
            TaskHandler annotation = handler.getClass().getAnnotation(TaskHandler.class);
            if (annotation == null) {
                return null;
            }
            
            String handlerName = getHandlerName(annotation, handler.getClass());
            
            // 这里可以根据需要创建不同类型的适配器
            // 目前先返回 null，表示不支持
            log.warn("暂不支持为非 TaskExecutor 类型的处理器创建适配器: {}", handlerName);
            return null;
        }
        
        /**
         * 获取处理器名称
         */
        private String getHandlerName(TaskHandler annotation, Class<?> handlerClass) {
            String name = annotation.value();
            if (!StringUtils.hasText(name)) {
                name = annotation.name();
            }
            if (!StringUtils.hasText(name)) {
                name = handlerClass.getSimpleName();
            }
            return name;
        }
        
        /**
         * 应用关闭时的清理工作
         */
        @PreDestroy
        public void destroy() {
            if (xxlJobRegistryService != null) {
                xxlJobRegistryService.stop();
            }
            log.info("任务模块已关闭");
        }
    }
}
