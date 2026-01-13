package io.nebula.crawler.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 爬虫基础配置
 *
 * @author Nebula Team
 * @since 2.0.1
 */
@Data
@ConfigurationProperties(prefix = "nebula.crawler")
public class CrawlerProperties {
    
    /**
     * 是否启用爬虫功能
     */
    private boolean enabled = false;
}

