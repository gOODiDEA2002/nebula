package io.nebula.autoconfigure.diagnostic;

import io.nebula.autoconfigure.rpc.AsyncRpcProperties;
import io.nebula.discovery.nacos.config.NacosProperties;
import io.nebula.rpc.core.config.RpcDiscoveryProperties;
import io.nebula.rpc.http.config.HttpRpcProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * Nebula 框架启动摘要监听器
 * 
 * 在应用启动完成后输出框架配置摘要，帮助开发者快速了解当前配置状态
 * 
 * @author Nebula Framework
 * @since 2.0.1
 */
@Slf4j
@AutoConfiguration
public class NebulaStartupSummaryListener {
    
    @Bean
    public ApplicationListener<ApplicationReadyEvent> nebulaStartupSummary(Environment environment) {
        return event -> printStartupSummary(environment);
    }
    
    private void printStartupSummary(Environment environment) {
        StringBuilder sb = new StringBuilder();
        String lineSeparator = System.lineSeparator();
        
        sb.append(lineSeparator);
        sb.append("=".repeat(70)).append(lineSeparator);
        sb.append("                    NEBULA FRAMEWORK STARTUP SUMMARY                   ").append(lineSeparator);
        sb.append("=".repeat(70)).append(lineSeparator);
        
        // 框架版本
        sb.append(formatSection("Framework Info"));
        sb.append(formatEntry("Version", "2.0.1-SNAPSHOT"));
        sb.append(formatEntry("Profile", getActiveProfiles(environment)));
        
        // 服务发现配置
        appendNacosInfo(sb, environment, lineSeparator);
        
        // RPC 配置
        appendRpcInfo(sb, environment, lineSeparator);
        
        // 异步 RPC 配置
        appendAsyncRpcInfo(sb, environment, lineSeparator);
        
        sb.append("=".repeat(70)).append(lineSeparator);
        
        log.info(sb.toString());
    }
    
    private void appendNacosInfo(StringBuilder sb, Environment environment, String lineSeparator) {
        boolean nacosEnabled = environment.getProperty("nebula.discovery.nacos.enabled", Boolean.class, true);
        
        sb.append(formatSection("Service Discovery (Nacos)"));
        
        if (!nacosEnabled) {
            sb.append(formatEntry("Status", "DISABLED"));
            return;
        }
        
        NacosProperties nacos = Binder.get(environment)
            .bind("nebula.discovery.nacos", NacosProperties.class)
            .orElseGet(NacosProperties::new);
        
        sb.append(formatEntry("Status", "ENABLED"));
        sb.append(formatEntry("Server Address", nacos.getServerAddr()));
        sb.append(formatEntry("Namespace", nacos.getNamespace().isEmpty() ? "public" : nacos.getNamespace()));
        sb.append(formatEntry("Group", nacos.getGroupName()));
        sb.append(formatEntry("Cluster", nacos.getClusterName()));
        sb.append(formatEntry("Auto Register", String.valueOf(nacos.isAutoRegister())));
    }
    
    private void appendRpcInfo(StringBuilder sb, Environment environment, String lineSeparator) {
        boolean httpRpcEnabled = environment.getProperty("nebula.rpc.http.enabled", Boolean.class, true);
        boolean discoveryEnabled = environment.getProperty("nebula.rpc.discovery.enabled", Boolean.class, true);
        
        sb.append(formatSection("RPC Configuration"));
        
        // HTTP RPC
        if (httpRpcEnabled) {
            HttpRpcProperties httpRpc = Binder.get(environment)
                .bind("nebula.rpc.http", HttpRpcProperties.class)
                .orElseGet(HttpRpcProperties::new);
            
            sb.append(formatEntry("HTTP RPC", "ENABLED"));
            sb.append(formatEntry("  Server Port", String.valueOf(httpRpc.getServer().getPort())));
            sb.append(formatEntry("  Context Path", httpRpc.getServer().getContextPath()));
            sb.append(formatEntry("  Client Timeout", httpRpc.getClient().getReadTimeout() + "ms"));
        } else {
            sb.append(formatEntry("HTTP RPC", "DISABLED"));
        }
        
        // RPC Discovery
        if (discoveryEnabled) {
            RpcDiscoveryProperties discovery = Binder.get(environment)
                .bind("nebula.rpc.discovery", RpcDiscoveryProperties.class)
                .orElseGet(RpcDiscoveryProperties::new);
            
            sb.append(formatEntry("Discovery Integration", "ENABLED"));
            sb.append(formatEntry("  Load Balance", discovery.getLoadBalanceStrategy().toUpperCase()));
            sb.append(formatEntry("  Cache Enabled", String.valueOf(discovery.isEnableCache())));
        } else {
            sb.append(formatEntry("Discovery Integration", "DISABLED"));
        }
    }
    
    private void appendAsyncRpcInfo(StringBuilder sb, Environment environment, String lineSeparator) {
        boolean asyncEnabled = environment.getProperty("nebula.rpc.async.enabled", Boolean.class, true);
        
        sb.append(formatSection("Async RPC Configuration"));
        
        if (!asyncEnabled) {
            sb.append(formatEntry("Status", "DISABLED"));
            return;
        }
        
        AsyncRpcProperties async = Binder.get(environment)
            .bind("nebula.rpc.async", AsyncRpcProperties.class)
            .orElseGet(AsyncRpcProperties::new);
        
        sb.append(formatEntry("Status", "ENABLED"));
        sb.append(formatEntry("Storage Type", async.getStorage().getType().toUpperCase()));
        sb.append(formatEntry("Executor Pool", 
            async.getExecutor().getCorePoolSize() + "-" + async.getExecutor().getMaxPoolSize()));
        sb.append(formatEntry("Cleanup Enabled", String.valueOf(async.getCleanup().isEnabled())));
    }
    
    private String formatSection(String title) {
        return String.format("%n  [%s]%n", title);
    }
    
    private String formatEntry(String key, String value) {
        return String.format("    %-20s : %s%n", key, value);
    }
    
    private String getActiveProfiles(Environment environment) {
        String[] profiles = environment.getActiveProfiles();
        if (profiles.length == 0) {
            return "default";
        }
        return String.join(", ", profiles);
    }
}
