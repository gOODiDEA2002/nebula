package io.nebula.ai.rag.chunking.parse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Markdown 结构解析
 * <p>
 * 重点不在「能识别标题」，而在识别顺序与畸形输入：围栏内的管道行不能被当成表格，
 * 缺分隔行的管道行不能被当成表格，未闭合的围栏不能让解析器一路吃到崩。
 */
@DisplayName("MarkdownStructureParser")
class MarkdownStructureParserTest {

    private final MarkdownStructureParser parser = new MarkdownStructureParser();

    @Test
    @DisplayName("空输入返回空表, 不抛异常")
    void emptyInput_yieldsNoElements() {
        assertThat(parser.parse(null, null)).isEmpty();
        assertThat(parser.parse("", null)).isEmpty();
        assertThat(parser.parse("   \n\n  ", null)).isEmpty();
    }

    @Test
    @DisplayName("format 标识为 markdown")
    void format_isMarkdown() {
        assertThat(parser.format()).isEqualTo("markdown");
    }

    @Test
    @DisplayName("标题维护层级栈, 正文拿到完整标题路径")
    void headings_buildBreadcrumbPath() {
        List<DocElement> elements = parser.parse("""
                # 网关
                导语段落。
                ## 限流
                ### 令牌桶
                #### 默认值
                补充速率默认每秒二百个。
                ## 路由
                匹配按前缀优先。
                """, null);

        DocElement leaf = elementWithText(elements, "补充速率默认每秒二百个。");
        assertThat(leaf.getType()).isEqualTo(DocElementType.PARAGRAPH);
        assertThat(leaf.getBreadcrumb()).containsExactly("网关", "限流", "令牌桶", "默认值");

        // 回到 H2 之后, 更深的层级必须被清掉, 否则路由段落会挂在令牌桶下面
        DocElement route = elementWithText(elements, "匹配按前缀优先。");
        assertThat(route.getBreadcrumb()).containsExactly("网关", "路由");
    }

    @Test
    @DisplayName("标题自身也产出元素(供装箱层推进面包屑)")
    void headings_areEmittedAsElements() {
        List<DocElement> elements = parser.parse("# 标题\n正文。\n", null);

        assertThat(elements).hasSize(2);
        assertThat(elements.get(0).getType()).isEqualTo(DocElementType.HEADING);
        assertThat(elements.get(0).getText()).isEqualTo("标题");
        assertThat(elements.get(0).getBreadcrumb()).containsExactly("标题");
    }

    @Test
    @DisplayName("围栏代码块整体成元素, 语言写进 attrs")
    void fencedCode_becomesAtomicElement() {
        List<DocElement> elements = parser.parse("""
                # 示例
                ```java
                public class A {
                    // 注释
                }
                ```
                收尾段落。
                """, null);

        DocElement code = elements.stream()
                .filter(e -> e.getType() == DocElementType.CODE).findFirst().orElseThrow();
        assertThat(code.getText()).isEqualTo("public class A {\n    // 注释\n}");
        assertThat(code.getAttrs()).containsEntry(DocElement.ATTR_LANGUAGE, "java");
        assertThat(code.getBreadcrumb()).containsExactly("示例");
    }

    @Test
    @DisplayName("围栏内的管道行与井号行不参与表格与标题识别")
    void fencedCode_shieldsInnerSyntax() {
        List<DocElement> elements = parser.parse("""
                ```text
                | 列一 | 列二 |
                |---|---|
                | 值一 | 值二 |
                # 这不是标题
                - 这不是列表项
                ```
                """, null);

        assertThat(elements).hasSize(1);
        assertThat(elements.get(0).getType()).isEqualTo(DocElementType.CODE);
        assertThat(elements.get(0).getText()).contains("| 值一 | 值二 |").contains("# 这不是标题");
    }

    @Test
    @DisplayName("yaml 围栏归为 CONFIG 而不是 CODE")
    void yamlFence_becomesConfigElement() {
        List<DocElement> elements = parser.parse("```yaml\nnebula:\n  ai: true\n```\n", null);

        assertThat(elements).singleElement()
                .satisfies(e -> assertThat(e.getType()).isEqualTo(DocElementType.CONFIG));
    }

    @Test
    @DisplayName("未闭合围栏吃到文末并留下标记, 不抛异常")
    void unterminatedFence_isToleratedAndFlagged() {
        List<DocElement> elements = parser.parse("""
                # 标题
                ```java
                public class A {
                """, null);

        DocElement code = elements.stream()
                .filter(e -> e.getType() == DocElementType.CODE).findFirst().orElseThrow();
        assertThat(code.getText()).contains("public class A {");
        assertThat(code.getAttrs()).containsEntry("unterminated", true);
    }

    @Test
    @DisplayName("表格整体成元素, 表头行数记 2(表头行 + 分隔行)")
    void table_becomesSingleElementWithHeaderRowCount() {
        List<DocElement> elements = parser.parse("""
                # 端口表
                | 模块 | 端口 |
                |---|---|
                | 网关 | 3093 |
                | 存储 | 3096 |

                表后说明。
                """, null);

        DocElement table = elements.stream()
                .filter(e -> e.getType() == DocElementType.TABLE).findFirst().orElseThrow();
        assertThat(table.intAttr(DocElement.ATTR_HEADER_ROW_COUNT, -1)).isEqualTo(2);
        assertThat(table.getText().lines().count()).isEqualTo(4);
        assertThat(table.getText()).startsWith("| 模块 | 端口 |");
        assertThat(elementWithText(elements, "表后说明。").getType())
                .isEqualTo(DocElementType.PARAGRAPH);
    }

    @Test
    @DisplayName("缺分隔行的管道行按段落处理, 不误判成表格")
    void brokenTable_fallsBackToParagraph() {
        List<DocElement> elements = parser.parse("| 模块 | 端口 |\n| 网关 | 3093 |\n", null);

        assertThat(elements).noneMatch(e -> e.getType() == DocElementType.TABLE);
        assertThat(elements).singleElement()
                .satisfies(e -> assertThat(e.getType()).isEqualTo(DocElementType.PARAGRAPH));
    }

    @Test
    @DisplayName("列表项逐项成元素, 有序无序都认")
    void listItems_areEmittedIndividually() {
        List<DocElement> elements = parser.parse("""
                - 第一项
                * 第二项
                1. 第三项
                """, null);

        assertThat(elements).hasSize(3);
        assertThat(elements).allSatisfy(e ->
                assertThat(e.getType()).isEqualTo(DocElementType.LIST_ITEM));
        assertThat(elements.get(2).getText()).isEqualTo("1. 第三项");
    }

    @Test
    @DisplayName("连续行合成一个段落, 空行断开")
    void paragraphs_areSplitByBlankLines() {
        List<DocElement> elements = parser.parse("第一行\n第二行\n\n第三行\n", null);

        assertThat(elements).hasSize(2);
        assertThat(elements.get(0).getText()).isEqualTo("第一行\n第二行");
        assertThat(elements.get(1).getText()).isEqualTo("第三行");
    }

    @Test
    @DisplayName("输入长度超限抛 ParseLimitExceededException")
    void inputLimit_isEnforced() {
        ParseOptions options = new ParseOptions();
        options.setMaxInputChars(10);

        assertThatThrownBy(() -> parser.parse("这是一段超过十个字符的中文内容", options))
                .isInstanceOf(ParseLimitExceededException.class)
                .hasMessageContaining("输入长度");
    }

    @Test
    @DisplayName("元素数量超限抛 ParseLimitExceededException")
    void elementLimit_isEnforced() {
        ParseOptions options = new ParseOptions();
        options.setMaxElements(2);

        assertThatThrownBy(() -> parser.parse("- 一\n- 二\n- 三\n", options))
                .isInstanceOf(ParseLimitExceededException.class)
                .hasMessageContaining("元素数量");
    }

    @Test
    @DisplayName("标题层级超过深度上限抛 ParseLimitExceededException")
    void depthLimit_isEnforced() {
        ParseOptions options = new ParseOptions();
        options.setMaxDepth(2);

        assertThatThrownBy(() -> parser.parse("# 一\n## 二\n### 三\n", options))
                .isInstanceOf(ParseLimitExceededException.class)
                .hasMessageContaining("嵌套深度");
    }

    private static DocElement elementWithText(List<DocElement> elements, String text) {
        return elements.stream().filter(e -> text.equals(e.getText())).findFirst()
                .orElseThrow(() -> new AssertionError("没有找到文本为 " + text + " 的元素"));
    }
}
