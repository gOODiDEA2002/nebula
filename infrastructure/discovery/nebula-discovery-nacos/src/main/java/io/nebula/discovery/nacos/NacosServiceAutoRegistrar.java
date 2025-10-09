package io.nebula.discovery.nacos;

import io.nebula.discovery.core.ServiceDiscovery;
import io.nebula.discovery.core.ServiceDiscoveryException;
import io.nebula.discovery.core.ServiceInstance;
import io.nebula.discovery.nacos.config.NacosProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.boot.web.servlet.context.ServletWebServerInitializedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.env.Environment;

import jakarta.annotation.PreDestroy;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

/**
 * Nacos 服务自动注册器
 * 在应用启动时自动将服务注册到 Nacos,关闭时自动注销
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
public class NacosServiceAutoRegistrar implements ApplicationListener<ApplicationEvent> {

    private final ServiceDiscovery serviceDiscovery;
    private final NacosProperties nacosProperties;
    private final Environment environment;
    
    private String serviceName;
    private String instanceId;
    private int port;
    private boolean registered = false;

    public NacosServiceAutoRegistrar(ServiceDiscovery serviceDiscovery,
                                     NacosProperties nacosProperties,
                                     Environment environment) {
        this.serviceDiscovery = serviceDiscovery;
        this.nacosProperties = nacosProperties;
        this.environment = environment;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ServletWebServerInitializedEvent) {
            ServletWebServerInitializedEvent serverEvent = (ServletWebServerInitializedEvent) event;
            registerService(serverEvent.getWebServer().getPort());
        } else if (event instanceof ContextClosedEvent) {
            deregisterService();
        }
    }

    /**
     * 注册服务到 Nacos
     */
    private void registerService(int serverPort) {
        if (!nacosProperties.isAutoRegister()) {
            log.info("Nacos 自动注册已禁用,跳过服务注册");
            return;
        }

        try {
            this.port = serverPort;
            this.serviceName = environment.getProperty("spring.application.name", "unknown-service");
            this.instanceId = generateInstanceId();

            String ip = getLocalIp();
            
            // 构建元数据
            Map<String, String> metadata = new HashMap<>(nacosProperties.getMetadata());
            metadata.put("startTime", String.valueOf(System.currentTimeMillis()));
            metadata.put("version", environment.getProperty("spring.application.version", "1.0.0"));
            metadata.put("profile", String.join(",", environment.getActiveProfiles()));

            // 构建服务实例
            ServiceInstance instance = ServiceInstance.builder()
                    .serviceName(serviceName)
                    .instanceId(instanceId)
                    .ip(ip)
                    .port(port)
                    .weight(nacosProperties.getWeight())
                    .healthy(nacosProperties.isHealthy())
                    .enabled(nacosProperties.isInstanceEnabled())
                    .clusterName(nacosProperties.getClusterName())
                    .groupName(nacosProperties.getGroupName())
                    .protocol("http")
                    .metadata(metadata)
                    .build();

            // 注册服务
            serviceDiscovery.register(instance);
            registered = true;

            log.info("服务自动注册到 Nacos 成功: serviceName={}, instanceId={}, address={}:{}", 
                    serviceName, instanceId, ip, port);
        } catch (Exception e) {
            log.error("服务自动注册到 Nacos 失败", e);
            throw new RuntimeException("服务自动注册失败", e);
        }
    }

    /**
     * 注销服务
     */
    @PreDestroy
    public void deregisterService() {
        if (!registered) {
            return;
        }

        try {
            serviceDiscovery.deregister(serviceName, instanceId);
            log.info("服务自动注销成功: serviceName={}, instanceId={}", serviceName, instanceId);
        } catch (ServiceDiscoveryException e) {
            log.error("服务自动注销失败: serviceName={}, instanceId={}", serviceName, instanceId, e);
        }
    }

    /**
     * 生成实例 ID
     */
    private String generateInstanceId() {
        String ip = getLocalIp();
        return String.format("%s:%s:%d", serviceName, ip, port);
    }

    /**
     * 获取本地 IP 地址
     */
    private String getLocalIp() {
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            return localHost.getHostAddress();
        } catch (UnknownHostException e) {
            log.warn("获取本地 IP 失败,使用默认值 localhost", e);
            return "127.0.0.1";
        }
    }
}

