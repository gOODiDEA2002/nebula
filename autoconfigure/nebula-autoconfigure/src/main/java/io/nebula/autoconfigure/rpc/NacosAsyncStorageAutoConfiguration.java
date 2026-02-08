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
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Properties;

/**
 * Nacos 异步存储自动配置
 * 
 * 配置复用策略：
 * - 如果显式配置了 nebula.rpc.async.storage.nacos.*，使用该配置
 * - 否则，自动复用 nebula.discovery.nacos.* 的配置
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
     * 配置 Nacos 存储
     * 
     * 自动复用服务发现的 Nacos 配置，减少重复配置
     */
    @Bean
    @ConditionalOnMissingBean(AsyncExecutionStorage.class)
    public AsyncExecutionStorage nacosAsyncExecutionStorage(
            AsyncRpcProperties properties,
            ObjectMapper objectMapper,
            Environment environment) {

        // 获取有效的 Nacos 配置（优先使用显式配置，否则复用服务发现配置）
        NacosConnectionConfig effectiveConfig = resolveNacosConfig(properties, environment);
        String appName = environment.getProperty("spring.application.name", "default-app");

        try {
            // 从配置创建 ConfigService
            Properties nacosProps = new Properties();
            nacosProps.setProperty("serverAddr", effectiveConfig.serverAddr());
            nacosProps.setProperty("namespace", effectiveConfig.namespace());

            if (effectiveConfig.username() != null && !effectiveConfig.username().isEmpty()) {
                nacosProps.setProperty("username", effectiveConfig.username());
            }
            if (effectiveConfig.password() != null && !effectiveConfig.password().isEmpty()) {
                nacosProps.setProperty("password", effectiveConfig.password());
            }

            ConfigService configService = NacosFactory.createConfigService(nacosProps);

            log.info("[AsyncRpc] 配置 Nacos 存储: serverAddr={}, namespace={}, appName={}, configSource={}",
                    effectiveConfig.serverAddr(), effectiveConfig.namespace(), appName, effectiveConfig.source());

            return new NacosAsyncExecutionStorage(configService, objectMapper, appName);
        } catch (NacosException e) {
            log.error("[AsyncRpc] 创建 Nacos ConfigService 失败", e);
            throw new RuntimeException("创建 Nacos ConfigService 失败，请检查 Nacos 配置", e);
        }
    }

    /**
     * 解析有效的 Nacos 配置
     * 
     * 优先级：
     * 1. nebula.rpc.async.storage.nacos.* 显式配置
     * 2. nebula.discovery.nacos.* 服务发现配置
     * 3. 默认值
     */
    private NacosConnectionConfig resolveNacosConfig(AsyncRpcProperties properties, Environment environment) {
        AsyncRpcProperties.NacosConfig asyncNacosConfig = properties.getStorage().getNacos();
        
        // 检查是否有显式配置 nebula.rpc.async.storage.nacos.server-addr
        String asyncServerAddr = environment.getProperty("nebula.rpc.async.storage.nacos.server-addr");
        
        if (asyncServerAddr != null && !asyncServerAddr.isEmpty()) {
            // 使用显式配置
            return new NacosConnectionConfig(
                    asyncNacosConfig.getServerAddr(),
                    resolveNamespace(asyncNacosConfig.getNamespace()),
                    asyncNacosConfig.getUsername(),
                    asyncNacosConfig.getPassword(),
                    "explicit (nebula.rpc.async.storage.nacos)"
            );
        }
        
        // 尝试复用服务发现配置
        String discoveryServerAddr = environment.getProperty("nebula.discovery.nacos.server-addr");
        if (discoveryServerAddr != null && !discoveryServerAddr.isEmpty()) {
            String namespace = environment.getProperty("nebula.discovery.nacos.namespace", "");
            String username = environment.getProperty("nebula.discovery.nacos.username", "nacos");
            String password = environment.getProperty("nebula.discovery.nacos.password", "nacos");
            
            return new NacosConnectionConfig(
                    discoveryServerAddr,
                    resolveNamespace(namespace),
                    username,
                    password,
                    "reused (nebula.discovery.nacos)"
            );
        }
        
        // 使用默认值
        return new NacosConnectionConfig(
                asyncNacosConfig.getServerAddr(),
                resolveNamespace(asyncNacosConfig.getNamespace()),
                asyncNacosConfig.getUsername(),
                asyncNacosConfig.getPassword(),
                "default"
        );
    }
    
    /**
     * 解析命名空间
     * "public" -> "" (Nacos 的 public 命名空间 ID 是空字符串)
     */
    private String resolveNamespace(String namespace) {
        if (namespace == null || "public".equalsIgnoreCase(namespace)) {
            return "";
        }
        return namespace;
    }
    
    /**
     * Nacos 连接配置记录
     */
    private record NacosConnectionConfig(
            String serverAddr,
            String namespace,
            String username,
            String password,
            String source
    ) {}
}
