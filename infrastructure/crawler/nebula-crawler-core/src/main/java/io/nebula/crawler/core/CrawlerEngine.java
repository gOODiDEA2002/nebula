package io.nebula.crawler.core;

import io.nebula.crawler.core.exception.CrawlerException;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 爬虫引擎接口
 * <p>
 * 定义统一的爬取能力抽象，不同实现（HTTP/浏览器）遵循相同契约。
 * 所有实现必须是线程安全的。
 * </p>
 *
 * @author Nebula Team
 * @since 2.0.1
 */
public interface CrawlerEngine {
    
    /**
     * 获取引擎类型
     *
     * @return 引擎类型枚举
     */
    CrawlerEngineType getType();
    
    /**
     * 同步爬取
     * <p>
     * 阻塞直到请求完成或超时。
     * 失败时不抛出异常，而是返回包含错误信息的Response。
     * </p>
     *
     * @param request 爬取请求，不能为null
     * @return 爬取响应，永不为null
     */
    CrawlerResponse crawl(CrawlerRequest request);
    
    /**
     * 异步爬取
     * <p>
     * 立即返回Future，可用于并行处理。
     * Future完成时包含响应结果，不会抛出异常。
     * </p>
     *
     * @param request 爬取请求，不能为null
     * @return 异步响应Future
     */
    CompletableFuture<CrawlerResponse> crawlAsync(CrawlerRequest request);
    
    /**
     * 批量爬取
     * <p>
     * 内部可能并行执行，提高效率。
     * 返回的列表顺序与请求列表对应。
     * </p>
     *
     * @param requests 请求列表，不能为null或空
     * @return 响应列表，顺序与请求对应
     */
    List<CrawlerResponse> crawlBatch(List<CrawlerRequest> requests);
    
    /**
     * 带回调的爬取
     * <p>
     * 适用于流式处理场景。
     * 默认实现基于同步爬取。
     * </p>
     *
     * @param request  爬取请求
     * @param callback 回调处理器
     */
    default void crawlWithCallback(CrawlerRequest request, CrawlerCallback callback) {
        try {
            CrawlerResponse response = crawl(request);
            if (response.isSuccess()) {
                callback.onSuccess(response);
            } else {
                callback.onFailure(request, new CrawlerException(response.getErrorMessage()));
            }
        } catch (Exception e) {
            callback.onFailure(request, e);
        }
    }
    
    /**
     * 关闭引擎
     * <p>
     * 释放资源，关闭连接池。
     * 关闭后不应再使用该引擎。
     * </p>
     */
    void shutdown();
    
    /**
     * 健康检查
     *
     * @return 引擎是否健康可用
     */
    boolean isHealthy();
}

