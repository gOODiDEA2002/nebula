package io.nebula.autoconfigure.rpc;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nebula.rpc.async.storage.AsyncExecutionStorage;
import io.nebula.rpc.async.storage.nacos.NacosAsyncExecutionStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Properties;

/**
 * Nacos异步存储自动配置
 * 
 * <p>独立配置类，仅在Nacos客户端存在时加载
 * 
 * @author Nebula Framework
 * @since 2.1.0
 */
@Slf4j
@Configuration
@ConditionalOnClass(name = "com.alibaba.nacos.api.config.ConfigService")
@ConditionalOnProperty(prefix = "nebula.rpc.async.storage", name = "type", havingValue = "nacos", matchIfMissing = true)
public class NacosAsyncStorageAutoConfiguration {

    /**
     * 配置Nacos存储
     */
    @Bean
    @ConditionalOnMissingBean(AsyncExecutionStorage.class)
    public AsyncExecutionStorage nacosAsyncExecutionStorage(
            AsyncRpcProperties properties,
            ObjectMapper objectMapper,
            Environment environment) {

        AsyncRpcProperties.NacosConfig nacosConfig = properties.getStorage().getNacos();
        String appName = environment.getProperty("spring.application.name", "default-app");

        try {
            // 从配置创建 ConfigService
            Properties nacosProps = new Properties();
            nacosProps.setProperty("serverAddr", nacosConfig.getServerAddr());
            nacosProps.setProperty("namespace", nacosConfig.getNamespace());

            if (nacosConfig.getUsername() != null && !nacosConfig.getUsername().isEmpty()) {
                nacosProps.setProperty("username", nacosConfig.getUsername());
            }
            if (nacosConfig.getPassword() != null && !nacosConfig.getPassword().isEmpty()) {
                nacosProps.setProperty("password", nacosConfig.getPassword());
            }

            ConfigService configService = NacosFactory.createConfigService(nacosProps);

            log.info("[AsyncRpc] 配置Nacos存储: serverAddr={}, namespace={}, appName={}",
                    nacosConfig.getServerAddr(), nacosConfig.getNamespace(), appName);

            return new NacosAsyncExecutionStorage(configService, objectMapper, appName);
        } catch (NacosException e) {
            log.error("[AsyncRpc] 创建Nacos ConfigService失败", e);
            throw new RuntimeException("创建Nacos ConfigService失败，请检查Nacos配置", e);
        }
    }
}
