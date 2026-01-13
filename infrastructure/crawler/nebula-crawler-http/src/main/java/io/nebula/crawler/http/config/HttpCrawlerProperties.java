package io.nebula.crawler.http.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * HTTP爬虫配置
 *
 * @author Nebula Team
 * @since 2.0.1
 */
@Data
@ConfigurationProperties(prefix = "nebula.crawler.http")
public class HttpCrawlerProperties {
    
    /**
     * 是否启用HTTP爬虫
     */
    private boolean enabled = true;
    
    /**
     * 连接超时(ms)
     */
    private int connectTimeout = 30000;
    
    /**
     * 读取超时(ms)
     */
    private int readTimeout = 60000;
    
    /**
     * 写入超时(ms)
     */
    private int writeTimeout = 60000;
    
    /**
     * 最大连接数
     */
    private int maxConnections = 200;
    
    /**
     * 每主机最大连接数
     */
    private int maxConnectionsPerHost = 20;
    
    /**
     * 连接保活时间(ms)
     */
    private long keepAliveTime = 300000;
    
    /**
     * 默认重试次数
     */
    private int retryCount = 3;
    
    /**
     * 重试间隔(ms)
     */
    private int retryInterval = 1000;
    
    /**
     * 是否使用代理
     */
    private boolean useProxy = false;
    
    /**
     * 默认QPS限制
     */
    private double defaultQps = 5.0;
    
    /**
     * 是否跟随重定向
     */
    private boolean followRedirects = true;
    
    /**
     * 是否信任所有SSL证书（仅限测试环境使用）
     * 警告：生产环境禁止设置为true
     */
    private boolean trustAllCerts = false;
    
    /**
     * User-Agent池
     */
    private List<String> userAgents = new ArrayList<>();
    
    /**
     * 获取随机User-Agent
     */
    public String getRandomUserAgent() {
        if (userAgents == null || userAgents.isEmpty()) {
            return getDefaultUserAgent();
        }
        int index = (int) (Math.random() * userAgents.size());
        return userAgents.get(index);
    }
    
    /**
     * 获取默认User-Agent
     */
    public String getDefaultUserAgent() {
        return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    }
}

