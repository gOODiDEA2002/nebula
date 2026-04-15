package io.nebula.example.crawler.entity.dto;

import lombok.Data;

import java.util.Map;

/**
 * 浏览器爬取请求 DTO
 */
@Data
public class BrowserCrawlRequestDto {

    /**
     * 目标 URL
     */
    private String url;

    /**
     * 等待指定 CSS 选择器出现后再获取内容
     */
    private String waitSelector;

    /**
     * 页面加载等待策略: commit, load, domcontentloaded, networkidle
     */
    private String waitUntil;

    /**
     * 等待超时（毫秒）
     */
    private Integer waitTimeout;

    /**
     * 是否截图
     */
    private boolean screenshot = false;

    /**
     * 额外请求头
     */
    private Map<String, String> headers;
}
