package io.nebula.ai.rag.chunking.pack;

import io.nebula.ai.rag.chunking.ChunkType;
import io.nebula.ai.rag.chunking.DocumentChunk;
import io.nebula.ai.rag.chunking.parse.DocElement;
import io.nebula.ai.rag.chunking.parse.DocElementType;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 装箱算法
 * <p>
 * 装箱决定了灌进向量库的内容边界，改这里等于全量重灌，因此把不变量钉死：
 * 原子单元不被切坏、表格切分后每片仍带表头、重叠落在整句边界、面包屑一定写进 metadata。
 */
@DisplayName("ChunkPacker 装箱算法")
class ChunkPackerTest {

    @Test
    @DisplayName("空元素流返回空表, docId 为空则快速失败")
    void emptyInput_andBlankDocId() {
        ChunkPacker packer = new ChunkPacker();

        assertThat(packer.pack("doc", null)).isEmpty();
        assertThat(packer.pack("doc", List.of())).isEmpty();
        assertThatThrownBy(() -> packer.pack("  ", List.of(paragraph("正文", "章节"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("docId");
    }

    @Test
    @DisplayName("标题只推进面包屑, 自己不成块")
    void heading_updatesBreadcrumbWithoutBecomingChunk() {
        List<DocumentChunk> chunks = new ChunkPacker().pack("doc", List.of(
                new DocElement(DocElementType.HEADING, "网关", List.of("网关")),
                new DocElement(DocElementType.PARAGRAPH, "导语。", List.of("网关"))));

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).getContent()).isEqualTo("导语。");
        assertThat(chunks.get(0).getTitle()).isEqualTo("网关");
    }

    @Test
    @DisplayName("同面包屑的相邻段落并箱, 放不下才封箱")
    void sameBreadcrumbParagraphs_areGreedilyPacked() {
        // 每段 8 字, 段间空行 2 字: 两段 18 字装得下, 三段 28 字装不下
        PackOptions options = new PackOptions();
        options.setMaxChunkSize(20);
        options.setOverlap(5);

        List<DocumentChunk> chunks = new ChunkPacker(options).pack("doc", List.of(
                paragraph("第一段八个字。", "章节"),
                paragraph("第二段八个字。", "章节"),
                paragraph("第三段八个字。", "章节")));

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).getContent()).isEqualTo("第一段八个字。\n\n第二段八个字。");
        assertThat(chunks.get(1).getContent()).isEqualTo("第三段八个字。");
    }

    @Test
    @DisplayName("面包屑一变立刻封箱: 两个小节的内容不混进同一块")
    void breadcrumbChange_forcesNewBox() {
        List<DocumentChunk> chunks = new ChunkPacker().pack("doc", List.of(
                paragraph("限流的正文。", "网关", "限流"),
                paragraph("路由的正文。", "网关", "路由")));

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).getTitle()).isEqualTo("限流");
        assertThat(chunks.get(1).getTitle()).isEqualTo("路由");
    }

    @Test
    @DisplayName("代码块超限也不切, 单独成块")
    void oversizedCode_isNeverSplit() {
        PackOptions options = new PackOptions();
        options.setMaxChunkSize(20);
        options.setOverlap(5);
        String code = "public class A {\n    int x;\n    int y;\n}";

        List<DocumentChunk> chunks = new ChunkPacker(options).pack("doc", List.of(
                new DocElement(DocElementType.CODE, code, List.of("示例"))));

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).getContent()).isEqualTo(code);
        assertThat(chunks.get(0).getChunkType()).isEqualTo(ChunkType.CODE);
    }

    @Test
    @DisplayName("表格超限按数据行切, 每片重复表头并记分片编号")
    void oversizedTable_isSplitByRowsWithRepeatedHeader() {
        PackOptions options = new PackOptions();
        options.setMaxChunkSize(55);
        options.setOverlap(10);
        String header = "| 模块 | 端口 |\n|---|---|";
        String table = header
                + "\n| 网关 | 3093 |"
                + "\n| 存储 | 3096 |"
                + "\n| 缓存 | 3084 |"
                + "\n| 搜索 | 3090 |";

        List<DocumentChunk> chunks = new ChunkPacker(options).pack("doc", List.of(
                new DocElement(DocElementType.TABLE, table, List.of("端口表"))
                        .attr(DocElement.ATTR_HEADER_ROW_COUNT, 2)));

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).allSatisfy(chunk -> {
            assertThat(chunk.getContent()).startsWith(header);
            assertThat(chunk.getChunkType()).isEqualTo(ChunkType.TABLE);
        });
        // 分片编号写成 i/n, 且所有数据行都还在
        assertThat(chunks).extracting(c -> c.getMetadata().get(PackOptions.META_TABLE_PART))
                .containsExactly("1/2", "2/2");
        String merged = chunks.stream().map(DocumentChunk::getContent).reduce("", String::concat);
        assertThat(merged).contains("网关").contains("存储").contains("缓存").contains("搜索");
    }

    @Test
    @DisplayName("表格不超限时整体成块, 不写分片编号")
    void tableWithinBudget_staysWhole() {
        String table = "| 模块 | 端口 |\n|---|---|\n| 网关 | 3093 |";

        List<DocumentChunk> chunks = new ChunkPacker().pack("doc", List.of(
                new DocElement(DocElementType.TABLE, table, List.of("端口表"))
                        .attr(DocElement.ATTR_HEADER_ROW_COUNT, 2)));

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).getContent()).isEqualTo(table);
        assertThat(chunks.get(0).getMetadata()).doesNotContainKey(PackOptions.META_TABLE_PART);
    }

    @Test
    @DisplayName("超长段落按句末标点降级切分, 重叠是一整句而不是半句")
    void oversizedParagraph_splitsAtSentenceBoundariesWithWholeSentenceOverlap() {
        PackOptions options = new PackOptions();
        options.setMaxChunkSize(30);
        options.setOverlap(10);
        String text = "第一句话结束。第二句话结束。第三句话结束。第四句话结束。第五句话结束。第六句话结束。";

        List<DocumentChunk> chunks = new ChunkPacker(options).pack("doc", List.of(
                paragraph(text, "章节")));

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).getContent())
                .isEqualTo("第一句话结束。第二句话结束。第三句话结束。第四句话结束。");
        // 重叠取整句回退：第二块以完整的第四句开头
        assertThat(chunks.get(1).getContent())
                .isEqualTo("第四句话结束。第五句话结束。第六句话结束。");
        assertThat(chunks).allSatisfy(chunk ->
                assertThat(chunk.getContent().length()).isLessThanOrEqualTo(30));
    }

    @Test
    @DisplayName("有空行时先按空行断段, 不下潜到句级")
    void blankLineSeparator_isPreferredOverSentenceLevel() {
        PackOptions options = new PackOptions();
        options.setMaxChunkSize(20);
        options.setOverlap(5);

        List<DocumentChunk> chunks = new ChunkPacker(options).pack("doc", List.of(
                paragraph("甲段第一句。甲段第二句。\n\n乙段第一句。乙段第二句。", "章节")));

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).getContent()).isEqualTo("甲段第一句。甲段第二句。");
        assertThat(chunks.get(1).getContent()).isEqualTo("乙段第一句。乙段第二句。");
    }

    @Test
    @DisplayName("无自然边界的长文本退化为字符级硬切, 且不带重叠")
    void boundarylessText_fallsBackToCharacterSplitWithoutOverlap() {
        PackOptions options = new PackOptions();
        options.setMaxChunkSize(10);
        options.setOverlap(4);
        String text = "a".repeat(35);

        List<DocumentChunk> chunks = new ChunkPacker(options).pack("doc", List.of(
                paragraph(text, "章节")));

        assertThat(chunks).hasSize(4);
        assertThat(chunks).allSatisfy(chunk ->
                assertThat(chunk.getContent().length()).isLessThanOrEqualTo(10));
        // 无重叠：拼起来正好还原原文，多一个字符都没有
        String merged = chunks.stream().map(DocumentChunk::getContent).reduce("", String::concat);
        assertThat(merged).isEqualTo(text);
    }

    @Test
    @DisplayName("切分不丢内容: 每一句都能在某个块里找到")
    void splitting_losesNothing() {
        PackOptions options = new PackOptions();
        options.setMaxChunkSize(25);
        options.setOverlap(8);
        List<String> sentences = List.of("甲句内容。", "乙句内容。", "丙句内容。", "丁句内容。",
                "戊句内容。", "己句内容。", "庚句内容。", "辛句内容。");

        List<DocumentChunk> chunks = new ChunkPacker(options).pack("doc", List.of(
                paragraph(String.join("", sentences), "章节")));

        for (String sentence : sentences) {
            assertThat(chunks).anySatisfy(chunk ->
                    assertThat(chunk.getContent()).contains(sentence));
        }
    }

    @Test
    @DisplayName("面包屑恒写 metadata; 默认不注入正文, 打开开关才注入")
    void breadcrumb_isAlwaysInMetadataAndOptionallyInContent() {
        List<DocElement> elements = List.of(paragraph("补充速率二百。", "网关", "限流", "令牌桶"));

        DocumentChunk defaultChunk = new ChunkPacker().pack("doc", elements).get(0);
        assertThat(defaultChunk.getMetadata()).containsEntry(PackOptions.META_BREADCRUMB,
                List.of("网关", "限流", "令牌桶"));
        assertThat(defaultChunk.getContent()).isEqualTo("补充速率二百。");

        PackOptions injected = new PackOptions();
        injected.setBreadcrumbToContent(true);
        DocumentChunk withPath = new ChunkPacker(injected).pack("doc", elements).get(0);
        assertThat(withPath.getContent()).isEqualTo("网关 > 限流 > 令牌桶\n补充速率二百。");
        assertThat(withPath.getMetadata()).containsEntry(PackOptions.META_BREADCRUMB,
                List.of("网关", "限流", "令牌桶"));
    }

    @Test
    @DisplayName("块类型按主导元素类型定, 记录与列表项归为 SECTION")
    void chunkType_followsDominantElementType() {
        ChunkPacker packer = new ChunkPacker();

        assertThat(packer.pack("doc", List.of(
                new DocElement(DocElementType.CONFIG, "nebula:\n  ai: true", List.of("配置"))))
                .get(0).getChunkType()).isEqualTo(ChunkType.CONFIG);
        assertThat(packer.pack("doc", List.of(
                new DocElement(DocElementType.RECORD, "{\"a\":1}", List.of("记录"))))
                .get(0).getChunkType()).isEqualTo(ChunkType.SECTION);
        assertThat(packer.pack("doc", List.of(
                new DocElement(DocElementType.LIST_ITEM, "- 一项", List.of("列表"))))
                .get(0).getChunkType()).isEqualTo(ChunkType.SECTION);
    }

    @Test
    @DisplayName("空文本元素被跳过, 不产出空块")
    void blankElements_areSkipped() {
        List<DocumentChunk> chunks = new ChunkPacker().pack("doc", List.of(
                new DocElement(DocElementType.PARAGRAPH, "   ", List.of("章节")),
                paragraph("有内容。", "章节")));

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).getContent()).isEqualTo("有内容。");
    }

    @Test
    @DisplayName("overlap 不小于 maxChunkSize 时构造期拒绝")
    void invalidOverlap_failsFast() {
        PackOptions options = new PackOptions();
        options.setMaxChunkSize(100);
        options.setOverlap(100);

        assertThatThrownBy(() -> new ChunkPacker(options))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("overlap");
    }

    @Test
    @DisplayName("模块名写成 docId, 便于按文档反查块")
    void moduleName_carriesDocId() {
        DocumentChunk chunk = new ChunkPacker().pack("guide.md", List.of(
                paragraph("正文。", "章节"))).get(0);

        assertThat(chunk.getModuleName()).isEqualTo("guide.md");
    }

    private static DocElement paragraph(String text, String... breadcrumb) {
        return new DocElement(DocElementType.PARAGRAPH, text, List.of(breadcrumb));
    }
}
