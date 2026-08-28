package io.nebula.ai.spring.vectorstore;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.beans.factory.InitializingBean;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 给 Qdrant 后端补齐点 ID 映射的 {@link VectorStore} 装饰器
 * <p>
 * 只包在 Qdrant 实现外面，Chroma 等其他后端与 {@code VectorStoreService} 抽象层都不受影响 ——
 * 字符串 ID 是 Qdrant 独有的限制，不该污染其他实现。
 * <p>
 * 三条路径必须成对映射，缺一条就会出现「写进去了却删不掉 / 查回来对不上」：
 * <ul>
 *   <li>写入：Document ID 换成 {@link QdrantPointIdMapper#toPointId} 的 UUID，
 *       原始 docId 落进 payload 字段（默认
 *       {@link QdrantPointIdMapper#DEFAULT_ORIGINAL_DOC_ID_FIELD}）；</li>
 *   <li>按 ID 删除：同一映射换算后再交给 Qdrant；</li>
 *   <li>检索：把点 ID 还原成原始 docId 再返回上游。</li>
 * </ul>
 * <b>映射是全量且对称的，没有「看起来像 UUID 就直接放行」的例外。</b>
 * 留例外会踩这个坑：分块 ID 本身可能就是 UUID 形态，写入时被映射成另一个 UUID，
 * 删除时却按原样放行，于是删了个不存在的点、真数据留在库里。
 * <p>
 * 按 filter 删除（{@link #delete(Filter.Expression)}）直接透传：它作用在 payload 上，与点 ID 无关。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class QdrantIdMappingVectorStore implements VectorStore, InitializingBean {

    private final VectorStore delegate;
    private final QdrantPointIdMapper pointIdMapper;
    private final String originalDocIdField;

    public QdrantIdMappingVectorStore(VectorStore delegate, QdrantPointIdMapper pointIdMapper) {
        this(delegate, pointIdMapper, QdrantPointIdMapper.DEFAULT_ORIGINAL_DOC_ID_FIELD);
    }

    public QdrantIdMappingVectorStore(VectorStore delegate, QdrantPointIdMapper pointIdMapper,
                                      String originalDocIdField) {
        if (delegate == null) {
            throw new IllegalArgumentException("被装饰的 VectorStore 不能为空");
        }
        if (pointIdMapper == null) {
            throw new IllegalArgumentException("QdrantPointIdMapper 不能为空");
        }
        if (originalDocIdField == null || originalDocIdField.isBlank()) {
            throw new IllegalArgumentException("原始 docId 的 payload 字段名不能为空");
        }
        this.delegate = delegate;
        this.pointIdMapper = pointIdMapper;
        this.originalDocIdField = originalDocIdField;
    }

    /**
     * {@code QdrantVectorStore} 靠 {@code afterPropertiesSet} 建集合，
     * 它被包在里面就不再是 Spring Bean，生命周期回调必须由装饰器转发
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        if (delegate instanceof InitializingBean initializingBean) {
            initializingBean.afterPropertiesSet();
        }
    }

    @Override
    public void add(List<Document> documents) {
        delegate.add(documents.stream().map(this::toPointDocument).toList());
    }

    @Override
    public void delete(List<String> idList) {
        delegate.delete(idList.stream().map(pointIdMapper::toPointId).toList());
    }

    @Override
    public void delete(Filter.Expression filterExpression) {
        delegate.delete(filterExpression);
    }

    @Override
    public List<Document> similaritySearch(SearchRequest request) {
        List<Document> results = delegate.similaritySearch(request);
        if (results == null) {
            return List.of();
        }
        return results.stream().map(this::restoreDocId).toList();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public <T> Optional<T> getNativeClient() {
        return delegate.getNativeClient();
    }

    /** 供装配与测试确认包着的是谁 */
    public VectorStore getDelegate() {
        return delegate;
    }

    /** 原始 docId 的 payload 字段名 */
    public String getOriginalDocIdField() {
        return originalDocIdField;
    }

    private Document toPointDocument(Document document) {
        Map<String, Object> metadata = document.getMetadata() != null
                ? new HashMap<>(document.getMetadata()) : new HashMap<>();
        metadata.put(originalDocIdField, document.getId());
        return document.mutate()
                .id(pointIdMapper.toPointId(document.getId()))
                .metadata(metadata)
                .build();
    }

    /**
     * 用 payload 里的原始 docId 还原 Document ID
     * <p>
     * 取不到（映射上线前写入的历史点）时保留点 ID 原样返回，检索结果不因此丢失。
     */
    private Document restoreDocId(Document document) {
        Object original = document.getMetadata() != null
                ? document.getMetadata().get(originalDocIdField) : null;
        if (original instanceof String docId && !docId.isBlank()) {
            return document.mutate().id(docId).build();
        }
        return document;
    }
}
