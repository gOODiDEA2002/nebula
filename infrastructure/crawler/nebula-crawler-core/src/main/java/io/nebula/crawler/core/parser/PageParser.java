package io.nebula.crawler.core.parser;

import io.nebula.crawler.core.CrawlerResponse;

/**
 * 页面解析器接口
 * <p>
 * 将爬取的响应内容解析为结构化数据
 * </p>
 *
 * @param <T> 解析结果类型
 * @author Nebula Team
 * @since 2.0.1
 */
public interface PageParser<T> {

    /**
     * 解析响应内容
     *
     * @param response 爬虫响应
     * @return 解析后的结构化数据
     * @throws ParseException 解析失败时抛出
     */
    T parse(CrawlerResponse response) throws ParseException;

    /**
     * 判断是否支持解析该响应
     *
     * @param response 爬虫响应
     * @return true表示支持
     */
    default boolean supports(CrawlerResponse response) {
        return response != null && response.hasContent();
    }

    /**
     * 获取解析器名称
     *
     * @return 解析器名称
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }
}

