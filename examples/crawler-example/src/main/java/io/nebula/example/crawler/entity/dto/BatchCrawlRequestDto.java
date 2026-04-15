package io.nebula.example.crawler.entity.dto;

import lombok.Data;

import java.util.List;

/**
 * 批量爬取请求DTO
 */
@Data
public class BatchCrawlRequestDto {
    /**
     * 目标URL列表
     */
    private List<String> urls;

    /**
     * 超时时间（毫秒）
     */
    private Integer timeout;
}
