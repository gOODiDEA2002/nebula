package io.nebula.autoconfigure.diagnostic;

import io.nebula.autoconfigure.rpc.AsyncRpcProperties;
import io.nebula.discovery.core.ServiceDiscovery;
import io.nebula.discovery.nacos.config.NacosProperties;
import io.nebula.rpc.core.config.RpcDiscoveryProperties;
import io.nebula.rpc.http.config.HttpRpcProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Nebula 框架诊断端点
 * 
 * 提供框架运行状态的诊断信息，支持通过 Actuator 访问
 * 
 * 访问路径: /actuator/nebula-diagnostic
 * 
 * @author Nebula Framework
 * @since 2.0.1
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(name = "org.springframework.boot.actuate.endpoint.annotation.Endpoint")
public class NebulaDiagnosticEndpoint {
    
    @Bean
    public NebulaDiagnosticEndpointBean nebulaDiagnosticEndpointBean(
            Environment environment,
            ApplicationContext applicationContext) {
        return new NebulaDiagnosticEndpointBean(environment, applicationContext);
    }
    
    @Endpoint(id = "nebula-diagnostic")
    public static class NebulaDiagnosticEndpointBean {
        
        private final Environment environment;
        private final ApplicationContext applicationContext;
        
        public NebulaDiagnosticEndpointBean(Environment environment, 
                                            ApplicationContext applicationContext) {
            this.environment = environment;
            this.applicationContext = applicationContext;
        }
        
        @ReadOperation
        public Map<String, Object> diagnostic() {
            Map<String, Object> result = new LinkedHashMap<>();
            
            result.put("timestamp", LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            result.put("framework", getFrameworkInfo());
            result.put("discovery", getDiscoveryInfo());
            result.put("rpc", getRpcInfo());
            result.put("asyncRpc", getAsyncRpcInfo());
            result.put("dependencies", getDependencyInfo());
            
            return result;
        }
        
        private Map<String, Object> getFrameworkInfo() {
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("name", "Nebula Framework");
            info.put("version", "2.0.1-SNAPSHOT");
            info.put("javaVersion", System.getProperty("java.version"));
            info.put("springBootVersion", org.springframework.boot.SpringBootVersion.getVersion());
            info.put("activeProfiles", environment.getActiveProfiles().length > 0 
                ? environment.getActiveProfiles() : new String[]{"default"});
            return info;
        }
        
        private Map<String, Object> getDiscoveryInfo() {
            Map<String, Object> info = new LinkedHashMap<>();
            
            boolean enabled = environment.getProperty(
                "nebula.discovery.nacos.enabled", Boolean.class, true);
            info.put("enabled", enabled);
            
            if (!enabled) {
                info.put("status", "DISABLED");
                return info;
            }
            
            NacosProperties nacos = Binder.get(environment)
                .bind("nebula.discovery.nacos", NacosProperties.class)
                .orElseGet(NacosProperties::new);
            
            info.put("type", "Nacos");
            info.put("serverAddr", nacos.getServerAddr());
            info.put("namespace", nacos.getNamespace().isEmpty() ? "public" : nacos.getNamespace());
            info.put("groupName", nacos.getGroupName());
            info.put("clusterName", nacos.getClusterName());
            info.put("autoRegister", nacos.isAutoRegister());
            
            // 检查连接状态
            try {
                ServiceDiscovery discovery = applicationContext.getBean(ServiceDiscovery.class);
                info.put("status", "CONNECTED");
                info.put("beanPresent", true);
            } catch (Exception e) {
                info.put("status", "NOT_CONNECTED");
                info.put("beanPresent", false);
                info.put("error", e.getMessage());
            }
            
            return info;
        }
        
        private Map<String, Object> getRpcInfo() {
            Map<String, Object> info = new LinkedHashMap<>();
            
            // HTTP RPC
            boolean httpEnabled = environment.getProperty(
                "nebula.rpc.http.enabled", Boolean.class, true);
            
            Map<String, Object> httpInfo = new LinkedHashMap<>();
            httpInfo.put("enabled", httpEnabled);
            
            if (httpEnabled) {
                HttpRpcProperties httpRpc = Binder.get(environment)
                    .bind("nebula.rpc.http", HttpRpcProperties.class)
                    .orElseGet(HttpRpcProperties::new);
                
                httpInfo.put("server", Map.of(
                    "enabled", httpRpc.getServer().isEnabled(),
                    "port", httpRpc.getServer().getPort(),
                    "contextPath", httpRpc.getServer().getContextPath()
                ));
                httpInfo.put("client", Map.of(
                    "enabled", httpRpc.getClient().isEnabled(),
                    "connectTimeout", httpRpc.getClient().getConnectTimeout(),
                    "readTimeout", httpRpc.getClient().getReadTimeout(),
                    "retryCount", httpRpc.getClient().getRetryCount()
                ));
            }
            info.put("http", httpInfo);
            
            // RPC Discovery Integration
            boolean discoveryEnabled = environment.getProperty(
                "nebula.rpc.discovery.enabled", Boolean.class, true);
            
            Map<String, Object> discoveryInfo = new LinkedHashMap<>();
            discoveryInfo.put("enabled", discoveryEnabled);
            
            if (discoveryEnabled) {
                RpcDiscoveryProperties discovery = Binder.get(environment)
                    .bind("nebula.rpc.discovery", RpcDiscoveryProperties.class)
                    .orElseGet(RpcDiscoveryProperties::new);
                
                discoveryInfo.put("loadBalanceStrategy", discovery.getLoadBalanceStrategy());
                discoveryInfo.put("cacheEnabled", discovery.isEnableCache());
                discoveryInfo.put("cacheTimeout", discovery.getCacheTimeout());
                discoveryInfo.put("retryCount", discovery.getRetryCount());
            }
            info.put("discoveryIntegration", discoveryInfo);
            
            return info;
        }
        
        private Map<String, Object> getAsyncRpcInfo() {
            Map<String, Object> info = new LinkedHashMap<>();
            
            boolean enabled = environment.getProperty(
                "nebula.rpc.async.enabled", Boolean.class, true);
            info.put("enabled", enabled);
            
            if (!enabled) {
                return info;
            }
            
            AsyncRpcProperties async = Binder.get(environment)
                .bind("nebula.rpc.async", AsyncRpcProperties.class)
                .orElseGet(AsyncRpcProperties::new);
            
            info.put("storage", Map.of(
                "type", async.getStorage().getType(),
                "nacos", Map.of(
                    "serverAddr", async.getStorage().getNacos().getServerAddr(),
                    "namespace", async.getStorage().getNacos().getNamespace()
                )
            ));
            
            info.put("executor", Map.of(
                "corePoolSize", async.getExecutor().getCorePoolSize(),
                "maxPoolSize", async.getExecutor().getMaxPoolSize(),
                "queueCapacity", async.getExecutor().getQueueCapacity()
            ));
            
            info.put("cleanup", Map.of(
                "enabled", async.getCleanup().isEnabled(),
                "retentionDays", async.getCleanup().getRetentionDays()
            ));
            
            return info;
        }
        
        private Map<String, Object> getDependencyInfo() {
            Map<String, Object> info = new LinkedHashMap<>();
            
            // 检查各模块是否存在
            info.put("nebula-discovery-nacos", checkClass("io.nebula.discovery.nacos.NacosServiceDiscovery"));
            info.put("nebula-rpc-http", checkClass("io.nebula.rpc.http.client.HttpRpcClient"));
            info.put("nebula-rpc-async", checkClass("io.nebula.rpc.async.execution.AsyncRpcExecutionManager"));
            info.put("nebula-security", checkClass("io.nebula.security.jwt.JwtTokenService"));
            info.put("nebula-data-persistence", checkClass("io.nebula.data.persistence.config.MybatisPlusProperties"));
            info.put("nebula-data-cache", checkClass("io.nebula.data.cache.manager.CacheManager"));
            
            return info;
        }
        
        private boolean checkClass(String className) {
            try {
                Class.forName(className);
                return true;
            } catch (ClassNotFoundException e) {
                return false;
            }
        }
    }
}
