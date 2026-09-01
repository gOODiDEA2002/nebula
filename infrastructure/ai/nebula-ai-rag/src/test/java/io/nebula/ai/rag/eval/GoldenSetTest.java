package io.nebula.ai.rag.eval;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 金标集加载与命中判据
 * <p>
 * 金标是评测的地基：坏条目必须当场炸掉而不是被静默跳过 ——
 * 少一条金标，报告上的 recall 分母就少 1，数字照样「好看」，没人会发现。
 */
@DisplayName("GoldenSet 加载与前缀判中")
class GoldenSetTest {

    @Test
    @DisplayName("顶层数组形态可加载")
    void arrayForm_isLoaded() {
        GoldenSet goldenSet = GoldenSet.fromJson(json("""
                [
                  {"query": "如何配置向量库", "expectedIdPrefixes": ["config.md#"], "subset": "plain"},
                  {"query": "表格末行写了什么", "expectedIdPrefixes": ["table.md#"], "subset": "table"}
                ]
                """));

        assertThat(goldenSet.size()).isEqualTo(2);
        assertThat(goldenSet.getEntries().get(0).getQuery()).isEqualTo("如何配置向量库");
        assertThat(goldenSet.subsets()).containsExactly("plain", "table");
    }

    @Test
    @DisplayName("{entries: [...]} 对象形态可加载")
    void wrapperForm_isLoaded() {
        GoldenSet goldenSet = GoldenSet.fromJson(json("""
                {"entries": [
                  {"query": "问题", "expectedIdPrefixes": ["a.md#"], "subset": "plain"}
                ]}
                """));

        assertThat(goldenSet.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("未知字段被忽略, 不因金标文件多写注释字段而报错")
    void unknownFields_areIgnored() {
        GoldenSet goldenSet = GoldenSet.fromJson(json("""
                [{"query": "问题", "expectedIdPrefixes": ["a.md#"], "note": "人工备注"}]
                """));

        assertThat(goldenSet.size()).isEqualTo(1);
        assertThat(goldenSet.getEntries().get(0).resolvedSubset())
                .isEqualTo(GoldenSet.DEFAULT_SUBSET);
    }

    @Test
    @DisplayName("命中判据是前缀匹配, 不是相等也不是包含")
    void matches_usesPrefixSemantics() {
        GoldenSetEntry entry = new GoldenSetEntry("问题", List.of("table.md#"), "table");

        assertThat(entry.matches("table.md#3")).isTrue();
        assertThat(entry.matches("table.md#")).isTrue();
        assertThat(entry.matches("other.md#3")).isFalse();
        // 「包含」会让 prefix-of-suffix 的巧合命中, 因此必须是 startsWith
        assertThat(entry.matches("x-table.md#3")).isFalse();
        assertThat(entry.matches(null)).isFalse();
    }

    @Test
    @DisplayName("多个前缀任一命中即算命中")
    void matches_anyPrefixCounts() {
        GoldenSetEntry entry = new GoldenSetEntry("问题", List.of("a.md#", "b.md#"), "plain");

        assertThat(entry.matches("b.md#7")).isTrue();
        assertThat(entry.matches("c.md#7")).isFalse();
    }

    @Test
    @DisplayName("坏金标一律抛 IllegalArgumentException 并指出具体条目")
    void malformedGoldenSet_failsLoudly() {
        assertThatThrownBy(() -> GoldenSet.fromJson(json("[{\"expectedIdPrefixes\": [\"a#\"]}]")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("缺少 query");

        assertThatThrownBy(() -> GoldenSet.fromJson(json("[{\"query\": \"问题\"}]")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("缺少 expectedIdPrefixes");

        assertThatThrownBy(() -> GoldenSet.fromJson(
                json("[{\"query\": \"问题\", \"expectedIdPrefixes\": [\"  \"]}]")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("空的 expectedIdPrefix");

        assertThatThrownBy(() -> GoldenSet.fromJson(json("[]")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不能为空");

        assertThatThrownBy(() -> GoldenSet.fromJson(json("{ 这不是 JSON")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("解析失败");

        assertThatThrownBy(() -> GoldenSet.fromJson(json("")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("内容为空");

        assertThatThrownBy(() -> GoldenSet.fromJson(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("输入流");
    }

    private static InputStream json(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }
}
