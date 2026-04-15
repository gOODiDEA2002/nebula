package io.nebula.autoconfigure.rpc;

import io.nebula.core.common.diagnostic.NebulaComponentSummary;
import io.nebula.core.common.diagnostic.SimpleComponentSummary;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestClient;

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
@AutoConfigureBefore(RpcDiscoveryAutoConfiguration.class) // 关键：确保 httpRpcClient 先创建
@ConditionalOnClass(name = { "io.nebula.rpc.http.config.HttpRpcProperties", "io.nebula.rpc.http.client.HttpRpcClient" })
@EnableConfigurationProperties(HttpRpcProperties.class)
@ConditionalOnProperty(prefix = "nebula.rpc.http", name = "enabled", havingValue = "true", matchIfMissing = false)
public class HttpRpcAutoConfiguration {

    /**
     * 配置HTTP RPC专用的RestClient（替代 RestTemplate）
     */
    @Bean(name = "rpcRestClient")
    @ConditionalOnMissingBean(name = "rpcRestClient")
    public RestClient rpcRestClient(HttpRpcProperties properties) {
        HttpRpcProperties.ClientConfig clientConfig = properties.getClient();

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(clientConfig.getConnectTimeout());
        factory.setReadTimeout(clientConfig.getReadTimeout());

        log.info("配置HTTP RPC RestClient: connectTimeout={}ms, readTimeout={}ms",
                clientConfig.getConnectTimeout(), clientConfig.getReadTimeout());

        return RestClient.builder()
                .requestFactory(factory)
                .build();
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
     * 
     * 注意：当 baseUrl 未配置时，使用 Spring 的 server.port 作为默认端口，
     * 确保与应用实际监听端口一致
     */
    @Bean(name = "httpRpcClient")
    @ConditionalOnMissingBean(name = "httpRpcClient")
    @ConditionalOnProperty(prefix = "nebula.rpc.http.client", name = "enabled", havingValue = "true", matchIfMissing = true)
    public HttpRpcClient httpRpcClient(RestClient rpcRestClient,
            Executor rpcExecutor,
            HttpRpcProperties properties,
            ObjectMapper objectMapper,
            @org.springframework.beans.factory.annotation.Value("${server.port:8080}") int serverPort) {
        String baseUrl = properties.getClient().getBaseUrl();
        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = "http://localhost:" + serverPort;
        }

        HttpRpcClient client = new HttpRpcClient(rpcRestClient, baseUrl, rpcExecutor, objectMapper);

        log.info("配置HTTP RPC客户端: baseUrl={}", baseUrl);

        return client;
    }

    /**
     * 配置HTTP RPC服务器
     * 
     * 当 nebula.rpc.http.server.port 未配置时，自动使用 Spring Boot 的 server.port
     */
    @Bean
    @ConditionalOnMissingBean(HttpRpcServer.class)
    @ConditionalOnProperty(prefix = "nebula.rpc.http.server", name = "enabled", havingValue = "true", matchIfMissing = true)
    public HttpRpcServer httpRpcServer(HttpRpcProperties properties,
            @org.springframework.beans.factory.annotation.Value("${server.port:8080}") int serverPort) {
        int resolvedPort = properties.getServer().getPort() > 0
                ? properties.getServer().getPort()
                : serverPort;

        HttpRpcServer server = new HttpRpcServer();
        server.start(resolvedPort);

        log.info("配置HTTP RPC服务器: port={}, contextPath={}",
                resolvedPort,
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

    /**
     * 组件摘要: HTTP RPC
     */
    @Bean
    NebulaComponentSummary httpRpcSummary(HttpRpcProperties properties,
            org.springframework.core.env.Environment env) {
        var details = new java.util.LinkedHashMap<String, String>();

        // Server Info
        Integer port = properties.getServer().getPort();
        if (port == null) {
            port = env.getProperty("server.port", Integer.class, 8080);
        }
        details.put("Server Port", String.valueOf(port));
        details.put("Context Path", properties.getServer().getContextPath());
        details.put("Request Timeout", properties.getServer().getRequestTimeout() + "ms");

        // Client Info
        details.put("Connect Timeout", properties.getClient().getConnectTimeout() + "ms");
        details.put("Read Timeout", properties.getClient().getReadTimeout() + "ms");
        details.put("Max Connections", String.valueOf(properties.getClient().getMaxConnections()));
        details.put("Retry Count", String.valueOf(properties.getClient().getRetryCount()));
        details.put("Compression", String.valueOf(properties.getClient().isCompressionEnabled()));

        return new SimpleComponentSummary("RPC", "HTTP RPC", true, 200, details);
    }
}
