package io.nebula.ai.rag.chunking.parse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JSON 结构解析
 * <p>
 * 核心不变量：产出的每个 RECORD 都是合法 JSON 子树，且带着它的键路径。
 * 这两条同时成立，配置项才不会与它所属的分组被切散。
 */
@DisplayName("JsonStructureParser")
class JsonStructureParserTest {

    @Test
    @DisplayName("整棵树装得下时只产出一个记录, 面包屑为空(即根)")
    void smallDocument_yieldsSingleRootRecord() {
        List<DocElement> elements = new JsonStructureParser(500)
                .parse("{\"a\":1,\"b\":\"x\"}", null);

        assertThat(elements).singleElement().satisfies(e -> {
            assertThat(e.getType()).isEqualTo(DocElementType.RECORD);
            assertThat(e.getBreadcrumb()).isEmpty();
            assertThat(e.getText()).isEqualTo("{\"a\":1,\"b\":\"x\"}");
        });
    }

    @Test
    @DisplayName("超预算的子树继续下潜, 每个记录带自己的键路径")
    void oversizedSubtree_isSplitByKeyPath() {
        String json = "{\"group\":{\"first\":\"" + "一".repeat(60) + "\","
                + "\"second\":\"" + "二".repeat(60) + "\"}}";

        List<DocElement> elements = new JsonStructureParser(80).parse(json, null);

        assertThat(elements).hasSize(2);
        assertThat(elements.get(0).getBreadcrumb()).containsExactly("group", "first");
        assertThat(elements.get(1).getBreadcrumb()).containsExactly("group", "second");
        assertThat(elements.get(0).getText()).startsWith("\"一一");
    }

    @Test
    @DisplayName("父路径与叶子键共存于同一个记录: 装得下就整棵封存")
    void parentPathAndLeaf_stayTogether() {
        String json = "{\"nebula\":{\"gateway\":{\"rate-limit\":{\"token-bucket\":"
                + "{\"refill-per-second\":200,\"capacity\":400}},\"padding\":\""
                + "填".repeat(200) + "\"}}}";

        List<DocElement> elements = new JsonStructureParser(120).parse(json, null);

        // 下潜到 rate-limit 这一层就装得下了, 于是整棵子树封成一条记录：
        // 面包屑给出祖先路径, 正文里 token-bucket 与两个叶子键仍然共处一块
        DocElement bucket = elements.stream()
                .filter(e -> e.getText().contains("token-bucket")).findFirst().orElseThrow();
        assertThat(bucket.getBreadcrumb()).containsExactly("nebula", "gateway", "rate-limit");
        assertThat(bucket.getText()).contains("refill-per-second").contains("capacity");
        // 同层的巨大兄弟键被分到另一条记录, 不会把 token-bucket 挤出预算
        assertThat(elements).anySatisfy(e ->
                assertThat(e.getBreadcrumb()).containsExactly("nebula", "gateway", "padding"));
    }

    @Test
    @DisplayName("数组下潜时用下标作为路径段")
    void arrayElements_useIndexSegments() {
        String json = "{\"items\":[\"" + "甲".repeat(50) + "\",\"" + "乙".repeat(50) + "\"]}";

        List<DocElement> elements = new JsonStructureParser(60).parse(json, null);

        assertThat(elements).hasSize(2);
        assertThat(elements.get(0).getBreadcrumb()).containsExactly("items", "[0]");
        assertThat(elements.get(1).getBreadcrumb()).containsExactly("items", "[1]");
    }

    @Test
    @DisplayName("超长标量无处再拆, 整条产出而不是截断")
    void oversizedScalar_isEmittedWhole() {
        String json = "{\"note\":\"" + "长".repeat(200) + "\"}";

        List<DocElement> elements = new JsonStructureParser(50).parse(json, null);

        assertThat(elements).singleElement().satisfies(e -> {
            assertThat(e.getBreadcrumb()).containsExactly("note");
            assertThat(e.getText()).hasSizeGreaterThan(200);
        });
    }

    @Test
    @DisplayName("空对象与空数组不产出元素")
    void emptyContainers_areSkipped() {
        String json = "{\"a\":{},\"b\":[],\"c\":\"" + "值".repeat(60) + "\"}";

        List<DocElement> elements = new JsonStructureParser(40).parse(json, null);

        assertThat(elements).singleElement()
                .satisfies(e -> assertThat(e.getBreadcrumb()).containsExactly("c"));
    }

    @Test
    @DisplayName("空输入返回空表")
    void blankInput_yieldsNoElements() {
        JsonStructureParser parser = new JsonStructureParser();
        assertThat(parser.parse(null, null)).isEmpty();
        assertThat(parser.parse("   ", null)).isEmpty();
    }

    @Test
    @DisplayName("坏 JSON 抛 IllegalArgumentException 而不是静默产出半份内容")
    void malformedJson_failsLoudly() {
        assertThatThrownBy(() -> new JsonStructureParser().parse("{\"a\": ", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JSON 解析失败");
    }

    @Test
    @DisplayName("嵌套深度超限抛 ParseLimitExceededException(挡深度炸弹)")
    void depthBomb_isRejected() {
        StringBuilder deep = new StringBuilder();
        for (int i = 0; i < 40; i++) {
            deep.append("{\"k\":");
        }
        deep.append("\"").append("值".repeat(200)).append("\"");
        deep.append("}".repeat(40));

        ParseOptions options = new ParseOptions();
        options.setMaxDepth(8);

        assertThatThrownBy(() -> new JsonStructureParser(10).parse(deep.toString(), options))
                .isInstanceOf(ParseLimitExceededException.class)
                .hasMessageContaining("嵌套深度");
    }

    @Test
    @DisplayName("元素数量超限抛 ParseLimitExceededException(挡巨数组)")
    void hugeArray_isRejected() {
        StringBuilder array = new StringBuilder("[");
        for (int i = 0; i < 50; i++) {
            if (i > 0) {
                array.append(',');
            }
            array.append('"').append("元".repeat(30)).append('"');
        }
        array.append(']');

        ParseOptions options = new ParseOptions();
        options.setMaxElements(10);

        assertThatThrownBy(() -> new JsonStructureParser(20).parse(array.toString(), options))
                .isInstanceOf(ParseLimitExceededException.class)
                .hasMessageContaining("元素数量");
    }

    @Test
    @DisplayName("非法记录预算构造期拒绝")
    void invalidBudget_failsFast() {
        assertThatThrownBy(() -> new JsonStructureParser(0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("format 标识为 json, 默认预算为 500")
    void formatAndDefaults() {
        JsonStructureParser parser = new JsonStructureParser();
        assertThat(parser.format()).isEqualTo("json");
        assertThat(parser.getMaxRecordChars()).isEqualTo(500);
    }
}
