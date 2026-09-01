package io.nebula.ai.rag.eval;

import io.nebula.ai.rag.chunking.DocumentChunk;
import io.nebula.ai.rag.chunking.TextChunker;
import io.nebula.ai.rag.chunking.pack.ChunkIdStrategy;
import io.nebula.ai.rag.chunking.pack.ChunkPacker;
import io.nebula.ai.rag.chunking.pack.PackOptions;
import io.nebula.ai.rag.chunking.parse.JsonStructureParser;
import io.nebula.ai.rag.chunking.parse.JsonlStructureParser;
import io.nebula.ai.rag.chunking.parse.MarkdownStructureParser;
import io.nebula.ai.rag.chunking.parse.StructureParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 评测语料与金标的加载入口（评测专用，非交付物）
 * <p>
 * <b>语料的构造原则（改语料前必读）：</b>
 * <ul>
 *   <li>五个子集里，plain 是参照系，其余四个都把关键答案<b>刻意</b>放在
 *       「定长切分必然切坏、结构切分必然保全」的位置：表格末行（表头被切走）、
 *       长代码块中段（首尾无法共存于一块）、深层小节正文（祖先标题被切走）、
 *       深层 JSON 叶子（键路径被切走）；</li>
 *   <li>全部语料共用一套框架词汇（模块、默认、超时、配置、索引、缓存……），
 *       这样只有「结构完整的那一块」才能同时覆盖查询的两半，靠泛化词蹭不到分；</li>
 *   <li>金标按<b>文件级</b>前缀判中（{@code <文件名>#}），A/B 两侧的块索引不同也可比。</li>
 * </ul>
 * 语料改动之后必须重跑 {@code ChunkingEvalComparisonTest}：A 侧指标若明显抬高，
 * 说明埋点被改没了，此时该修语料而不是放宽阈值。
 */
final class EvalCorpus {

    /** 语料文件清单：显式列出而不是扫目录，清单本身就是语料构成的说明 */
    static final List<String> FILES = List.of(
            // plain：基线参照，长段落、结构平坦，两种切分产出的块形态接近
            "plain-overview.md",
            "plain-thread-model.md",
            "plain-degrade-policy.md",
            "plain-naming.md",
            // table：大表格，答案在末行，定长切分会把表头切走
            "table-module-ports.md",
            "table-config-keys.md",
            "table-error-codes.md",
            // code：长代码块，答案要靠块首与块中的信息共存
            "code-retriever-sample.md",
            "code-pipeline-wiring.md",
            "code-mq-consumer.md",
            // breadcrumb：四级标题，答案在短小的 H4 正文里，祖先标题在数百字之前
            "breadcrumb-gateway.md",
            "breadcrumb-cache.md",
            "breadcrumb-search.md",
            "breadcrumb-storage.md",
            // json：嵌套配置与记录行，答案要靠键路径与叶子值共存
            "json-app-config.json",
            "json-retriever-registry.json",
            "jsonl-index-records.jsonl");

    /** A 侧（现状）切分参数：与 nebula.ai.rag.chunking 的默认值一致 */
    static final int CHUNK_SIZE = 500;

    /** A 侧（现状）重叠参数 */
    static final int OVERLAP = 100;

    private static final String CORPUS_PATH = "/eval/corpus/";

    private static final String GOLDEN_SET_PATH = "/eval/golden-set.json";

    private EvalCorpus() {
    }

    /**
     * 读取金标集
     */
    static GoldenSet goldenSet() {
        try (InputStream in = EvalCorpus.class.getResourceAsStream(GOLDEN_SET_PATH)) {
            if (in == null) {
                throw new IllegalStateException("金标集资源缺失: " + GOLDEN_SET_PATH);
            }
            return GoldenSet.fromJson(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * 读取单个语料文件的原文
     */
    static String read(String fileName) {
        try (InputStream in = EvalCorpus.class.getResourceAsStream(CORPUS_PATH + fileName)) {
            if (in == null) {
                throw new IllegalStateException("语料资源缺失: " + CORPUS_PATH + fileName);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * A 侧块集：现状 {@link TextChunker} 定长 500/100 切分，ID 为 {@code <文件名>#<序号>}
     * <p>
     * 现状切分不产出块 ID（{@code DocumentChunk} 构造器给的是随机 UUID），
     * 这里显式写入确定性 ID —— 否则前缀判中的金标根本没法用在 A 侧，
     * 也就无从比较。这正是上位设计把「确定性块 ID」列为评测前置的原因。
     */
    static List<DocumentChunk> fixedLengthChunks() {
        TextChunker chunker = new TextChunker(CHUNK_SIZE, OVERLAP);
        List<DocumentChunk> all = new ArrayList<>();
        for (String fileName : FILES) {
            List<String> pieces = chunker.chunk(read(fileName));
            for (int i = 0; i < pieces.size(); i++) {
                DocumentChunk chunk = new DocumentChunk();
                chunk.setId(fileName + "#" + i);
                chunk.setModuleName(fileName);
                chunk.setContent(pieces.get(i));
                all.add(chunk);
            }
        }
        return all;
    }

    /**
     * B 侧块集：结构解析 + 装箱，预算与 A 侧完全相同（500/100）
     * <p>
     * 两侧唯一的差别就是「怎么切」：A 侧按字符数定长切，B 侧沿结构边界切。
     * 预算、语料、检索器、评测深度全部一致，指标差异因此只能归因于切分方式。
     */
    static List<DocumentChunk> structureChunks() {
        ChunkPacker packer = new ChunkPacker(packOptions());
        List<DocumentChunk> all = new ArrayList<>();
        for (String fileName : FILES) {
            all.addAll(packer.pack(fileName,
                    parserFor(fileName).parse(read(fileName), null)));
        }
        return all;
    }

    /**
     * B 侧装箱参数
     * <p>
     * 面包屑注入正文是有意打开的：标题路径与键路径的收益要落到被检索的文本上才成立，
     * 只写 metadata 对纯文本检索没有任何帮助。这一项会写进评测报告的配置快照。
     */
    static PackOptions packOptions() {
        PackOptions options = new PackOptions();
        options.setMaxChunkSize(CHUNK_SIZE);
        options.setOverlap(OVERLAP);
        options.setBreadcrumbToContent(true);
        options.setIdStrategy(ChunkIdStrategy.deterministic());
        return options;
    }

    /**
     * 按扩展名选解析器；语料里只有三种扩展名，出现别的说明清单和文件对不上了
     */
    static StructureParser parserFor(String fileName) {
        if (fileName.endsWith(".md")) {
            return new MarkdownStructureParser();
        }
        if (fileName.endsWith(".jsonl")) {
            return new JsonlStructureParser();
        }
        if (fileName.endsWith(".json")) {
            // 记录预算与装箱预算对齐，避免解析层切出装箱层还要再切一次的元素
            return new JsonStructureParser(CHUNK_SIZE);
        }
        throw new IllegalStateException("语料清单里出现了没有解析器的扩展名: " + fileName);
    }
}
