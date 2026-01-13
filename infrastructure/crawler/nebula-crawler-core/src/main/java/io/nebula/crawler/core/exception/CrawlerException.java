package io.nebula.crawler.core.exception;

import io.nebula.core.common.exception.NebulaException;

/**
 * 爬虫基础异常
 *
 * @author Nebula Team
 * @since 2.0.1
 */
public class CrawlerException extends NebulaException {
    
    private static final String DEFAULT_ERROR_CODE = "CRAWLER_ERROR";
    
    public CrawlerException(String message) {
        super(DEFAULT_ERROR_CODE, message);
    }
    
    public CrawlerException(String message, Throwable cause) {
        super(DEFAULT_ERROR_CODE, message, cause);
    }
    
    public CrawlerException(String errorCode, String message) {
        super(errorCode, message);
    }
    
    public CrawlerException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
    
    /**
     * 创建爬取失败异常
     */
    public static CrawlerException crawlFailed(String url, String reason) {
        return new CrawlerException("CRAWL_FAILED", 
            String.format("爬取失败: url=%s, reason=%s", url, reason));
    }
    
    /**
     * 创建解析失败异常
     */
    public static CrawlerException parseFailed(String url, String reason) {
        return new CrawlerException("PARSE_FAILED",
            String.format("解析失败: url=%s, reason=%s", url, reason));
    }
}

