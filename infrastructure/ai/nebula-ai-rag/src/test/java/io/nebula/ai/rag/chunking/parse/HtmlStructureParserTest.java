package io.nebula.ai.rag.chunking.parse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * HTML 结构解析
 * <p>
 * 本测试同时充当依赖边界的证据：{@code HtmlStructureParser} 是模块内唯一 import jsoup 的类，
 * 而 jsoup 是 optional 依赖，所以只有本测试会因为缺 jsoup 而失败。
 */
@DisplayName("HtmlStructureParser")
class HtmlStructureParserTest {

    private final HtmlStructureParser parser = new HtmlStructureParser();

    @Test
    @DisplayName("标题维护面包屑, 段落挂在标题路径下")
    void headings_buildBreadcrumbPath() {
        List<DocElement> elements = parser.parse("""
                <html><body>
                  <h1>网关</h1>
                  <h2>限流</h2>
                  <p>令牌桶允许积攒额度。</p>
                  <h2>路由</h2>
                  <p>匹配按前缀优先。</p>
                </body></html>
                """, null);

        DocElement first = paragraphWith(elements, "令牌桶允许积攒额度。");
        assertThat(first.getBreadcrumb()).containsExactly("网关", "限流");
        DocElement second = paragraphWith(elements, "匹配按前缀优先。");
        assertThat(second.getBreadcrumb()).containsExactly("网关", "路由");
    }

    @Test
    @DisplayName("脚本样式与导航页脚被剔除, 不污染每一个块")
    void noiseTags_areRemoved() {
        List<DocElement> elements = parser.parse("""
                <html><body>
                  <nav>首页 产品 文档</nav>
                  <script>var a = 1;</script>
                  <style>.a { color: red; }</style>
                  <p>正文段落。</p>
                  <footer>版权所有</footer>
                </body></html>
                """, null);

        assertThat(elements).hasSize(1);
        assertThat(elements.get(0).getText()).isEqualTo("正文段落。");
    }

    @Test
    @DisplayName("表格转 Markdown 管道表示并带表头行数")
    void table_isConvertedToPipeMarkdown() {
        List<DocElement> elements = parser.parse("""
                <table>
                  <tr><th>模块</th><th>端口</th></tr>
                  <tr><td>网关</td><td>3093</td></tr>
                </table>
                """, null);

        DocElement table = elements.stream()
                .filter(e -> e.getType() == DocElementType.TABLE).findFirst().orElseThrow();
        assertThat(table.intAttr(DocElement.ATTR_HEADER_ROW_COUNT, -1)).isEqualTo(2);
        assertThat(table.getText()).isEqualTo("| 模块 | 端口 |\n| --- | --- |\n| 网关 | 3093 |");
    }

    @Test
    @DisplayName("pre 成 CODE 元素并保留原始换行与缩进")
    void pre_becomesCodeElement() {
        List<DocElement> elements = parser.parse(
                "<pre>public class A {\n    int x;\n}</pre>", null);

        assertThat(elements).singleElement().satisfies(e -> {
            assertThat(e.getType()).isEqualTo(DocElementType.CODE);
            assertThat(e.getText()).isEqualTo("public class A {\n    int x;\n}");
        });
    }

    @Test
    @DisplayName("li 逐项成 LIST_ITEM 元素")
    void listItems_areEmittedIndividually() {
        List<DocElement> elements = parser.parse("<ul><li>第一项</li><li>第二项</li></ul>", null);

        assertThat(elements).hasSize(2);
        assertThat(elements).allSatisfy(e ->
                assertThat(e.getType()).isEqualTo(DocElementType.LIST_ITEM));
    }

    @Test
    @DisplayName("容器标签上的直接文本不丢, 且不与后代文本重复")
    void containerDirectText_isCapturedOnce() {
        List<DocElement> elements = parser.parse(
                "<div>外层直接文本<p>内层段落</p></div>", null);

        assertThat(elements).extracting(DocElement::getText)
                .containsExactly("外层直接文本", "内层段落");
    }

    @Test
    @DisplayName("嵌套容器可以一路下潜到最内层内容")
    void nestedContainers_areTraversed() {
        List<DocElement> elements = parser.parse(
                "<div><section><article><p>最内层</p></article></section></div>", null);

        assertThat(elements).singleElement()
                .satisfies(e -> assertThat(e.getText()).isEqualTo("最内层"));
    }

    @Test
    @DisplayName("空输入与纯空白返回空表")
    void emptyInput_yieldsNoElements() {
        assertThat(parser.parse(null, null)).isEmpty();
        assertThat(parser.parse("   ", null)).isEmpty();
    }

    @Test
    @DisplayName("畸形 HTML 由 jsoup 容错, 不抛异常")
    void malformedHtml_isTolerated() {
        List<DocElement> elements = parser.parse("<p>没有闭合的段落<div>另一段", null);

        assertThat(elements).isNotEmpty();
    }

    @Test
    @DisplayName("输入长度与元素数量超限抛 ParseLimitExceededException")
    void limits_areEnforced() {
        ParseOptions inputLimit = new ParseOptions();
        inputLimit.setMaxInputChars(5);
        assertThatThrownBy(() -> parser.parse("<p>一段比较长的正文</p>", inputLimit))
                .isInstanceOf(ParseLimitExceededException.class)
                .hasMessageContaining("输入长度");

        ParseOptions elementLimit = new ParseOptions();
        elementLimit.setMaxElements(2);
        assertThatThrownBy(() -> parser.parse("<p>一</p><p>二</p><p>三</p>", elementLimit))
                .isInstanceOf(ParseLimitExceededException.class)
                .hasMessageContaining("元素数量");
    }

    @Test
    @DisplayName("format 标识为 html, jsoup 类名常量与实际依赖一致")
    void formatAndJsoupClassName() {
        assertThat(parser.format()).isEqualTo("html");
        assertThat(HtmlStructureParser.JSOUP_CLASS_NAME).isEqualTo("org.jsoup.Jsoup");
        assertThat(org.jsoup.Jsoup.class.getName())
                .isEqualTo(HtmlStructureParser.JSOUP_CLASS_NAME);
    }

    private static DocElement paragraphWith(List<DocElement> elements, String text) {
        return elements.stream().filter(e -> text.equals(e.getText())).findFirst()
                .orElseThrow(() -> new AssertionError("没有找到文本为 " + text + " 的元素"));
    }
}
