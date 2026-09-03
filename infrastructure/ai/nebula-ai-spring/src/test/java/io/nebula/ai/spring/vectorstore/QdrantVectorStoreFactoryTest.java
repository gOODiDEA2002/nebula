package io.nebula.ai.spring.vectorstore;

import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * id-mapping 装饰规则单一出处（R3 §4.2）
 * <p>
 * {@code AIAutoConfiguration} 与 {@code QdrantIndexTargetFactory} 都经
 * {@link QdrantVectorStoreFactory#decorateWithIdMapping} 装饰 —— 本测试钉住该规则，
 * 从而保证「两条路径产出的 VectorStore 装饰形态一致」（同输入同形态），避免新旧代际 ID 形态不一致。
 */
class QdrantVectorStoreFactoryTest {

    private final VectorStore raw = mock(VectorStore.class);

    @Test
    void disabled_returnsRawUndecorated() {
        VectorStore result = QdrantVectorStoreFactory.decorateWithIdMapping(
                raw, false, "vector.example.com", "orig_doc_id");
        assertThat(result).isSameAs(raw);
    }

    @Test
    void enabled_wrapsInMappingDecoratorWithGivenField() {
        VectorStore result = QdrantVectorStoreFactory.decorateWithIdMapping(
                raw, true, "vector.example.com", "orig_doc_id");

        assertThat(result).isInstanceOf(QdrantIdMappingVectorStore.class);
        QdrantIdMappingVectorStore decorated = (QdrantIdMappingVectorStore) result;
        assertThat(decorated.getDelegate()).isSameAs(raw);
        assertThat(decorated.getOriginalDocIdField()).isEqualTo("orig_doc_id");
    }

    @Test
    void enabled_blankField_usesDefaultPayloadField() {
        VectorStore result = QdrantVectorStoreFactory.decorateWithIdMapping(
                raw, true, "vector.example.com", "  ");

        assertThat(((QdrantIdMappingVectorStore) result).getOriginalDocIdField())
                .isEqualTo(QdrantPointIdMapper.DEFAULT_ORIGINAL_DOC_ID_FIELD);
    }

    @Test
    void enabled_missingNamespace_failsFast() {
        assertThatThrownBy(() -> QdrantVectorStoreFactory.decorateWithIdMapping(
                raw, true, "  ", "orig_doc_id"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("namespace-name");
    }

    @Test
    void bothPaths_produceIdenticalDecorationForm() {
        // 模拟「装配路径」与「重灌工厂路径」用同一套规则、同一输入装饰同一个 raw
        VectorStore viaConfig = QdrantVectorStoreFactory.decorateWithIdMapping(
                raw, true, "vector.example.com", "orig_doc_id");
        VectorStore viaFactory = QdrantVectorStoreFactory.decorateWithIdMapping(
                raw, true, "vector.example.com", "orig_doc_id");

        assertThat(viaConfig).isInstanceOf(QdrantIdMappingVectorStore.class);
        assertThat(viaFactory).isInstanceOf(QdrantIdMappingVectorStore.class);
        QdrantIdMappingVectorStore a = (QdrantIdMappingVectorStore) viaConfig;
        QdrantIdMappingVectorStore b = (QdrantIdMappingVectorStore) viaFactory;
        // 装饰形态一致：同一被装饰对象、同一 payload 字段
        assertThat(a.getDelegate()).isSameAs(b.getDelegate());
        assertThat(a.getOriginalDocIdField()).isEqualTo(b.getOriginalDocIdField());
    }
}
