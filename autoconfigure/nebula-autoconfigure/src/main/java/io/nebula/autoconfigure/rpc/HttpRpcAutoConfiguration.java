package io.nebula.autoconfigure.rpc;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nebula.rpc.core.client.RpcClient;
import io.nebula.rpc.http.client.HttpRpcClient;
import io.nebula.rpc.http.config.HttpRpcProperties;
import io.nebula.rpc.http.processor.RpcServiceRegistrationProcessor;
import io.nebula.rpc.http.server.HttpRpcController;
import io.nebula.rpc.http.server.HttpRpcServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * HTTP RPC 自动配置类
 * 必须在 RpcDiscoveryAutoConfiguration 之前初始化，提供 httpRpcClient Bean
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
@AutoConfiguration
@AutoConfigureBefore(RpcDiscoveryAutoConfiguration.class)  // 关键：确保 httpRpcClient 先创建
@ConditionalOnClass({RpcClient.class, RestTemplate.class})
@EnableConfigurationProperties(HttpRpcProperties.class)
@ConditionalOnProperty(prefix = "nebula.rpc.http", name = "enabled", havingValue = "true", matchIfMissing = true)
public class HttpRpcAutoConfiguration {
    
    /**
     * 配置 RestTemplateBuilder (如果不存在)
     * 某些场景下可能没有引入 spring-boot-starter-web，需要手动创建
     */
    @Bean
    @ConditionalOnMissingBean(RestTemplateBuilder.class)
    public RestTemplateBuilder restTemplateBuilder() {
        return new RestTemplateBuilder();
    }

    /**
     * 配置HTTP RPC专用的RestTemplate
     */
    @Bean(name = "rpcRestTemplate")
    @ConditionalOnMissingBean(name = "rpcRestTemplate")
    public RestTemplate rpcRestTemplate(HttpRpcProperties properties, 
                                       RestTemplateBuilder builder) {
        HttpRpcProperties.ClientConfig clientConfig = properties.getClient();
        
        RestTemplate restTemplate = builder
                .setConnectTimeout(Duration.ofMillis(clientConfig.getConnectTimeout()))
                .setReadTimeout(Duration.ofMillis(clientConfig.getReadTimeout()))
                .build();
        
        // 配置连接池
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(clientConfig.getConnectTimeout());
        factory.setReadTimeout(clientConfig.getReadTimeout());
        restTemplate.setRequestFactory(factory);
        
        log.info("配置HTTP RPC RestTemplate: connectTimeout={}ms, readTimeout={}ms",
                clientConfig.getConnectTimeout(), clientConfig.getReadTimeout());
        
        return restTemplate;
    }
    
    /**
     * 配置RPC客户端执行器
     */
    @Bean(name = "rpcExecutor")
    @ConditionalOnMissingBean(name = "rpcExecutor")
    public Executor rpcExecutor(HttpRpcProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("rpc-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        
        log.info("配置RPC执行器: corePoolSize=10, maxPoolSize=50, queueCapacity=200");
        
        return executor;
    }
    
    /**
     * 配置HTTP RPC客户端
     */
    @Bean(name = "httpRpcClient")
    @ConditionalOnMissingBean(name = "httpRpcClient")
    @ConditionalOnProperty(prefix = "nebula.rpc.http.client", name = "enabled", havingValue = "true", matchIfMissing = true)
    public HttpRpcClient httpRpcClient(RestTemplate rpcRestTemplate, 
                                       Executor rpcExecutor,
                                       HttpRpcProperties properties,
                                       ObjectMapper objectMapper) {
        String baseUrl = properties.getClient().getBaseUrl();
        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = "http://localhost:" + properties.getServer().getPort();
        }
        
        HttpRpcClient client = new HttpRpcClient(rpcRestTemplate, baseUrl, rpcExecutor, objectMapper);
        
        log.info("配置HTTP RPC客户端: baseUrl={}", baseUrl);
        
        return client;
    }
    
    /**
     * 配置HTTP RPC服务器
     */
    @Bean
    @ConditionalOnMissingBean(HttpRpcServer.class)
    @ConditionalOnProperty(prefix = "nebula.rpc.http.server", name = "enabled", havingValue = "true", matchIfMissing = true)
    public HttpRpcServer httpRpcServer(HttpRpcProperties properties) {
        HttpRpcServer server = new HttpRpcServer();
        server.start(properties.getServer().getPort());
        
        log.info("配置HTTP RPC服务器: port={}, contextPath={}", 
                properties.getServer().getPort(),
                properties.getServer().getContextPath());
        
        return server;
    }
    
    /**
     * 配置HTTP RPC控制器
     */
    @Bean
    @ConditionalOnMissingBean(HttpRpcController.class)
    @ConditionalOnProperty(prefix = "nebula.rpc.http.server", name = "enabled", havingValue = "true", matchIfMissing = true)
    public HttpRpcController httpRpcController(HttpRpcServer httpRpcServer, ObjectMapper objectMapper) {
        log.info("配置HTTP RPC控制器");
        return new HttpRpcController(httpRpcServer, objectMapper);
    }
    
    /**
     * 配置RPC服务注册处理器
     */
    @Bean
    @ConditionalOnProperty(prefix = "nebula.rpc.http.server", name = "enabled", havingValue = "true", matchIfMissing = true)
    public static RpcServiceRegistrationProcessor rpcServiceRegistrationProcessor(@Lazy HttpRpcServer httpRpcServer) {
        return new RpcServiceRegistrationProcessor(httpRpcServer);
    }
}

