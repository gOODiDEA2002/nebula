package io.nebula.ai.rag.chunking.parse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JSONL 结构解析
 */
@DisplayName("JsonlStructureParser")
class JsonlStructureParserTest {

    private final JsonlStructureParser parser = new JsonlStructureParser();

    @Test
    @DisplayName("每个非空行一条记录, 面包屑记行号")
    void eachLine_becomesRecord() {
        List<DocElement> elements = parser.parse("""
                {"id":1}
                {"id":2}
                """, null);

        assertThat(elements).hasSize(2);
        assertThat(elements.get(0).getType()).isEqualTo(DocElementType.RECORD);
        assertThat(elements.get(0).getText()).isEqualTo("{\"id\":1}");
        assertThat(elements.get(0).getBreadcrumb()).containsExactly("line 1");
        assertThat(elements.get(1).getBreadcrumb()).containsExactly("line 2");
    }

    @Test
    @DisplayName("空行跳过, 但行号仍按原文计数")
    void blankLines_areSkippedWithoutShiftingLineNumbers() {
        List<DocElement> elements = parser.parse("{\"id\":1}\n\n   \n{\"id\":4}\n", null);

        assertThat(elements).hasSize(2);
        assertThat(elements.get(1).getBreadcrumb()).containsExactly("line 4");
    }

    @Test
    @DisplayName("CRLF 换行不把回车带进记录")
    void crlf_isStripped() {
        List<DocElement> elements = parser.parse("{\"id\":1}\r\n{\"id\":2}\r\n", null);

        assertThat(elements).hasSize(2);
        assertThat(elements.get(0).getText()).isEqualTo("{\"id\":1}");
    }

    @Test
    @DisplayName("坏行照样产出记录: 一行坏不该丢掉整个文件")
    void malformedLine_isTolerated() {
        List<DocElement> elements = parser.parse("{\"id\":1}\n这不是 JSON\n", null);

        assertThat(elements).hasSize(2);
        assertThat(elements.get(1).getText()).isEqualTo("这不是 JSON");
    }

    @Test
    @DisplayName("空输入返回空表")
    void emptyInput_yieldsNoElements() {
        assertThat(parser.parse(null, null)).isEmpty();
        assertThat(parser.parse("", null)).isEmpty();
    }

    @Test
    @DisplayName("元素数量超限抛 ParseLimitExceededException")
    void elementLimit_isEnforced() {
        ParseOptions options = new ParseOptions();
        options.setMaxElements(2);

        assertThatThrownBy(() -> parser.parse("{\"a\":1}\n{\"b\":2}\n{\"c\":3}\n", options))
                .isInstanceOf(ParseLimitExceededException.class)
                .hasMessageContaining("元素数量");
    }

    @Test
    @DisplayName("输入长度超限抛 ParseLimitExceededException")
    void inputLimit_isEnforced() {
        ParseOptions options = new ParseOptions();
        options.setMaxInputChars(5);

        assertThatThrownBy(() -> parser.parse("{\"a\":1}\n", options))
                .isInstanceOf(ParseLimitExceededException.class);
    }

    @Test
    @DisplayName("format 标识为 jsonl")
    void format_isJsonl() {
        assertThat(parser.format()).isEqualTo("jsonl");
    }
}
