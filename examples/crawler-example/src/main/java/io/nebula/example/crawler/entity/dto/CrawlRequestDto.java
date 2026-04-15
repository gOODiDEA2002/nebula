package io.nebula.example.crawler.entity.dto;

import lombok.Data;

import java.util.Map;

/**
 * 爬取请求DTO
 */
@Data
public class CrawlRequestDto {
    /**
     * 目标URL
     */
    private String url;

    /**
     * HTTP方法（GET / POST），默认 GET
     */
    private String method;

    /**
     * 自定义请求头
     */
    private Map<String, String> headers;

    /**
     * 请求体（POST 时使用）
     */
    private String body;

    /**
     * 超时时间（毫秒），默认 30000
     */
    private Integer timeout;
}
