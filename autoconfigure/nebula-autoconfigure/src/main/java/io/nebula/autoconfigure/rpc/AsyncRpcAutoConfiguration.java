package io.nebula.autoconfigure.rpc;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nebula.rpc.async.execution.AsyncRpcExecutionManager;
import io.nebula.rpc.async.storage.AsyncExecutionStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步RPC自动配置
 * 
 * @author Nebula Framework
 * @since 2.1.0
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(AsyncRpcProperties.class)
@ConditionalOnProperty(prefix = "nebula.rpc.async", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AsyncRpcAutoConfiguration {

    /**
     * 配置异步RPC执行器
     */
    @Bean(name = "asyncRpcExecutor")
    @ConditionalOnMissingBean(name = "asyncRpcExecutor")
    public Executor asyncRpcExecutor(AsyncRpcProperties properties) {
        AsyncRpcProperties.ExecutorConfig config = properties.getExecutor();

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(config.getCorePoolSize());
        executor.setMaxPoolSize(config.getMaxPoolSize());
        executor.setQueueCapacity(config.getQueueCapacity());
        executor.setThreadNamePrefix(config.getThreadNamePrefix());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();

        log.info("[AsyncRpc] 配置异步执行器: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                config.getCorePoolSize(), config.getMaxPoolSize(), config.getQueueCapacity());

        return executor;
    }

    /**
     * 配置Nacos存储（默认零配置）
     */
    @Bean
    @ConditionalOnMissingBean(AsyncExecutionStorage.class)
    @ConditionalOnClass(name = "com.alibaba.nacos.api.config.ConfigService")
    @ConditionalOnBean(com.alibaba.nacos.api.config.ConfigService.class)
    @ConditionalOnProperty(prefix = "nebula.rpc.async.storage", name = "type", havingValue = "nacos", matchIfMissing = true)
    public AsyncExecutionStorage nacosAsyncExecutionStorage(
            com.alibaba.nacos.api.config.ConfigService nacosConfigService,
            ObjectMapper objectMapper,
            org.springframework.core.env.Environment environment) {

        String appName = environment.getProperty("spring.application.name", "default-app");
        log.info("[AsyncRpc] 配置Nacos存储: appName={}", appName);

        return new io.nebula.rpc.async.storage.nacos.NacosAsyncExecutionStorage(
                nacosConfigService, objectMapper, appName);
    }

    /**
     * 配置异步RPC执行管理器
     * 只有当 AsyncExecutionStorage 可用时才创建
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(AsyncExecutionStorage.class)
    public AsyncRpcExecutionManager asyncRpcExecutionManager(
            AsyncExecutionStorage storage,
            Executor asyncRpcExecutor,
            ObjectMapper objectMapper) {

        log.info("[AsyncRpc] 配置执行管理器");
        return new AsyncRpcExecutionManager(storage, asyncRpcExecutor, objectMapper);
    }
}
