package io.nebula.search.elasticsearch.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.search.*;
import co.elastic.clients.elasticsearch.indices.*;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import io.nebula.search.core.SearchService;
import io.nebula.search.core.model.*;
import io.nebula.search.core.query.AggregationQuery;
import io.nebula.search.core.query.SearchQuery;
import io.nebula.search.core.query.SuggestQuery;
import io.nebula.search.elasticsearch.config.ElasticsearchProperties;
import io.nebula.search.elasticsearch.converter.AggregationConverter;
import io.nebula.search.elasticsearch.converter.QueryConverter;
import io.nebula.search.elasticsearch.converter.SuggesterConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Elasticsearch 搜索服务实现
 *
 * @author nebula
 */
public class ElasticsearchSearchService implements SearchService {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchSearchService.class);

    private final ElasticsearchClient client;
    private final ElasticsearchProperties properties;

    public ElasticsearchSearchService(ElasticsearchClient client,
                                      ElasticsearchProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public IndexResult createIndex(String indexName, IndexMapping mapping) {
        try {
            String actualIndexName = getActualIndexName(indexName);

            // mapping.properties / mapping.settings 经「Map -> JSON -> withJson」通用转换下发；
            // 两者皆空时走现状路径（仅默认分片/副本），行为与改动前完全一致。
            TypeMapping typeMapping = buildTypeMapping(indexName, mapping);
            IndexSettings indexSettings = buildIndexSettings(indexName, mapping);

            CreateIndexResponse response = client.indices().create(c -> {
                c.index(actualIndexName);
                if (typeMapping != null) {
                    c.mappings(typeMapping);
                }
                if (indexSettings != null) {
                    c.settings(indexSettings);
                } else {
                    c.settings(s -> s
                        .numberOfShards(String.valueOf(properties.getDefaultShards()))
                        .numberOfReplicas(String.valueOf(properties.getDefaultReplicas()))
                    );
                }
                return c;
            });

            return IndexResult.success(actualIndexName);

        } catch (Exception e) {
            logger.error("Failed to create index: {}", e.getMessage(), e);
            return IndexResult.error(indexName, e.getMessage());
        }
    }

    /**
     * 由 {@code mapping.properties} 构建 {@link TypeMapping}
     * <p>
     * {@code IndexMapping.properties} 承载的是 mappings 的 JSON 主体（含顶层 {@code properties} 键，
     * 与既有调用方一致）。为空返回 null（不设 mappings）。转换失败时包装为含 indexName 的明确异常。
     */
    private TypeMapping buildTypeMapping(String indexName, IndexMapping mapping) {
        if (mapping == null || mapping.getProperties() == null || mapping.getProperties().isEmpty()) {
            return null;
        }
        return fromMap(indexName, "mappings", mapping.getProperties(), TypeMapping._DESERIALIZER);
    }

    /**
     * 由 {@code mapping.settings} 构建 {@link IndexSettings}，并与默认分片/副本合并（用户键覆盖默认）
     * <p>
     * settings 为空返回 null，交由调用方走现状默认路径。
     */
    private IndexSettings buildIndexSettings(String indexName, IndexMapping mapping) {
        if (mapping == null || mapping.getSettings() == null || mapping.getSettings().isEmpty()) {
            return null;
        }
        Map<String, Object> merged = new LinkedHashMap<>();
        merged.put("number_of_shards", String.valueOf(properties.getDefaultShards()));
        merged.put("number_of_replicas", String.valueOf(properties.getDefaultReplicas()));
        merged.putAll(mapping.getSettings());
        return fromMap(indexName, "settings", merged, IndexSettings._DESERIALIZER);
    }

    /**
     * 把一个 {@code Map} 序列化为 JSON 再用 ES 客户端反序列化成目标类型
     * <p>
     * 走客户端自带的 {@code JsonpMapper}，无需引入额外 JSON 依赖；转换失败时抛出含 indexName、
     * 片段名与原始片段的明确异常，避免 withJson 对畸形 Map 报出不可读的错误。
     */
    private <T> T fromMap(String indexName, String section, Map<String, Object> map,
                          co.elastic.clients.json.JsonpDeserializer<T> deserializer) {
        co.elastic.clients.json.JsonpMapper jsonpMapper = client._jsonpMapper();
        try {
            java.io.StringWriter writer = new java.io.StringWriter();
            try (jakarta.json.stream.JsonGenerator generator =
                         jsonpMapper.jsonProvider().createGenerator(writer)) {
                jsonpMapper.serialize(map, generator);
            }
            try (jakarta.json.stream.JsonParser parser =
                         jsonpMapper.jsonProvider().createParser(new StringReader(writer.toString()))) {
                return deserializer.deserialize(parser, jsonpMapper);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "索引 " + indexName + " 的 " + section + " 转换失败: " + e.getMessage()
                            + "; 原始片段=" + map, e);
        }
    }

    @Override
    public IndexResult deleteIndex(String indexName) {
        try {
            String actualIndexName = getActualIndexName(indexName);
            
            DeleteIndexResponse response = client.indices().delete(d -> d.index(actualIndexName));
            return IndexResult.success(actualIndexName);
            
        } catch (Exception e) {
            logger.error("Failed to delete index: {}", e.getMessage(), e);
            return IndexResult.error(indexName, e.getMessage());
        }
    }

    @Override
    public boolean indexExists(String indexName) {
        try {
            String actualIndexName = getActualIndexName(indexName);
            
            BooleanResponse response = client.indices().exists(e -> e.index(actualIndexName));
            return response.value();
            
        } catch (Exception e) {
            logger.error("Failed to check index existence: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public IndexInfo getIndexInfo(String indexName) {
        try {
            String actualIndexName = getActualIndexName(indexName);
            
            GetIndexResponse response = client.indices().get(g -> g.index(actualIndexName));
            
            IndexInfo indexInfo = new IndexInfo();
            indexInfo.setIndexName(actualIndexName);
            // 这里需要根据实际响应设置其他属性
            return indexInfo;
            
        } catch (Exception e) {
            logger.error("Failed to get index info: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public <T> DocumentResult indexDocument(String indexName, String id, T document) {
        try {
            String actualIndexName = getActualIndexName(indexName);
            
            IndexResponse response = client.index(i -> i
                .index(actualIndexName)
                .id(id)
                .document(document));
            
            DocumentResult result = new DocumentResult();
            result.setSuccess(true);
            result.setIndex(actualIndexName);
            result.setId(response.id());
            return result;
            
        } catch (Exception e) {
            logger.error("Failed to index document: {}", e.getMessage(), e);
            DocumentResult result = new DocumentResult();
            result.setSuccess(false);
            result.setIndex(indexName);
            result.setId(id);
            result.setErrorMessage(e.getMessage());
            return result;
        }
    }

    @Override
    public <T> BulkResult bulkIndexDocuments(String indexName, Map<String, T> documents) {
        try {
            String actualIndexName = getActualIndexName(indexName);
            
            List<BulkOperation> operations = documents.entrySet().stream()
                .map(entry -> BulkOperation.of(o -> o
                    .index(i -> i
                        .index(actualIndexName)
                        .id(entry.getKey())
                        .document(entry.getValue())
                    )
                ))
                .collect(Collectors.toList());
            
            BulkResponse response = client.bulk(b -> b
                .operations(operations)
                .timeout(t -> t.time(properties.getBulkTimeout().toMillis() + "ms")));
            
            BulkResult result = new BulkResult();
            result.setTotalCount(response.items().size());
            result.setSuccessCount((int) response.items().stream()
                .filter(item -> item.error() == null).count());
            result.setFailureCount((int) response.items().stream()
                .filter(item -> item.error() != null).count());
            result.setErrors(response.items().stream()
                .filter(item -> item.error() != null)
                .map(item -> item.error().reason())
                .collect(Collectors.toList()));
            result.setSuccess(true);
            return result;
            
        } catch (Exception e) {
            logger.error("Failed to bulk index: {}", e.getMessage(), e);
            BulkResult result = new BulkResult();
            result.setTotalCount(documents.size());
            result.setSuccessCount(0);
            result.setFailureCount(documents.size());
            result.setErrors(List.of(e.getMessage()));
            result.setSuccess(false);
            return result;
        }
    }

    @Override
    public <T> DocumentResult updateDocument(String indexName, String id, T document) {
        try {
            String actualIndexName = getActualIndexName(indexName);
            
            UpdateResponse<T> response = client.update(u -> u
                .index(actualIndexName)
                .id(id)
                .doc(document), (Class<T>) document.getClass());
            
            DocumentResult result = new DocumentResult();
            result.setSuccess(true);
            result.setIndex(actualIndexName);
            result.setId(response.id());
            return result;
            
        } catch (Exception e) {
            logger.error("Failed to update document: {}", e.getMessage(), e);
            DocumentResult result = new DocumentResult();
            result.setSuccess(false);
            result.setIndex(indexName);
            result.setId(id);
            result.setErrorMessage(e.getMessage());
            return result;
        }
    }

    @Override
    public DocumentResult updateDocumentPartial(String indexName, String id, Map<String, Object> partialDocument) {
        try {
            String actualIndexName = getActualIndexName(indexName);
            
            UpdateResponse<Map> response = client.update(u -> u
                .index(actualIndexName)
                .id(id)
                .doc(partialDocument), Map.class);
            
            DocumentResult result = new DocumentResult();
            result.setSuccess(true);
            result.setIndex(actualIndexName);
            result.setId(response.id());
            return result;
            
        } catch (Exception e) {
            logger.error("Failed to update document partial: {}", e.getMessage(), e);
            DocumentResult result = new DocumentResult();
            result.setSuccess(false);
            result.setIndex(indexName);
            result.setId(id);
            result.setErrorMessage(e.getMessage());
            return result;
        }
    }

    @Override
    public DocumentResult deleteDocument(String indexName, String id) {
        try {
            String actualIndexName = getActualIndexName(indexName);
            
            DeleteResponse response = client.delete(d -> d
                .index(actualIndexName)
                .id(id));
            
            DocumentResult result = new DocumentResult();
            result.setSuccess(true);
            result.setIndex(actualIndexName);
            result.setId(response.id());
            return result;
            
        } catch (Exception e) {
            logger.error("Failed to delete document: {}", e.getMessage(), e);
            DocumentResult result = new DocumentResult();
            result.setSuccess(false);
            result.setIndex(indexName);
            result.setId(id);
            result.setErrorMessage(e.getMessage());
            return result;
        }
    }

    @Override
    public BulkResult bulkDeleteDocuments(String indexName, List<String> ids) {
        try {
            String actualIndexName = getActualIndexName(indexName);
            
            List<BulkOperation> operations = ids.stream()
                .map(id -> BulkOperation.of(o -> o
                    .delete(d -> d
                        .index(actualIndexName)
                        .id(id)
                    )
                ))
                .collect(Collectors.toList());
            
            BulkResponse response = client.bulk(b -> b
                .operations(operations)
                .timeout(t -> t.time(properties.getBulkTimeout().toMillis() + "ms")));
            
            BulkResult result = new BulkResult();
            result.setTotalCount(response.items().size());
            result.setSuccessCount((int) response.items().stream()
                .filter(item -> item.error() == null).count());
            result.setFailureCount((int) response.items().stream()
                .filter(item -> item.error() != null).count());
            result.setErrors(response.items().stream()
                .filter(item -> item.error() != null)
                .map(item -> item.error().reason())
                .collect(Collectors.toList()));
            result.setSuccess(true);
            return result;
            
        } catch (Exception e) {
            logger.error("Failed to bulk delete: {}", e.getMessage(), e);
            BulkResult result = new BulkResult();
            result.setTotalCount(ids.size());
            result.setSuccessCount(0);
            result.setFailureCount(ids.size());
            result.setErrors(List.of(e.getMessage()));
            result.setSuccess(false);
            return result;
        }
    }

    @Override
    public <T> T getDocument(String indexName, String id, Class<T> clazz) {
        try {
            String actualIndexName = getActualIndexName(indexName);
            
            GetResponse<T> response = client.get(g -> g
                .index(actualIndexName)
                .id(id), clazz);
            
            if (response.found()) {
                return response.source();
            }
            return null;
            
        } catch (Exception e) {
            logger.error("Failed to get document: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public <T> SearchResult<T> search(SearchQuery query, Class<T> clazz) {
        try {
            String actualIndexName = getActualIndexName(query.getIndices()[0]);
            
            SearchResponse<T> response = client.search(s -> s
                .index(actualIndexName)
                .query(buildSearchQuery(query))
                .from(query.getFrom())
                .size(query.getSize()), clazz);
            
            SearchResult<T> result = new SearchResult<>();
            result.setTotalHits(response.hits().total() != null ? response.hits().total().value() : 0L);
            result.setMaxScore(response.hits().maxScore() != null ? response.hits().maxScore().doubleValue() : 0.0);
            result.setTook(response.took());
            result.setTimedOut(response.timedOut());
            result.setDocuments(response.hits().hits().stream()
                .map(hit -> new SearchDocument<T>(hit.id(), hit.source(), hit.score() != null ? hit.score().doubleValue() : 0.0))
                .collect(Collectors.toList()));
            result.setSuccess(true);
            return result;
            
        } catch (Exception e) {
            logger.error("Failed to search: {}", e.getMessage(), e);
            SearchResult<T> result = new SearchResult<>();
            result.setTotalHits(0L);
            result.setMaxScore(0.0);
            result.setDocuments(Collections.emptyList());
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            return result;
        }
    }

    @Override
    public <T> SearchResult<T> searchTemplate(String templateName, Map<String, Object> params, Class<T> clazz) {
        // 模板搜索暂不实现
        SearchResult<T> result = new SearchResult<>();
        result.setSuccess(false);
        result.setErrorMessage("Search template not implemented");
        return result;
    }

    @Override
    public AggregationResult aggregate(AggregationQuery query) {
        try {
            String actualIndexName = getActualIndexName(query.getIndices()[0]);
            
            // 构建聚合
            Map<String, Aggregation> aggregations = new HashMap<>();
            for (io.nebula.search.core.aggregation.Aggregation agg : query.getAggregations()) {
                Aggregation esAgg = AggregationConverter.convert(agg);
                if (esAgg != null) {
                    aggregations.put(agg.getName(), esAgg);
                }
            }
            
            // 构建查询条件（用于过滤聚合数据）
            Query filterQuery = query.getQueryBuilder() != null ? 
                QueryConverter.convert(query.getQueryBuilder()) : 
                QueryBuilders.matchAll().build()._toQuery();
            
            // 执行聚合查询
            SearchResponse<Void> response = client.search(s -> s
                .index(actualIndexName)
                .query(filterQuery)
                .aggregations(aggregations)
                .size(0), // 不返回文档，只返回聚合结果
                Void.class
            );
            
            // 解析聚合结果
            Map<String, Object> aggResults = AggregationConverter.parseAggregationResults(response.aggregations());
            
            AggregationResult result = new AggregationResult();
            result.setSuccess(true);
            result.setAggregations(aggResults);
            return result;
            
        } catch (Exception e) {
            logger.error("Failed to aggregate: {}", e.getMessage(), e);
            AggregationResult result = new AggregationResult();
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            result.setAggregations(new HashMap<>());
            return result;
        }
    }

    @Override
    public SuggestResult suggest(SuggestQuery query) {
        try {
            String actualIndexName = getActualIndexName(query.getIndices()[0]);
            
            // 转换建议器
            co.elastic.clients.elasticsearch.core.search.Suggester suggester = SuggesterConverter.convert(query.getSuggesters());
            
            if (suggester == null) {
                logger.warn("No valid suggester found in query");
                SuggestResult result = new SuggestResult();
                result.setSuccess(true);
                result.setSuggestions(Collections.emptyMap());
                return result;
            }
            
            // 执行建议查询
            SearchResponse<Void> response = client.search(s -> s
                .index(actualIndexName)
                .suggest(suggester)
                .size(0), // 不返回文档，只返回建议结果
                Void.class
            );
            
            // 解析建议结果
            Map<String, List<String>> suggestions = SuggesterConverter.parseSuggestionResults(response.suggest());
            
            SuggestResult result = new SuggestResult();
            result.setSuccess(true);
            result.setSuggestions(suggestions);
            return result;
            
        } catch (Exception e) {
            logger.error("Failed to suggest: {}", e.getMessage(), e);
            SuggestResult result = new SuggestResult();
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            result.setSuggestions(Collections.emptyMap());
            return result;
        }
    }

    @Override
    public List<SearchResult<?>> multiSearch(List<SearchQuery> queries) {
        // 多重搜索暂不实现
        return Collections.emptyList();
    }

    @Override
    public long count(SearchQuery query) {
        try {
            String actualIndexName = getActualIndexName(query.getIndices()[0]);
            
            CountResponse response = client.count(c -> c
                .index(actualIndexName)
                .query(buildSearchQuery(query)));
            
            return response.count();
            
        } catch (Exception e) {
            logger.error("Failed to count: {}", e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public <T> ScrollResult<T> scrollSearch(SearchQuery query, Class<T> clazz) {
        try {
            String actualIndexName = getActualIndexName(query.getIndices()[0]);
            
            SearchResponse<T> response = client.search(s -> s
                .index(actualIndexName)
                .query(buildSearchQuery(query))
                .from(query.getFrom())
                .size(query.getSize())
                .scroll(t -> t.time(properties.getScrollTimeout().toMillis() + "ms")), clazz);
            
            ScrollResult<T> result = new ScrollResult<>();
            result.setScrollId(response.scrollId());
            result.setDocuments(response.hits().hits().stream()
                .map(hit -> new SearchDocument<T>(hit.id(), hit.source(), hit.score() != null ? hit.score().doubleValue() : 0.0))
                .collect(Collectors.toList()));
            result.setHasNext(!result.getDocuments().isEmpty());
            result.setTotalHits(response.hits().total() != null ? response.hits().total().value() : 0L);
            result.setSuccess(true);
            return result;
            
        } catch (Exception e) {
            logger.error("Failed to scroll search: {}", e.getMessage(), e);
            ScrollResult<T> result = new ScrollResult<>();
            result.setScrollId(null);
            result.setDocuments(Collections.emptyList());
            result.setHasNext(false);
            result.setTotalHits(0L);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            return result;
        }
    }

    @Override
    public <T> ScrollResult<T> scroll(String scrollId, Class<T> clazz) {
        try {
            ScrollResponse<T> response = client.scroll(s -> s
                .scrollId(scrollId)
                .scroll(Time.of(t -> t.time(properties.getScrollTimeout().toMillis() + "ms"))), clazz);
            
            ScrollResult<T> result = new ScrollResult<>();
            result.setScrollId(response.scrollId());
            result.setDocuments(response.hits().hits().stream()
                .map(hit -> new SearchDocument<T>(hit.id(), hit.source(), hit.score() != null ? hit.score().doubleValue() : 0.0))
                .collect(Collectors.toList()));
            result.setHasNext(!result.getDocuments().isEmpty());
            result.setTotalHits(response.hits().total() != null ? response.hits().total().value() : 0L);
            result.setSuccess(true);
            return result;
            
        } catch (Exception e) {
            logger.error("Failed to scroll: {}", e.getMessage(), e);
            ScrollResult<T> result = new ScrollResult<>();
            result.setScrollId(scrollId);
            result.setDocuments(Collections.emptyList());
            result.setHasNext(false);
            result.setTotalHits(0L);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            return result;
        }
    }

    @Override
    public void clearScroll(String scrollId) {
        try {
            client.clearScroll(c -> c.scrollId(scrollId));
        } catch (Exception e) {
            logger.error("Failed to clear scroll: {}", e.getMessage(), e);
        }
    }

    @Override
    public void refresh(String indexName) {
        try {
            String actualIndexName = getActualIndexName(indexName);
            client.indices().refresh(r -> r.index(actualIndexName));
        } catch (Exception e) {
            logger.error("Failed to refresh index: {}", e.getMessage(), e);
        }
    }

    @Override
    public void refreshAll() {
        try {
            client.indices().refresh(r -> r.index("_all"));
        } catch (Exception e) {
            logger.error("Failed to refresh all indices: {}", e.getMessage(), e);
        }
    }

    /**
     * 把别名原子指向目标索引：在一次 {@code updateAliases} 请求内提交
     * 「remove 别名的旧指向（逐个）+ add 别名到新索引」。别名此前不存在时操作列表只含 add。
     * <p>
     * 别名与索引名均经 {@link #getActualIndexName} 前缀化，与建索引/删索引一致。
     */
    @Override
    public IndexResult switchAlias(String aliasName, String targetIndexName) {
        try {
            // resolveAlias 返回的是去前缀的逻辑名(与调用方传入口径一致)
            List<String> currentIndices = resolveAlias(aliasName);
            String actualAlias = getActualIndexName(aliasName);
            String actualTarget = getActualIndexName(targetIndexName);

            List<co.elastic.clients.elasticsearch.indices.update_aliases.Action> actions = new ArrayList<>();
            for (String current : currentIndices) {
                String actualCurrent = getActualIndexName(current);
                if (!actualCurrent.equals(actualTarget)) {
                    actions.add(co.elastic.clients.elasticsearch.indices.update_aliases.Action.of(
                            a -> a.remove(r -> r.index(actualCurrent).alias(actualAlias))));
                }
            }
            actions.add(co.elastic.clients.elasticsearch.indices.update_aliases.Action.of(
                    a -> a.add(add -> add.index(actualTarget).alias(actualAlias))));

            client.indices().updateAliases(u -> u.actions(actions));
            return IndexResult.success(actualAlias);
        } catch (Exception e) {
            logger.error("Failed to switch alias {} -> {}: {}", aliasName, targetIndexName,
                    e.getMessage(), e);
            return IndexResult.error(aliasName, e.getMessage());
        }
    }

    /**
     * 别名当前指向的索引名清单（去前缀，与调用方传入口径一致）
     * <p>
     * 别名不存在时 ES 抛 404，此处捕获返回空列表（不把「没有别名」当作错误）。
     */
    @Override
    public List<String> resolveAlias(String aliasName) {
        try {
            String actualAlias = getActualIndexName(aliasName);
            co.elastic.clients.elasticsearch.indices.GetAliasResponse response =
                    client.indices().getAlias(g -> g.name(actualAlias));
            return response.aliases().keySet().stream()
                    .map(this::stripIndexPrefix)
                    .collect(Collectors.toList());
        } catch (co.elastic.clients.elasticsearch._types.ElasticsearchException e) {
            if (e.status() == 404) {
                // 别名不存在: 返回空列表, 不作为错误
                return List.of();
            }
            logger.error("Failed to resolve alias {}: {}", aliasName, e.getMessage(), e);
            return List.of();
        } catch (Exception e) {
            logger.error("Failed to resolve alias {}: {}", aliasName, e.getMessage(), e);
            return List.of();
        }
    }

    // 私有辅助方法

    private String getActualIndexName(String indexName) {
        if (properties.getIndexPrefix() != null && !properties.getIndexPrefix().isEmpty()) {
            return properties.getIndexPrefix() + "_" + indexName;
        }
        return indexName;
    }

    /**
     * 去掉 {@link #getActualIndexName} 加的前缀，还原调用方口径的逻辑名；无前缀时原样返回
     */
    private String stripIndexPrefix(String actualName) {
        String prefix = properties.getIndexPrefix();
        if (prefix != null && !prefix.isEmpty()) {
            String full = prefix + "_";
            if (actualName.startsWith(full)) {
                return actualName.substring(full.length());
            }
        }
        return actualName;
    }

    private Query buildSearchQuery(SearchQuery query) {
        if (query.getQueryBuilder() != null) {
            return QueryConverter.convert(query.getQueryBuilder());
        }
        return QueryBuilders.matchAll().build()._toQuery();
    }


}