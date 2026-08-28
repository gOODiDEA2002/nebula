package io.nebula.ai.spring.vectorstore;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * docId 到 Qdrant 点 ID 的映射规则本身要被钉死
 * <p>
 * 生产事故是「点 ID 不是 UUID 就写不进去」，所以这里的断言只有两类：
 * 产出必须是合法 UUID，且同一个 docId 恒得同一个值（幂等的地基）。
 * 三条用例 ID 直接取自生产实际形态，不用自造样本。
 * <p>
 * 金标命名空间取自已有 460 万点的生产库（{@code vector.sia.vocoor.com}）：
 * 映射规则一旦漂移，等于把全库点 ID 换一遍、只能全量重灌，所以拿真实现存值当锚点。
 */
class QdrantPointIdMapperTest {

    /** 生产库现用命名空间，改这里等于宣布全量重灌 */
    private static final String PRODUCTION_NAMESPACE_NAME = "vector.sia.vocoor.com";

    private final QdrantPointIdMapper mapper = new QdrantPointIdMapper(PRODUCTION_NAMESPACE_NAME);

    @ParameterizedTest
    @ValueSource(strings = {"mc:0113120012", "product-1480334", "enterprise-123"})
    void productionDocIdShapes_mapToParsableUuid(String docId) {
        String pointId = mapper.toPointId(docId);

        // 能被 UUID.fromString 解析，正是 QdrantVectorStore#doAdd 里那条炸掉的校验
        UUID parsed = UUID.fromString(pointId);
        assertThat(parsed.version()).isEqualTo(5);
        assertThat(parsed.variant()).isEqualTo(2);  // RFC 4122
    }

    @ParameterizedTest
    @ValueSource(strings = {"mc:0113120012", "product-1480334", "enterprise-123"})
    void sameDocId_alwaysMapsToSamePointId(String docId) {
        assertThat(mapper.toPointId(docId)).isEqualTo(mapper.toPointId(docId));
    }

    @Test
    void differentDocIds_mapToDifferentPointIds() {
        assertThat(mapper.toPointId("mc:0113120012"))
                .isNotEqualTo(mapper.toPointId("mc:0113120013"));
    }

    /**
     * 跨进程/跨语言的锚点：期望值由 Python 标准库
     * {@code uuid.uuid5(uuid.UUID('a9729452-ee79-5d3d-ad4e-f65bf9d9bdde'), name)} 算出，
     * 与生产库中实际存在的点 ID 一致。
     * 对上说明本实现确是 RFC 4122 v5，运维脚本可以用一行 Python 复算出同一个点 ID，
     * 不必给高基数的 orig_doc_id 建 payload 索引。
     */
    @Test
    void mappingMatchesRfc4122V5_crossCheckedWithPython() {
        assertThat(mapper.toPointId("mc:0113120012"))
                .isEqualTo("3bb9c5da-a0dd-5f4d-bca0-82aa217d3e13");
        assertThat(mapper.toPointId("product-1480334"))
                .isEqualTo("7895339f-74a6-557e-a177-d191de923050");
        assertThat(mapper.toPointId("enterprise-123"))
                .isEqualTo("8e4fdd45-d81f-5433-bdad-d36d1be6b94b");
    }

    /**
     * 命名空间从常量改为构造参数后，推导结果必须与迁移前的字面量完全一致，
     * 否则现有 460 万点全部对不上
     */
    @Test
    void namespace_isDerivedFromNameAndMatchesLegacyConstant() {
        assertThat(mapper.getNamespace()).isEqualTo(
                QdrantPointIdMapper.uuidV5(QdrantPointIdMapper.DNS_NAMESPACE, PRODUCTION_NAMESPACE_NAME));
        assertThat(mapper.getNamespace()).hasToString("a9729452-ee79-5d3d-ad4e-f65bf9d9bdde");
        assertThat(mapper.getNamespaceName()).isEqualTo(PRODUCTION_NAMESPACE_NAME);
    }

    /**
     * 不同命名空间必须互相隔离：这正是选 v5 而不是 UUID.nameUUIDFromBytes(v3) 的原因
     */
    @Test
    void differentNamespaces_produceDifferentPointIdsForSameDocId() {
        QdrantPointIdMapper other = new QdrantPointIdMapper("vector.other.example.com");

        assertThat(other.toPointId("mc:0113120012")).isNotEqualTo(mapper.toPointId("mc:0113120012"));
    }

    @Test
    void blankNamespaceName_rejectedAtConstruction() {
        assertThatThrownBy(() -> new QdrantPointIdMapper(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new QdrantPointIdMapper("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void blankDocId_rejectedInsteadOfSilentlyMapped() {
        assertThatThrownBy(() -> mapper.toPointId(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> mapper.toPointId("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
