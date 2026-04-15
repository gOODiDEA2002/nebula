package io.nebula.example.crawler.service.impl;

import io.nebula.crawler.browser.BrowserCrawlerEngine;
import io.nebula.crawler.core.CrawlerRequest;
import io.nebula.crawler.core.CrawlerResponse;
import io.nebula.crawler.core.HttpMethod;
import io.nebula.crawler.http.HttpCrawlerEngine;
import io.nebula.example.crawler.entity.dto.BatchCrawlRequestDto;
import io.nebula.example.crawler.entity.dto.BrowserCrawlRequestDto;
import io.nebula.example.crawler.entity.dto.CrawlRequestDto;
import io.nebula.example.crawler.service.CrawlerDemoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 爬虫演示服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlerDemoServiceImpl implements CrawlerDemoService {

    private final HttpCrawlerEngine crawlerEngine;

    @Autowired(required = false)
    private BrowserCrawlerEngine browserCrawlerEngine;

    @Override
    public Map<String, Object> crawl(CrawlRequestDto dto) {
        log.info("[爬虫演示] 单页爬取: url={}", dto.getUrl());

        CrawlerRequest.CrawlerRequestBuilder builder = CrawlerRequest.builder()
                .url(dto.getUrl())
                .method("POST".equalsIgnoreCase(dto.getMethod()) ? HttpMethod.POST : HttpMethod.GET);

        if (dto.getHeaders() != null) {
            builder.headers(dto.getHeaders());
        }
        if (dto.getBody() != null) {
            builder.body(dto.getBody());
        }
        if (dto.getTimeout() != null) {
            builder.connectTimeout(dto.getTimeout()).readTimeout(dto.getTimeout());
        }

        CrawlerResponse response = crawlerEngine.crawl(builder.build());
        return buildResponseMap(response);
    }

    @Override
    public List<Map<String, Object>> crawlBatch(BatchCrawlRequestDto dto) {
        log.info("[爬虫演示] 批量爬取: {} 个URL", dto.getUrls().size());

        List<CrawlerRequest> requests = dto.getUrls().stream()
                .map(url -> {
                    CrawlerRequest.CrawlerRequestBuilder b = CrawlerRequest.builder().url(url);
                    if (dto.getTimeout() != null) {
                        b.connectTimeout(dto.getTimeout()).readTimeout(dto.getTimeout());
                    }
                    return b.build();
                })
                .collect(Collectors.toList());

        List<CrawlerResponse> responses = crawlerEngine.crawlBatch(requests);
        return responses.stream()
                .map(this::buildResponseMap)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> crawlAndParse(String url) {
        log.info("[爬虫演示] 爬取并解析: url={}", url);

        CrawlerResponse response = crawlerEngine.crawl(CrawlerRequest.get(url));
        Map<String, Object> result = buildResponseMap(response);

        if (response.isSuccess() && response.hasContent()) {
            Document doc = response.asDocument();

            result.put("title", doc.title());

            Elements links = doc.select("a[href]");
            List<Map<String, String>> linkList = new ArrayList<>();
            int limit = Math.min(links.size(), 20);
            for (int i = 0; i < limit; i++) {
                Element link = links.get(i);
                linkList.add(Map.of(
                        "text", link.text(),
                        "href", link.attr("abs:href")
                ));
            }
            result.put("links", linkList);
            result.put("totalLinks", links.size());

            Elements metas = doc.select("meta[name=description], meta[property=og:description]");
            if (!metas.isEmpty()) {
                result.put("description", metas.first().attr("content"));
            }
        }

        return result;
    }

    @Override
    public Map<String, Object> browserCrawl(BrowserCrawlRequestDto dto) {
        if (browserCrawlerEngine == null) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("success", false);
            error.put("errorMessage", "浏览器引擎未启用。请配置 nebula.crawler.browser.enabled=true 并确保远程 Playwright Server 可用");
            return error;
        }

        log.info("[浏览器爬虫] 爬取: url={}, waitSelector={}, waitUntil={}",
                dto.getUrl(), dto.getWaitSelector(), dto.getWaitUntil());

        CrawlerRequest.CrawlerRequestBuilder builder = CrawlerRequest.builder()
                .url(dto.getUrl())
                .renderJs(true);

        if (dto.getWaitSelector() != null) {
            builder.waitSelector(dto.getWaitSelector());
        }
        if (dto.getWaitUntil() != null) {
            builder.waitUntil(dto.getWaitUntil());
        }
        if (dto.getWaitTimeout() != null) {
            builder.waitTimeout(dto.getWaitTimeout());
        }
        if (dto.getHeaders() != null) {
            builder.headers(dto.getHeaders());
        }
        builder.screenshot(dto.isScreenshot());

        CrawlerResponse response = browserCrawlerEngine.crawl(builder.build());
        Map<String, Object> result = buildResponseMap(response);

        result.put("engineType", "BROWSER");
        if (response.isSuccess() && response.hasContent()) {
            Document doc = response.asDocument();
            result.put("title", doc.title());
        }
        if (response.getScreenshot() != null) {
            result.put("screenshotSize", response.getScreenshot().length + " bytes");
        }

        return result;
    }

    @Override
    public Map<String, Object> healthCheck() {
        Map<String, Object> result = new LinkedHashMap<>();

        result.put("httpEngine", Map.of(
                "type", crawlerEngine.getType().name(),
                "healthy", crawlerEngine.isHealthy()
        ));

        if (browserCrawlerEngine != null) {
            result.put("browserEngine", Map.of(
                    "type", browserCrawlerEngine.getType().name(),
                    "healthy", browserCrawlerEngine.isHealthy(),
                    "poolStats", browserCrawlerEngine.getPoolStats()
            ));
        } else {
            result.put("browserEngine", Map.of(
                    "available", false,
                    "message", "浏览器引擎未启用"
            ));
        }

        return result;
    }

    private Map<String, Object> buildResponseMap(CrawlerResponse response) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("requestId", response.getRequestId());
        map.put("url", response.getUrl());
        map.put("success", response.isSuccess());
        map.put("statusCode", response.getStatusCode());
        map.put("responseTime", response.getResponseTime() + "ms");
        map.put("contentLength", response.getContentLength());

        if (!response.isSuccess()) {
            map.put("errorMessage", response.getErrorMessage());
        }

        if (response.isSuccess() && response.hasContent()) {
            String content = response.getContent();
            map.put("contentPreview", content.length() > 500
                    ? content.substring(0, 500) + "..."
                    : content);
        }

        return map;
    }
}
