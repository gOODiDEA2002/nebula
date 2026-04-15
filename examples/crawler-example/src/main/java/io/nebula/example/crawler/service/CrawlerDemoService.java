package io.nebula.example.crawler.service;

import io.nebula.example.crawler.entity.dto.BatchCrawlRequestDto;
import io.nebula.example.crawler.entity.dto.BrowserCrawlRequestDto;
import io.nebula.example.crawler.entity.dto.CrawlRequestDto;

import java.util.List;
import java.util.Map;

/**
 * 爬虫演示服务接口
 */
public interface CrawlerDemoService {

    /**
     * 单页爬取（HTTP 引擎）
     */
    Map<String, Object> crawl(CrawlRequestDto dto);

    /**
     * 批量爬取（HTTP 引擎）
     */
    List<Map<String, Object>> crawlBatch(BatchCrawlRequestDto dto);

    /**
     * 爬取并解析 HTML（提取标题和链接）
     */
    Map<String, Object> crawlAndParse(String url);

    /**
     * 浏览器引擎爬取（支持 JS 渲染）
     */
    Map<String, Object> browserCrawl(BrowserCrawlRequestDto dto);

    /**
     * 引擎健康检查（所有引擎）
     */
    Map<String, Object> healthCheck();
}
