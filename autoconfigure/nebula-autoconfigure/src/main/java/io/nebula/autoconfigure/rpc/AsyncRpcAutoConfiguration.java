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
import org.springframework.context.annotation.Import;
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
@ConditionalOnClass(name = "io.nebula.rpc.async.execution.AsyncRpcExecutionManager")
@ConditionalOnProperty(prefix = "nebula.rpc.async", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import(NacosAsyncStorageAutoConfiguration.class)
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

        log.info("[AsyncRpc] 配置执行管理器: storage={}", storage.getClass().getSimpleName());
        return new AsyncRpcExecutionManager(storage, asyncRpcExecutor, objectMapper);
    }
}
