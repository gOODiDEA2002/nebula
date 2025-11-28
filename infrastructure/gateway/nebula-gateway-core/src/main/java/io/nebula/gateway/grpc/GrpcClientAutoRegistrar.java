package io.nebula.gateway.grpc;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nebula.discovery.core.ServiceDiscovery;
import io.nebula.discovery.core.ServiceDiscoveryException;
import io.nebula.discovery.core.ServiceInstance;
import io.nebula.gateway.config.GatewayProperties;
import io.nebula.rpc.core.annotation.RpcClient;
import io.nebula.rpc.grpc.client.GrpcRpcClient;
import io.nebula.rpc.grpc.config.GrpcRpcProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * gRPC客户端自动注册器
 * <p>
 * 根据配置文件中的服务定义，自动扫描@RpcClient接口并创建gRPC客户端代理Bean
 * 支持两种模式：
 * <ul>
 *   <li>服务发现模式：从 Nacos 等注册中心动态获取服务地址</li>
 *   <li>静态地址模式：使用配置文件中指定的固定地址</li>
 * </ul>
 */
@Slf4j
public class GrpcClientAutoRegistrar {

    private final GatewayProperties gatewayProperties;
    private final ObjectMapper objectMapper;
    private final DefaultListableBeanFactory beanFactory;
    private final ServiceDiscovery serviceDiscovery;
    
    /** 服务名到gRPC客户端的映射 */
    private final Map<String, GrpcRpcClient> grpcClients = new HashMap<>();
    
    /** 接口类到代理对象的映射 */
    private final Map<Class<?>, Object> proxyCache = new HashMap<>();
    
    /** 轮询索引（用于负载均衡） */
    private final Map<String, AtomicInteger> roundRobinCounters = new HashMap<>();

    public GrpcClientAutoRegistrar(GatewayProperties gatewayProperties,
                                   ObjectMapper objectMapper,
                                   DefaultListableBeanFactory beanFactory,
                                   ServiceDiscovery serviceDiscovery) {
        this.gatewayProperties = gatewayProperties;
        this.objectMapper = objectMapper;
        this.beanFactory = beanFactory;
        this.serviceDiscovery = serviceDiscovery;
    }

    /**
     * 扫描并注册所有gRPC客户端
     */
    public void scanAndRegister() {
        GatewayProperties.GrpcConfig grpcConfig = gatewayProperties.getGrpc();
        if (!grpcConfig.isEnabled()) {
            log.info("gRPC Gateway功能已禁用");
            return;
        }

        Map<String, GatewayProperties.ServiceConfig> services = grpcConfig.getServices();
        if (services.isEmpty()) {
            log.warn("未配置任何gRPC服务");
            return;
        }

        log.info("开始扫描并注册gRPC客户端，配置了{}个服务", services.size());

        for (Map.Entry<String, GatewayProperties.ServiceConfig> entry : services.entrySet()) {
            String serviceName = entry.getKey();
            GatewayProperties.ServiceConfig serviceConfig = entry.getValue();

            if (!serviceConfig.isEnabled()) {
                log.info("服务 {} 已禁用，跳过", serviceName);
                continue;
            }

            registerService(serviceName, serviceConfig, grpcConfig.getClientDefaults());
        }

        log.info("gRPC客户端注册完成，共注册{}个服务", grpcClients.size());
    }

    /**
     * 注册单个服务的gRPC客户端
     */
    private void registerService(String configKey,
                                  GatewayProperties.ServiceConfig serviceConfig,
                                  GatewayProperties.ClientDefaults defaults) {
        // 确定服务名（优先使用 serviceName 配置，否则使用 configKey）
        String serviceName = serviceConfig.getServiceName() != null && !serviceConfig.getServiceName().isEmpty()
                ? serviceConfig.getServiceName()
                : configKey;
        
        // 解析服务地址
        String address = resolveServiceAddress(serviceName, serviceConfig);
        if (address == null || address.isEmpty()) {
            log.warn("服务 {} 无法获取地址，跳过", serviceName);
            return;
        }

        // 创建gRPC客户端配置
        GrpcRpcProperties.ClientConfig clientConfig = new GrpcRpcProperties.ClientConfig();
        clientConfig.setTarget(address.replace("static://", ""));
        clientConfig.setNegotiationType(defaults.getNegotiationType());
        clientConfig.setConnectTimeout(
                serviceConfig.getConnectTimeout() != null ? serviceConfig.getConnectTimeout() : defaults.getConnectTimeout());
        clientConfig.setRequestTimeout(
                serviceConfig.getRequestTimeout() != null ? serviceConfig.getRequestTimeout() : defaults.getRequestTimeout());
        clientConfig.setRetryCount(
                serviceConfig.getRetryCount() != null ? serviceConfig.getRetryCount() : defaults.getRetryCount());
        clientConfig.setRetryInterval(defaults.getRetryInterval());
        clientConfig.setMaxInboundMessageSize(defaults.getMaxInboundMessageSize());

        // 创建gRPC客户端
        GrpcRpcClient grpcClient = new GrpcRpcClient(objectMapper, clientConfig);
        grpcClients.put(configKey, grpcClient);
        log.info("创建gRPC客户端: {} -> {} (服务发现: {})", configKey, address, serviceConfig.isUseDiscovery());

        // 扫描并注册@RpcClient接口
        if (gatewayProperties.getGrpc().isAutoScan() && !serviceConfig.getApiPackages().isEmpty()) {
            scanAndRegisterRpcClients(configKey, grpcClient, serviceConfig.getApiPackages());
        }
    }
    
    /**
     * 解析服务地址
     * 支持服务发现模式和静态地址模式
     */
    private String resolveServiceAddress(String serviceName, GatewayProperties.ServiceConfig serviceConfig) {
        // 如果不使用服务发现，直接返回配置的静态地址
        if (!serviceConfig.isUseDiscovery()) {
            String address = serviceConfig.getAddress();
            if (address == null || address.isEmpty()) {
                log.warn("服务 {} 配置了静态地址模式但未指定 address", serviceName);
            }
            return address;
        }
        
        // 使用服务发现获取地址
        if (serviceDiscovery == null) {
            log.warn("服务 {} 配置了服务发现模式，但 ServiceDiscovery 未注入，回退到静态地址", serviceName);
            return serviceConfig.getAddress();
        }
        
        try {
            List<ServiceInstance> instances = serviceDiscovery.getInstances(serviceName);
            if (instances == null || instances.isEmpty()) {
                log.warn("从服务发现获取服务 {} 实例失败：无可用实例", serviceName);
                // 回退到静态地址
                if (serviceConfig.getAddress() != null && !serviceConfig.getAddress().isEmpty()) {
                    log.info("回退使用静态地址: {}", serviceConfig.getAddress());
                    return serviceConfig.getAddress();
                }
                return null;
            }
            
            // 使用轮询负载均衡选择实例
            ServiceInstance instance = selectInstance(serviceName, instances);
            
            // 计算 gRPC 端口
            int grpcPort = serviceConfig.getGrpcPort() != null 
                    ? serviceConfig.getGrpcPort() 
                    : calculateGrpcPort(instance);
            
            String address = instance.getIp() + ":" + grpcPort;
            log.info("从服务发现获取服务 {} 地址: {} (实例数: {})", serviceName, address, instances.size());
            return address;
            
        } catch (ServiceDiscoveryException e) {
            log.error("从服务发现获取服务 {} 实例失败: {}", serviceName, e.getMessage());
            // 回退到静态地址
            if (serviceConfig.getAddress() != null && !serviceConfig.getAddress().isEmpty()) {
                log.info("回退使用静态地址: {}", serviceConfig.getAddress());
                return serviceConfig.getAddress();
            }
            return null;
        }
    }
    
    /**
     * 使用轮询策略选择服务实例
     */
    private ServiceInstance selectInstance(String serviceName, List<ServiceInstance> instances) {
        AtomicInteger counter = roundRobinCounters.computeIfAbsent(serviceName, k -> new AtomicInteger(0));
        int index = counter.getAndIncrement() % instances.size();
        return instances.get(index);
    }
    
    /**
     * 计算 gRPC 端口
     * 默认规则：gRPC 端口 = HTTP 端口 + 1000
     * 例如：HTTP 4001 -> gRPC 5001
     */
    private int calculateGrpcPort(ServiceInstance instance) {
        // 检查元数据中是否有 gRPC 端口
        String grpcPortMeta = instance.getMetadata("grpcPort");
        if (grpcPortMeta != null) {
            try {
                return Integer.parseInt(grpcPortMeta);
            } catch (NumberFormatException e) {
                log.warn("无效的 gRPC 端口元数据: {}", grpcPortMeta);
            }
        }
        
        // 默认：HTTP 端口 + 1000
        return instance.getPort() + 1000;
    }

    /**
     * 扫描指定包下的@RpcClient接口并注册代理Bean
     */
    private void scanAndRegisterRpcClients(String serviceName, GrpcRpcClient grpcClient, java.util.List<String> packages) {
        // 创建自定义扫描器，支持扫描接口
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(org.springframework.beans.factory.annotation.AnnotatedBeanDefinition beanDefinition) {
                // 允许接口作为候选组件
                return beanDefinition.getMetadata().isInterface() || super.isCandidateComponent(beanDefinition);
            }
        };
        scanner.addIncludeFilter(new AnnotationTypeFilter(RpcClient.class));

        log.info("开始扫描服务 {} 的API包: {}", serviceName, packages);

        for (String packageName : packages) {
            log.debug("扫描包: {}", packageName);
            Set<BeanDefinition> candidates = scanner.findCandidateComponents(packageName);
            log.info("在包 {} 中发现 {} 个候选组件", packageName, candidates.size());
            
            for (BeanDefinition candidate : candidates) {
                try {
                    Class<?> interfaceClass = Class.forName(candidate.getBeanClassName());
                    
                    if (!interfaceClass.isInterface()) {
                        continue;
                    }

                    // 生成Bean名称
                    String beanName = generateBeanName(interfaceClass);
                    
                    // 检查是否已存在
                    if (beanFactory.containsBean(beanName)) {
                        log.debug("Bean {} 已存在，跳过", beanName);
                        continue;
                    }

                    // 创建代理对象
                    Object proxy = grpcClient.createProxy(interfaceClass);
                    proxyCache.put(interfaceClass, proxy);

                    // 注册为Spring Bean
                    beanFactory.registerSingleton(beanName, proxy);
                    
                    log.info("注册gRPC客户端代理: {} -> {} (服务: {})", 
                            beanName, interfaceClass.getSimpleName(), serviceName);

                } catch (ClassNotFoundException e) {
                    log.warn("无法加载类: {}", candidate.getBeanClassName(), e);
                }
            }
        }
    }

    /**
     * 生成Bean名称
     */
    private String generateBeanName(Class<?> interfaceClass) {
        String simpleName = interfaceClass.getSimpleName();
        // 首字母小写
        return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
    }

    /**
     * 获取指定接口的代理对象
     */
    @SuppressWarnings("unchecked")
    public <T> T getProxy(Class<T> interfaceClass) {
        return (T) proxyCache.get(interfaceClass);
    }

    /**
     * 获取指定服务的gRPC客户端
     */
    public GrpcRpcClient getGrpcClient(String serviceName) {
        return grpcClients.get(serviceName);
    }

    /**
     * 获取所有代理对象
     */
    public Map<Class<?>, Object> getAllProxies() {
        return new HashMap<>(proxyCache);
    }

    /**
     * 关闭所有gRPC客户端
     */
    public void shutdown() {
        log.info("关闭所有gRPC客户端...");
        for (Map.Entry<String, GrpcRpcClient> entry : grpcClients.entrySet()) {
            try {
                entry.getValue().close();
                log.debug("关闭gRPC客户端: {}", entry.getKey());
            } catch (Exception e) {
                log.error("关闭gRPC客户端失败: {}", entry.getKey(), e);
            }
        }
    }
}

