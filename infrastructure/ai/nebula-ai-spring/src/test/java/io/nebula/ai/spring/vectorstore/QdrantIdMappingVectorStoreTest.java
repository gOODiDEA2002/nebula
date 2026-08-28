package io.nebula.ai.spring.vectorstore;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * ID 映射装饰器的三条路径（写入/删除/检索）必须成对
 * <p>
 * 替身 {@link UuidOnlyVectorStore} 复刻 Qdrant 的硬约束——点 ID 不是 UUID 就抛
 * {@code IllegalArgumentException: Invalid UUID string}，与生产事故的堆栈同型。
 * 替身不比真货宽松，去掉映射时这些用例必然报红，而不是靠替身放水侥幸通过。
 */
class QdrantIdMappingVectorStoreTest {

    private static final String MATERIAL_CODE_ID = "mc:0113120012";
    private static final String PRODUCT_ID = "product-1480334";
    private static final String ENTERPRISE_ID = "enterprise-123";

    private final QdrantPointIdMapper mapper = new QdrantPointIdMapper("vector.sia.vocoor.com");
    private final UuidOnlyVectorStore backend = new UuidOnlyVectorStore();
    private final QdrantIdMappingVectorStore store = new QdrantIdMappingVectorStore(backend, mapper);

    @Test
    void productionDocIdShapes_areWritableAndKeyedByMappedUuid() {
        List<Document> documents = List.of(
                doc(MATERIAL_CODE_ID, "空气开关"),
                doc(PRODUCT_ID, "义乌购商品"),
                doc(ENTERPRISE_ID, "供应商简介"));

        assertThatCode(() -> store.add(documents)).doesNotThrowAnyException();

        assertThat(backend.points.keySet()).containsExactlyInAnyOrder(
                mapper.toPointId(MATERIAL_CODE_ID),
                mapper.toPointId(PRODUCT_ID),
                mapper.toPointId(ENTERPRISE_ID));
    }

    @Test
    void originalDocId_isKeptInPayloadForRestoreAndOpsLookup() {
        store.add(List.of(doc(MATERIAL_CODE_ID, "空气开关")));

        Document stored = backend.points.get(mapper.toPointId(MATERIAL_CODE_ID));
        assertThat(stored.getMetadata())
                .containsEntry(QdrantPointIdMapper.DEFAULT_ORIGINAL_DOC_ID_FIELD, MATERIAL_CODE_ID)
                // 业务元数据不能被映射顺手弄丢，doc_type 一丢知识域路由整条哑掉
                .containsEntry("doc_type", "material_code");
    }

    @Test
    void rewritingSameDocId_upsertsOnePointInsteadOfDuplicating() {
        store.add(List.of(doc(MATERIAL_CODE_ID, "空气开关")));
        store.add(List.of(doc(MATERIAL_CODE_ID, "空气开关 修订版")));

        assertThat(backend.points).hasSize(1);
        assertThat(backend.points.values().iterator().next().getText()).isEqualTo("空气开关 修订版");
    }

    @Test
    void deleteByOriginalDocId_hitsTheMappedPoint() {
        store.add(List.of(doc(MATERIAL_CODE_ID, "空气开关"), doc(PRODUCT_ID, "义乌购商品")));

        assertThatCode(() -> store.delete(List.of(MATERIAL_CODE_ID))).doesNotThrowAnyException();

        assertThat(backend.points.keySet()).containsExactly(mapper.toPointId(PRODUCT_ID));
    }

    @Test
    void similaritySearch_restoresOriginalDocIdAndKeepsScore() {
        store.add(List.of(doc(MATERIAL_CODE_ID, "空气开关")));

        List<Document> results = store.similaritySearch(SearchRequest.builder().query("开关").build());

        assertThat(results).hasSize(1);
        // 还原 docId 是 RRF 融合按 ID 对齐关键词与向量两路的前提
        assertThat(results.get(0).getId()).isEqualTo(MATERIAL_CODE_ID);
        // 分数不能在改 ID 时被抹掉，否则重演「向量得分恒 0」那次回归
        assertThat(results.get(0).getScore()).isEqualTo(0.87);
        assertThat(results.get(0).getText()).isEqualTo("空气开关");
    }

    @Test
    void legacyPointWithoutOriginalDocId_keepsPointIdInsteadOfDisappearing() {
        String legacyPointId = UUID.randomUUID().toString();
        backend.points.put(legacyPointId, Document.builder()
                .id(legacyPointId).text("映射上线前写入的历史点").metadata(Map.of()).build());

        List<Document> results = store.similaritySearch(SearchRequest.builder().query("历史").build());

        assertThat(results).extracting(Document::getId).containsExactly(legacyPointId);
    }

    @Test
    void filterDelete_isPassedThroughUntouched() {
        Filter.Expression expression = new Filter.Expression(Filter.ExpressionType.EQ,
                new Filter.Key("doc_type"), new Filter.Value("material_code"));

        store.delete(expression);

        assertThat(backend.filterDeletes).containsExactly(expression);
    }

    /**
     * payload 字段名可配后，写入与还原必须成对使用同一个字段名，
     * 只改一半会表现为「查回来的 ID 还是 UUID」
     */
    @Test
    void customOriginalDocIdField_isUsedForBothStoreAndRestore() {
        UuidOnlyVectorStore customBackend = new UuidOnlyVectorStore();
        QdrantIdMappingVectorStore customStore =
                new QdrantIdMappingVectorStore(customBackend, mapper, "source_doc_id");

        customStore.add(List.of(doc(MATERIAL_CODE_ID, "空气开关")));

        Document stored = customBackend.points.get(mapper.toPointId(MATERIAL_CODE_ID));
        assertThat(stored.getMetadata())
                .containsEntry("source_doc_id", MATERIAL_CODE_ID)
                .doesNotContainKey(QdrantPointIdMapper.DEFAULT_ORIGINAL_DOC_ID_FIELD);

        List<Document> results = customStore.similaritySearch(
                SearchRequest.builder().query("开关").build());
        assertThat(results.get(0).getId()).isEqualTo(MATERIAL_CODE_ID);
    }

    private static Document doc(String docId, String text) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("doc_type", "material_code");
        return Document.builder().id(docId).text(text).metadata(metadata).build();
    }

    /**
     * 只认 UUID 点 ID 的向量库替身，复刻 Qdrant 的约束与 upsert 语义
     */
    private static final class UuidOnlyVectorStore implements VectorStore {

        private final Map<String, Document> points = new LinkedHashMap<>();
        private final List<Filter.Expression> filterDeletes = new ArrayList<>();

        @Override
        public void add(List<Document> documents) {
            documents.forEach(document -> points.put(requireUuid(document.getId()), document));
        }

        @Override
        public void delete(List<String> idList) {
            idList.forEach(id -> points.remove(requireUuid(id)));
        }

        @Override
        public void delete(Filter.Expression filterExpression) {
            filterDeletes.add(filterExpression);
        }

        @Override
        public List<Document> similaritySearch(SearchRequest request) {
            return points.values().stream()
                    .map(document -> document.mutate().score(0.87).build())
                    .toList();
        }

        /** 与 QdrantVectorStore#doAdd 同型：非 UUID 直接抛 IllegalArgumentException */
        private static String requireUuid(String id) {
            UUID.fromString(id);
            return id;
        }
    }
}
