package io.nebula.mcp.core.search;

import java.util.List;
import java.util.Map;

/**
 * 文档搜索服务接口
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
public interface DocumentSearchService {
    
    /**
     * 语义搜索
     * 
     * @param query 搜索查询
     * @param topK 返回结果数量
     * @return 搜索结果列表
     */
    List<SearchResult> search(String query, int topK);
    
    /**
     * 带过滤的语义搜索
     * 
     * @param query 搜索查询
     * @param topK 返回结果数量
     * @param filters 过滤条件
     * @return 搜索结果列表
     */
    List<SearchResult> search(String query, int topK, Map<String, String> filters);
    
    /**
     * 搜索结果
     */
    record SearchResult(
            String content,
            float score,
            Map<String, Object> metadata
    ) {}
}

