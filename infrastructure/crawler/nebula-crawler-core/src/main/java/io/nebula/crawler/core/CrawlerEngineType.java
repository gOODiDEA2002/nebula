package io.nebula.crawler.core;

/**
 * 爬虫引擎类型枚举
 *
 * @author Nebula Team
 * @since 2.0.1
 */
public enum CrawlerEngineType {
    
    /**
     * HTTP爬虫引擎
     * 适用于静态页面，使用HTTP客户端直接请求
     */
    HTTP,
    
    /**
     * 浏览器爬虫引擎
     * 适用于需要JavaScript渲染的动态页面
     */
    BROWSER,
    
    /**
     * API调用引擎
     * 适用于RESTful API数据获取
     */
    API
}

