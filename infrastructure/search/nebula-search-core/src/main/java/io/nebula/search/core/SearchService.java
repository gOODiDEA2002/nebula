package io.nebula.search.core;

import io.nebula.search.core.model.*;
import io.nebula.search.core.query.*;

import java.util.List;
import java.util.Map;

/**
 * 统一搜索服务接口
 * 提供搜索引擎的基础操作抽象
 */
public interface SearchService {
    
    /**
     * 创建索引
     * 
     * @param indexName 索引名称
     * @param mapping 索引映射
     * @return 创建结果
     */
    IndexResult createIndex(String indexName, IndexMapping mapping);
    
    /**
     * 删除索引
     * 
     * @param indexName 索引名称
     * @return 删除结果
     */
    IndexResult deleteIndex(String indexName);
    
    /**
     * 检查索引是否存在
     * 
     * @param indexName 索引名称
     * @return 是否存在
     */
    boolean indexExists(String indexName);
    
    /**
     * 获取索引信息
     * 
     * @param indexName 索引名称
     * @return 索引信息
     */
    IndexInfo getIndexInfo(String indexName);
    
    /**
     * 索引文档
     * 
     * @param indexName 索引名称
     * @param id 文档ID
     * @param document 文档对象
     * @return 索引结果
     */
    <T> DocumentResult indexDocument(String indexName, String id, T document);
    
    /**
     * 批量索引文档
     * 
     * @param indexName 索引名称
     * @param documents 文档列表
     * @return 批量操作结果
     */
    <T> BulkResult bulkIndexDocuments(String indexName, Map<String, T> documents);
    
    /**
     * 更新文档
     * 
     * @param indexName 索引名称
     * @param id 文档ID
     * @param document 文档对象
     * @return 更新结果
     */
    <T> DocumentResult updateDocument(String indexName, String id, T document);
    
    /**
     * 部分更新文档
     * 
     * @param indexName 索引名称
     * @param id 文档ID
     * @param partialDocument 部分文档
     * @return 更新结果
     */
    DocumentResult updateDocumentPartial(String indexName, String id, Map<String, Object> partialDocument);
    
    /**
     * 删除文档
     * 
     * @param indexName 索引名称
     * @param id 文档ID
     * @return 删除结果
     */
    DocumentResult deleteDocument(String indexName, String id);
    
    /**
     * 批量删除文档
     * 
     * @param indexName 索引名称
     * @param ids 文档ID列表
     * @return 批量删除结果
     */
    BulkResult bulkDeleteDocuments(String indexName, List<String> ids);
    
    /**
     * 根据ID获取文档
     * 
     * @param indexName 索引名称
     * @param id 文档ID
     * @param clazz 文档类型
     * @return 文档
     */
    <T> T getDocument(String indexName, String id, Class<T> clazz);
    
    /**
     * 搜索文档
     * 
     * @param query 搜索查询
     * @param clazz 文档类型
     * @return 搜索结果
     */
    <T> SearchResult<T> search(SearchQuery query, Class<T> clazz);
    
    /**
     * 使用搜索模板
     * 
     * @param templateName 模板名称
     * @param params 模板参数
     * @param clazz 文档类型
     * @return 搜索结果
     */
    <T> SearchResult<T> searchTemplate(String templateName, Map<String, Object> params, Class<T> clazz);
    
    /**
     * 聚合查询
     * 
     * @param query 聚合查询
     * @return 聚合结果
     */
    AggregationResult aggregate(AggregationQuery query);
    
    /**
     * 搜索建议
     * 
     * @param query 建议查询
     * @return 建议结果
     */
    SuggestResult suggest(SuggestQuery query);
    
    /**
     * 多重搜索
     * 
     * @param queries 查询列表
     * @return 多重搜索结果
     */
    List<SearchResult<?>> multiSearch(List<SearchQuery> queries);
    
    /**
     * 计数查询
     * 
     * @param query 查询条件
     * @return 文档数量
     */
    long count(SearchQuery query);
    
    /**
     * 滚动搜索开始
     * 
     * @param query 搜索查询
     * @param clazz 文档类型
     * @return 滚动搜索结果
     */
    <T> ScrollResult<T> scrollSearch(SearchQuery query, Class<T> clazz);
    
    /**
     * 滚动搜索继续
     * 
     * @param scrollId 滚动ID
     * @param clazz 文档类型
     * @return 滚动搜索结果
     */
    <T> ScrollResult<T> scroll(String scrollId, Class<T> clazz);
    
    /**
     * 清除滚动
     * 
     * @param scrollId 滚动ID
     */
    void clearScroll(String scrollId);
    
    /**
     * 刷新索引
     * 
     * @param indexName 索引名称
     */
    void refresh(String indexName);
    
    /**
     * 刷新所有索引
     */
    void refreshAll();
}
