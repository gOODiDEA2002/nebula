package io.nebula.crawler.browser.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 浏览器爬虫配置
 * <p>
 * 支持两种运行模式：
 * <ul>
 *   <li>LOCAL - 本地启动浏览器（默认）</li>
 *   <li>REMOTE - 连接到远程 Playwright Server（适用于 Docker/K8s 部署）</li>
 * </ul>
 * </p>
 *
 * @author Nebula Team
 * @since 2.0.1
 */
@Data
@ConfigurationProperties(prefix = "nebula.crawler.browser")
public class BrowserCrawlerProperties {
    
    /**
     * 是否启用浏览器爬虫
     */
    private boolean enabled = true;
    
    /**
     * 运行模式：LOCAL 或 REMOTE
     * 默认使用 REMOTE 模式，连接远程 Playwright Server
     */
    private Mode mode = Mode.REMOTE;
    
    /**
     * 浏览器类型: chromium, firefox, webkit
     */
    private String browserType = "chromium";
    
    /**
     * 是否无头模式（仅 LOCAL 模式有效）
     */
    private boolean headless = true;
    
    /**
     * 浏览器实例池大小
     */
    private int poolSize = 5;
    
    /**
     * 页面加载超时(ms)
     */
    private int pageTimeout = 30000;
    
    /**
     * 导航超时(ms)
     */
    private int navigationTimeout = 30000;
    
    /**
     * 连接超时(ms)（仅 REMOTE 模式有效）
     */
    private int connectTimeout = 30000;
    
    /**
     * 错误时是否截图
     */
    private boolean screenshotOnError = true;
    
    /**
     * 是否使用代理
     */
    private boolean useProxy = false;
    
    /**
     * 视口宽度
     */
    private int viewportWidth = 1920;
    
    /**
     * 视口高度
     */
    private int viewportHeight = 1080;
    
    /**
     * 是否禁用图片加载（提高性能）
     */
    private boolean disableImages = false;
    
    /**
     * 是否禁用CSS（提高性能）
     */
    private boolean disableCss = false;
    
    /**
     * 默认User-Agent
     */
    private String userAgent;
    
    /**
     * 浏览器启动慢速模式延迟(ms)，0表示禁用
     * 用于调试或降低检测风险（仅 LOCAL 模式有效）
     */
    private int slowMo = 0;
    
    /**
     * 远程连接配置（仅 REMOTE 模式有效）
     */
    private RemoteConfig remote = new RemoteConfig();
    
    /**
     * 运行模式枚举
     */
    public enum Mode {
        /**
         * 本地启动浏览器
         */
        LOCAL,
        /**
         * 连接到远程 Playwright Server
         */
        REMOTE
    }
    
    /**
     * 远程连接配置
     */
    @Data
    public static class RemoteConfig {
        
        /**
         * 远程 Playwright Server 端点列表
         * 支持多个端点用于负载均衡
         * 格式: ws://host:port
         * 默认连接 localhost:9222
         */
        private List<String> endpoints = new ArrayList<>(List.of("ws://localhost:9222"));
        
        /**
         * 负载均衡策略
         */
        private LoadBalanceStrategy loadBalanceStrategy = LoadBalanceStrategy.ROUND_ROBIN;
        
        /**
         * 健康检查间隔(ms)
         */
        private int healthCheckInterval = 30000;
        
        /**
         * 连接失败重试次数
         */
        private int maxRetries = 3;
        
        /**
         * 重试间隔(ms)
         */
        private int retryInterval = 1000;
        
        /**
         * 获取第一个端点（兼容单端点配置）
         */
        public String getFirstEndpoint() {
            return endpoints.isEmpty() ? null : endpoints.get(0);
        }
        
        /**
         * 添加端点
         */
        public void addEndpoint(String endpoint) {
            if (endpoints == null) {
                endpoints = new ArrayList<>();
            }
            endpoints.add(endpoint);
        }
    }
    
    /**
     * 负载均衡策略
     */
    public enum LoadBalanceStrategy {
        /**
         * 轮询
         */
        ROUND_ROBIN,
        /**
         * 随机
         */
        RANDOM,
        /**
         * 最少连接
         */
        LEAST_CONNECTIONS
    }
    
    /**
     * 检查是否为远程模式
     */
    public boolean isRemoteMode() {
        return mode == Mode.REMOTE;
    }
    
    /**
     * 检查是否为本地模式
     */
    public boolean isLocalMode() {
        return mode == Mode.LOCAL;
    }
}

