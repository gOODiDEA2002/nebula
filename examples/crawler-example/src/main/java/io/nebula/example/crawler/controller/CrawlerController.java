package io.nebula.example.crawler.controller;

import io.nebula.core.common.result.Result;
import io.nebula.example.crawler.entity.dto.BatchCrawlRequestDto;
import io.nebula.example.crawler.entity.dto.BrowserCrawlRequestDto;
import io.nebula.example.crawler.entity.dto.CrawlRequestDto;
import io.nebula.example.crawler.service.CrawlerDemoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 爬虫演示控制器
 */
@Slf4j
@RestController
@RequestMapping("/crawler")
@RequiredArgsConstructor
public class CrawlerController {

    private final CrawlerDemoService crawlerDemoService;

    /**
     * 单页爬取（HTTP 引擎）
     */
    @PostMapping("/crawl")
    public Result<Map<String, Object>> crawl(@RequestBody CrawlRequestDto dto) {
        return Result.success(crawlerDemoService.crawl(dto));
    }

    /**
     * 批量爬取（HTTP 引擎）
     */
    @PostMapping("/batch")
    public Result<List<Map<String, Object>>> crawlBatch(@RequestBody BatchCrawlRequestDto dto) {
        return Result.success(crawlerDemoService.crawlBatch(dto));
    }

    /**
     * 爬取并解析（提取标题、链接、描述）
     */
    @GetMapping("/parse")
    public Result<Map<String, Object>> crawlAndParse(@RequestParam String url) {
        return Result.success(crawlerDemoService.crawlAndParse(url));
    }

    /**
     * 浏览器引擎爬取（支持 JS 渲染的动态页面）
     * 通过远程 Playwright Server 执行，适用于需要 JavaScript 渲染的页面
     */
    @PostMapping("/browser")
    public Result<Map<String, Object>> browserCrawl(@RequestBody BrowserCrawlRequestDto dto) {
        return Result.success(crawlerDemoService.browserCrawl(dto));
    }

    /**
     * 引擎健康检查（所有引擎）
     */
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        return Result.success(crawlerDemoService.healthCheck());
    }
}
