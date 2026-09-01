package io.nebula.ai.rag.chunking.parse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * XML 结构解析
 * <p>
 * 安全用例是本测试的重点：DTD 与外部实体必须被拒绝。这两项默认是开着的，
 * 一旦有人「顺手」把工厂换成默认配置，实体炸弹与外部实体注入就会重新可用，
 * 因此这里用真实的攻击样本钉死行为。
 */
@DisplayName("XmlStructureParser")
class XmlStructureParserTest {

    private final XmlStructureParser parser = new XmlStructureParser();

    @Test
    @DisplayName("叶子元素成记录, 面包屑是元素路径")
    void leafElements_becomeRecordsWithPath() {
        List<DocElement> elements = parser.parse("""
                <config>
                  <gateway>
                    <rateLimit>每秒二百个令牌</rateLimit>
                  </gateway>
                </config>
                """, null);

        assertThat(elements).singleElement().satisfies(e -> {
            assertThat(e.getType()).isEqualTo(DocElementType.RECORD);
            assertThat(e.getText()).isEqualTo("每秒二百个令牌");
            assertThat(e.getBreadcrumb()).containsExactly("config", "gateway", "rateLimit");
        });
    }

    @Test
    @DisplayName("识别性属性拼进面包屑, 重复兄弟元素因此可区分")
    void identityAttributes_disambiguateSiblings() {
        List<DocElement> elements = parser.parse("""
                <retrievers>
                  <retriever name="vector"><weight>0.6</weight></retriever>
                  <retriever name="keyword"><weight>0.4</weight></retriever>
                </retrievers>
                """, null);

        assertThat(elements).hasSize(2);
        assertThat(elements.get(0).getBreadcrumb())
                .containsExactly("retrievers", "retriever[name=vector]", "weight");
        assertThat(elements.get(1).getBreadcrumb())
                .containsExactly("retrievers", "retriever[name=keyword]", "weight");
    }

    @Test
    @DisplayName("容器元素的缩进空白不产出记录")
    void containerWhitespace_isNotEmitted() {
        List<DocElement> elements = parser.parse(
                "<a>\n  <b>值</b>\n</a>", null);

        assertThat(elements).hasSize(1);
        assertThat(elements.get(0).getText()).isEqualTo("值");
    }

    @Test
    @DisplayName("CDATA 内容按文本收进记录")
    void cdata_isCollected() {
        List<DocElement> elements = parser.parse(
                "<note><![CDATA[包含 <标签> 的原始文本]]></note>", null);

        assertThat(elements).singleElement()
                .satisfies(e -> assertThat(e.getText()).isEqualTo("包含 <标签> 的原始文本"));
    }

    @Test
    @DisplayName("外部实体注入样本被拒绝: 不读本地文件也不返回内容")
    void externalEntityInjection_isRejected() {
        String attack = """
                <?xml version="1.0"?>
                <!DOCTYPE root [
                  <!ENTITY xxe SYSTEM "file:///etc/passwd">
                ]>
                <root><data>&xxe;</data></root>
                """;

        assertThatThrownBy(() -> parser.parse(attack, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("XML 解析失败");
    }

    @Test
    @DisplayName("实体展开炸弹样本被拒绝: DTD 关闭后无处展开")
    void entityExpansionBomb_isRejected() {
        String bomb = """
                <?xml version="1.0"?>
                <!DOCTYPE lolz [
                  <!ENTITY lol "lol">
                  <!ENTITY lol2 "&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;">
                  <!ENTITY lol3 "&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;">
                ]>
                <lolz>&lol3;</lolz>
                """;

        assertThatThrownBy(() -> parser.parse(bomb, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("XML 解析失败");
    }

    @Test
    @DisplayName("坏 XML 抛 IllegalArgumentException")
    void malformedXml_failsLoudly() {
        assertThatThrownBy(() -> parser.parse("<a><b></a>", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("XML 解析失败");
    }

    @Test
    @DisplayName("空输入返回空表")
    void emptyInput_yieldsNoElements() {
        assertThat(parser.parse(null, null)).isEmpty();
        assertThat(parser.parse("   ", null)).isEmpty();
    }

    @Test
    @DisplayName("嵌套深度超限抛 ParseLimitExceededException")
    void depthLimit_isEnforced() {
        StringBuilder deep = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            deep.append("<n>");
        }
        deep.append("值");
        deep.append("</n>".repeat(20));

        ParseOptions options = new ParseOptions();
        options.setMaxDepth(5);

        assertThatThrownBy(() -> parser.parse(deep.toString(), options))
                .isInstanceOf(ParseLimitExceededException.class)
                .hasMessageContaining("嵌套深度");
    }

    @Test
    @DisplayName("元素数量超限抛 ParseLimitExceededException")
    void elementLimit_isEnforced() {
        ParseOptions options = new ParseOptions();
        options.setMaxElements(2);

        assertThatThrownBy(() -> parser.parse(
                "<r><a>一</a><b>二</b><c>三</c></r>", options))
                .isInstanceOf(ParseLimitExceededException.class)
                .hasMessageContaining("元素数量");
    }

    @Test
    @DisplayName("format 标识为 xml")
    void format_isXml() {
        assertThat(parser.format()).isEqualTo("xml");
    }
}
