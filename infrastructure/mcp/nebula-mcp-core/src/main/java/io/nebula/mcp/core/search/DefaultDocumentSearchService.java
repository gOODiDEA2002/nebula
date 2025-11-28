package io.nebula.mcp.core.search;

import io.nebula.mcp.core.config.McpProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 默认文档搜索服务实现
 * 
 * 这是一个基础实现，实际项目中应该替换为基于向量数据库的实现
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultDocumentSearchService implements DocumentSearchService {
    
    private final McpProperties properties;
    
    @Override
    public List<SearchResult> search(String query, int topK) {
        return search(query, topK, Collections.emptyMap());
    }
    
    @Override
    public List<SearchResult> search(String query, int topK, Map<String, String> filters) {
        log.debug("搜索文档: query={}, topK={}, filters={}", query, topK, filters);
        
        // 默认实现返回空结果
        // 实际项目中应该接入向量数据库（如 Chroma, Milvus, Pinecone 等）
        log.warn("使用默认搜索服务，返回空结果。请实现 DocumentSearchService 接口并接入向量数据库。");
        
        return Collections.emptyList();
    }
}

