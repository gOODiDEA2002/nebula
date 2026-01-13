package io.nebula.crawler.core.exception;

/**
 * 爬虫超时异常
 *
 * @author Nebula Team
 * @since 2.0.1
 */
public class CrawlerTimeoutException extends CrawlerException {
    
    private static final String ERROR_CODE = "CRAWLER_TIMEOUT";
    
    private final String url;
    private final long timeout;
    
    public CrawlerTimeoutException(String url, long timeout) {
        super(ERROR_CODE, String.format("爬取超时: url=%s, timeout=%dms", url, timeout));
        this.url = url;
        this.timeout = timeout;
    }
    
    public CrawlerTimeoutException(String url, long timeout, Throwable cause) {
        super(ERROR_CODE, String.format("爬取超时: url=%s, timeout=%dms", url, timeout), cause);
        this.url = url;
        this.timeout = timeout;
    }
    
    public String getUrl() {
        return url;
    }
    
    public long getTimeout() {
        return timeout;
    }
}

