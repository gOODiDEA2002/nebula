package io.nebula.crawler.proxy.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 代理池配置属性
 *
 * @author Nebula Team
 * @since 2.0.1
 */
@Data
@ConfigurationProperties(prefix = "nebula.crawler.proxy")
public class ProxyPoolProperties {
    
    /**
     * 是否启用代理池
     */
    private boolean enabled = false;
    
    /**
     * 最小可用代理数量，低于此数量时自动刷新
     */
    private int minAvailable = 10;
    
    /**
     * 代理检测URL
     */
    private String checkUrl = "https://www.baidu.com";
    
    /**
     * 代理检测超时时间(ms)
     */
    private int checkTimeout = 5000;
    
    /**
     * 代理检测间隔(ms)
     */
    private long checkInterval = 300000; // 5分钟
    
    /**
     * 代理最大失败次数，超过此次数将加入黑名单
     */
    private int maxFailCount = 3;
    
    /**
     * 黑名单过期时间(小时)
     */
    private int blacklistExpireHours = 24;
    
    /**
     * 静态代理列表
     */
    private List<String> staticProxies = new ArrayList<>();
    
    /**
     * API代理源配置
     */
    private List<ApiSource> apiSources = new ArrayList<>();
    
    /**
     * API代理源配置
     */
    @Data
    public static class ApiSource {
        /**
         * 来源名称
         */
        private String name;
        
        /**
         * API URL
         */
        private String url;
        
        /**
         * 响应格式: text, json
         */
        private String format = "text";
        
        /**
         * 优先级
         */
        private int priority = 100;
        
        /**
         * JSON格式时的字段映射
         */
        private String hostField = "host";
        private String portField = "port";
        private String usernameField = "username";
        private String passwordField = "password";
    }
}
