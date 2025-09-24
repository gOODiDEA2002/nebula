package io.nebula.ai.core.vectorstore;

import io.nebula.ai.core.model.Document;
import io.nebula.ai.core.model.SearchRequest;
import io.nebula.ai.core.model.SearchResult;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 向量存储服务接口
 * 提供文档向量化存储和相似性搜索的统一抽象
 */
public interface VectorStoreService {

    /**
     * 添加单个文档到向量存储
     *
     * @param document 文档
     * @return 是否添加成功
     */
    boolean add(Document document);

    /**
     * 批量添加文档到向量存储
     *
     * @param documents 文档列表
     * @return 成功添加的文档数量
     */
    int addAll(List<Document> documents);

    /**
     * 异步添加单个文档
     *
     * @param document 文档
     * @return 异步添加结果
     */
    CompletableFuture<Boolean> addAsync(Document document);

    /**
     * 异步批量添加文档
     *
     * @param documents 文档列表
     * @return 异步添加结果
     */
    CompletableFuture<Integer> addAllAsync(List<Document> documents);

    /**
     * 根据ID获取文档
     *
     * @param id 文档ID
     * @return 文档，如果不存在返回null
     */
    Document get(String id);

    /**
     * 根据ID列表批量获取文档
     *
     * @param ids 文档ID列表
     * @return 文档列表
     */
    List<Document> getAll(List<String> ids);

    /**
     * 根据ID删除文档
     *
     * @param id 文档ID
     * @return 是否删除成功
     */
    boolean delete(String id);

    /**
     * 根据ID列表批量删除文档
     *
     * @param ids 文档ID列表
     * @return 成功删除的文档数量
     */
    int deleteAll(List<String> ids);

    /**
     * 根据过滤条件删除文档
     *
     * @param filter 过滤条件
     * @return 删除的文档数量
     */
    int deleteByFilter(Map<String, Object> filter);

    /**
     * 更新文档
     *
     * @param document 文档
     * @return 是否更新成功
     */
    boolean update(Document document);

    /**
     * 执行相似性搜索
     *
     * @param query 查询文本
     * @param topK 返回的最大结果数
     * @return 搜索结果
     */
    SearchResult search(String query, int topK);

    /**
     * 执行相似性搜索（带相似度阈值）
     *
     * @param query 查询文本
     * @param topK 返回的最大结果数
     * @param similarityThreshold 相似度阈值
     * @return 搜索结果
     */
    SearchResult search(String query, int topK, double similarityThreshold);

    /**
     * 执行相似性搜索（带过滤条件）
     *
     * @param query 查询文本
     * @param topK 返回的最大结果数
     * @param filter 过滤条件
     * @return 搜索结果
     */
    SearchResult search(String query, int topK, Map<String, Object> filter);

    /**
     * 执行相似性搜索
     *
     * @param request 搜索请求
     * @return 搜索结果
     */
    SearchResult search(SearchRequest request);

    /**
     * 异步执行相似性搜索
     *
     * @param query 查询文本
     * @param topK 返回的最大结果数
     * @return 异步搜索结果
     */
    CompletableFuture<SearchResult> searchAsync(String query, int topK);

    /**
     * 异步执行相似性搜索
     *
     * @param request 搜索请求
     * @return 异步搜索结果
     */
    CompletableFuture<SearchResult> searchAsync(SearchRequest request);

    /**
     * 根据向量执行搜索
     *
     * @param vector 查询向量
     * @param topK 返回的最大结果数
     * @return 搜索结果
     */
    SearchResult searchByVector(List<Double> vector, int topK);

    /**
     * 根据向量执行搜索（带过滤条件）
     *
     * @param vector 查询向量
     * @param topK 返回的最大结果数
     * @param filter 过滤条件
     * @return 搜索结果
     */
    SearchResult searchByVector(List<Double> vector, int topK, Map<String, Object> filter);

    /**
     * 检查文档是否存在
     *
     * @param id 文档ID
     * @return 是否存在
     */
    boolean exists(String id);

    /**
     * 获取存储的文档总数
     *
     * @return 文档总数
     */
    long count();

    /**
     * 根据过滤条件获取文档数量
     *
     * @param filter 过滤条件
     * @return 文档数量
     */
    long count(Map<String, Object> filter);

    /**
     * 清空所有文档
     *
     * @return 是否清空成功
     */
    boolean clear();

    /**
     * 检查服务是否可用
     *
     * @return 是否可用
     */
    boolean isAvailable();

    /**
     * 获取集合/索引名称
     *
     * @return 集合名称
     */
    String getCollectionName();

    /**
     * 创建集合/索引
     *
     * @param dimension 向量维度
     * @return 是否创建成功
     */
    boolean createCollection(int dimension);

    /**
     * 删除集合/索引
     *
     * @return 是否删除成功
     */
    boolean deleteCollection();

    /**
     * 检查集合/索引是否存在
     *
     * @return 是否存在
     */
    boolean collectionExists();
}
