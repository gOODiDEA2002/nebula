package io.nebula.crawler.core;

/**
 * 爬虫回调接口
 * <p>
 * 用于异步爬取的回调处理
 * </p>
 *
 * @author Nebula Team
 * @since 2.0.1
 */
public interface CrawlerCallback {
    
    /**
     * 爬取成功回调
     *
     * @param response 成功的响应
     */
    void onSuccess(CrawlerResponse response);
    
    /**
     * 爬取失败回调
     *
     * @param request   原始请求
     * @param exception 异常信息
     */
    void onFailure(CrawlerRequest request, Throwable exception);
}

