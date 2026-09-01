package io.nebula.ai.rag.chunking.pack;

import io.nebula.ai.rag.chunking.ChunkType;
import io.nebula.ai.rag.chunking.DocumentChunk;
import io.nebula.ai.rag.chunking.parse.DocElement;
import io.nebula.ai.rag.chunking.parse.DocElementType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 装箱器：把元素流装成文档块
 * <p>
 * 两段式切分的第二段，<b>与文档格式完全无关</b> —— 它只认 {@link DocElement} 的类型与面包屑，
 * 因此新增一种文档格式不需要动本类一行。
 * <p>
 * 装箱算法（顺序即优先级）：
 * <ol>
 *   <li>顺序扫描元素流；标题只推进面包屑，自己不成块；</li>
 *   <li>同一面包屑下的相邻非原子元素贪心并箱，放不下就封箱开新箱 ——
 *       面包屑一变就封箱，因为跨小节拼在一起的块在检索上是两件事挤在一起；</li>
 *   <li>原子元素（默认表格与代码）独立成块且<b>超限不切</b>；唯一例外是表格：
 *       超限时按数据行切，每片重复表头行，并记下分片编号；</li>
 *   <li>单个非原子元素超限时按分隔符层级递归降级切分，重叠取整句整行边界，
 *       字符级硬切不带重叠；</li>
 *   <li>每块写入面包屑 metadata、按主导元素类型定块类型、按策略生成 ID。</li>
 * </ol>
 * 本类只用 {@link DocumentChunk} 的 setter 写入，不改动该类一行代码。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class ChunkPacker {

    private final PackOptions options;

    public ChunkPacker() {
        this(PackOptions.defaults());
    }

    public ChunkPacker(PackOptions options) {
        if (options == null) {
            throw new IllegalArgumentException("PackOptions 不能为空");
        }
        options.validate();
        this.options = options;
    }

    /**
     * 装箱
     *
     * @param docId    文档标识，参与块 ID 生成，不能为空
     * @param elements 元素流，可为 null
     * @return 文档块列表；无内容返回空表
     */
    public List<DocumentChunk> pack(String docId, List<DocElement> elements) {
        if (docId == null || docId.isBlank()) {
            throw new IllegalArgumentException("docId 不能为空: 块 ID 依赖它");
        }
        List<Draft> drafts = new ArrayList<>();
        if (elements == null || elements.isEmpty()) {
            return List.of();
        }

        List<String> currentBreadcrumb = List.of();
        Box box = null;

        for (DocElement element : elements) {
            if (element == null || element.getText() == null || element.getText().isBlank()) {
                continue;
            }
            DocElementType type = element.getType();

            if (type == DocElementType.HEADING) {
                flush(box, drafts);
                box = null;
                currentBreadcrumb = element.getBreadcrumb();
                continue;
            }

            List<String> breadcrumb = element.getBreadcrumb().isEmpty()
                    ? currentBreadcrumb : element.getBreadcrumb();

            if (box != null && !box.breadcrumb.equals(breadcrumb)) {
                flush(box, drafts);
                box = null;
            }

            if (options.getPreserveTypes().contains(type)) {
                flush(box, drafts);
                box = null;
                emitAtomic(element, breadcrumb, drafts);
                continue;
            }

            String text = element.getText().strip();
            if (measure(text) > options.getMaxChunkSize()) {
                flush(box, drafts);
                box = null;
                for (String piece : splitOversized(text)) {
                    drafts.add(new Draft(piece, breadcrumb, type, null));
                }
                continue;
            }

            if (box == null) {
                box = new Box(breadcrumb);
            }
            if (!box.fits(text, type)) {
                flush(box, drafts);
                box = new Box(breadcrumb);
            }
            box.append(text, type);
        }
        flush(box, drafts);

        return toChunks(docId, drafts);
    }

    // ------------------------------------------------------------------
    // 原子元素
    // ------------------------------------------------------------------

    private void emitAtomic(DocElement element, List<String> breadcrumb, List<Draft> drafts) {
        String text = element.getText().strip();
        if (element.getType() == DocElementType.TABLE
                && measure(text) > options.getMaxChunkSize()) {
            emitTableParts(element, text, breadcrumb, drafts);
            return;
        }
        // 超限也不切：代码块切一半不再是可执行片段，切坏之后的检索价值接近于零
        drafts.add(new Draft(text, breadcrumb, element.getType(), null));
    }

    /**
     * 表格超限时按数据行切，每片重复表头行
     */
    private void emitTableParts(DocElement element, String text, List<String> breadcrumb,
                                List<Draft> drafts) {
        String[] lines = text.split("\n");
        int headerRowCount = Math.min(
                Math.max(0, element.intAttr(DocElement.ATTR_HEADER_ROW_COUNT, 0)), lines.length);
        String header = String.join("\n", List.of(lines).subList(0, headerRowCount));
        List<String> dataLines = List.of(lines).subList(headerRowCount, lines.length);
        if (dataLines.isEmpty()) {
            drafts.add(new Draft(text, breadcrumb, DocElementType.TABLE, null));
            return;
        }

        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder(header);
        boolean hasData = false;
        for (String line : dataLines) {
            String candidate = current.length() == 0 ? line : current + "\n" + line;
            if (hasData && measure(candidate) > options.getMaxChunkSize()) {
                parts.add(current.toString());
                current = new StringBuilder(header);
                hasData = false;
                candidate = current.length() == 0 ? line : current + "\n" + line;
            }
            current.setLength(0);
            current.append(candidate);
            hasData = true;
        }
        parts.add(current.toString());

        for (int i = 0; i < parts.size(); i++) {
            String tablePart = parts.size() > 1 ? (i + 1) + "/" + parts.size() : null;
            drafts.add(new Draft(parts.get(i), breadcrumb, DocElementType.TABLE, tablePart));
        }
    }

    // ------------------------------------------------------------------
    // 超长非原子元素：分隔符层级递归降级
    // ------------------------------------------------------------------

    /**
     * 切分超长文本：先拆成不超预算的最小单元，再带重叠贪心并回块
     */
    private List<String> splitOversized(String text) {
        List<String> units = new ArrayList<>();
        flatten(text, 0, units);
        return packUnits(units);
    }

    /**
     * 把文本拆成「每个都不超预算」的单元；能在粗粒度解决就不往细粒度下潜
     */
    private void flatten(String text, int level, List<String> units) {
        if (measure(text) <= options.getMaxChunkSize()) {
            units.add(text);
            return;
        }
        SeparatorHierarchy separators = options.getSeparators();
        if (level >= separators.levelCount() || separators.isCharacterLevel(level)) {
            units.addAll(hardSplit(text));
            return;
        }
        List<String> segments = splitKeepingSeparator(text, separators.separatorsAt(level));
        if (segments.size() <= 1) {
            // 本级分隔符在文本里根本不存在，直接下潜
            flatten(text, level + 1, units);
            return;
        }
        for (String segment : segments) {
            flatten(segment, level + 1, units);
        }
    }

    /**
     * 贪心并单元；开新块时把上一块尾部的<b>整个单元</b>带过来作为重叠
     */
    private List<String> packUnits(List<String> units) {
        List<String> pieces = new ArrayList<>();
        List<String> current = new ArrayList<>();
        int currentLength = 0;

        for (String unit : units) {
            int unitLength = measure(unit);
            if (!current.isEmpty() && currentLength + unitLength > options.getMaxChunkSize()) {
                pieces.add(String.join("", current));
                List<String> tail = overlapTail(current);
                current = new ArrayList<>(tail);
                currentLength = measure(String.join("", tail));
            }
            current.add(unit);
            currentLength += unitLength;
        }
        if (!current.isEmpty()) {
            pieces.add(String.join("", current));
        }
        return pieces;
    }

    /**
     * 取尾部若干个完整单元作为重叠，总长不超过 overlap
     * <p>
     * 只取整单元：单元边界就是整句或整行，从句子中间截一段当重叠，
     * 两个块都会带上一句读不通的半截话。字符级硬切出来的单元本身就等于预算，
     * 一个都塞不进重叠额度，于是自然退化成「无重叠」。
     */
    private List<String> overlapTail(List<String> current) {
        List<String> tail = new ArrayList<>();
        int length = 0;
        for (int i = current.size() - 1; i >= 0; i--) {
            int unitLength = measure(current.get(i));
            if (length + unitLength > options.getOverlap()) {
                break;
            }
            tail.add(0, current.get(i));
            length += unitLength;
        }
        // 全部单元都进了重叠等于原地踏步，必须至少丢掉一个
        if (tail.size() == current.size() && !tail.isEmpty()) {
            tail.remove(0);
        }
        return tail;
    }

    /**
     * 字符级硬切：兜底路径，切出来的边界不带任何语义
     */
    private List<String> hardSplit(String text) {
        List<String> pieces = new ArrayList<>();
        int max = options.getMaxChunkSize();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + max, text.length());
            // 度量可被替换（例如按 token 计），字符数不等于度量值时收缩到满足预算为止
            while (end > start + 1 && measure(text.substring(start, end)) > max) {
                end = start + Math.max(1, (end - start) * 9 / 10);
            }
            pieces.add(text.substring(start, end));
            start = end;
        }
        return pieces;
    }

    /**
     * 按分隔符切分，分隔符留在前一段末尾，拼接可无损还原原文
     */
    static List<String> splitKeepingSeparator(String text, List<String> separators) {
        List<String> segments = new ArrayList<>();
        int start = 0;
        int index = 0;
        while (index < text.length()) {
            String matched = null;
            for (String separator : separators) {
                if (!separator.isEmpty() && text.startsWith(separator, index)) {
                    matched = separator;
                    break;
                }
            }
            if (matched == null) {
                index++;
                continue;
            }
            int end = index + matched.length();
            segments.add(text.substring(start, end));
            start = end;
            index = end;
        }
        if (start < text.length()) {
            segments.add(text.substring(start));
        }
        return segments;
    }

    // ------------------------------------------------------------------
    // 产出
    // ------------------------------------------------------------------

    private void flush(Box box, List<Draft> drafts) {
        if (box == null || box.isEmpty()) {
            return;
        }
        drafts.add(new Draft(box.text.toString(), box.breadcrumb, box.dominantType(), null));
    }

    private List<DocumentChunk> toChunks(String docId, List<Draft> drafts) {
        List<DocumentChunk> chunks = new ArrayList<>(drafts.size());
        for (Draft draft : drafts) {
            // 切分后的片段可能带着上一级分隔符的尾巴（末尾的空行或换行），
            // 留在块正文里既没有信息量又会让相同内容的块看起来不一样
            String text = draft.text.strip();
            if (text.isEmpty()) {
                continue;
            }
            DocumentChunk chunk = new DocumentChunk();
            chunk.setModuleName(docId);
            chunk.setChunkType(toChunkType(draft.type));
            chunk.setTitle(draft.breadcrumb.isEmpty()
                    ? null : draft.breadcrumb.get(draft.breadcrumb.size() - 1));
            chunk.setContent(renderContent(text, draft.breadcrumb));
            chunk.addMetadata(PackOptions.META_BREADCRUMB, List.copyOf(draft.breadcrumb));
            if (draft.tablePart != null) {
                chunk.addMetadata(PackOptions.META_TABLE_PART, draft.tablePart);
            }
            chunk.setId(options.getIdStrategy().chunkId(docId, chunks.size(), chunk));
            chunks.add(chunk);
        }
        return chunks;
    }

    private String renderContent(String text, List<String> breadcrumb) {
        if (!options.isBreadcrumbToContent() || breadcrumb.isEmpty()) {
            return text;
        }
        return String.join(PackOptions.BREADCRUMB_SEPARATOR, breadcrumb) + "\n" + text;
    }

    private static ChunkType toChunkType(DocElementType type) {
        if (type == null) {
            return ChunkType.SECTION;
        }
        return switch (type) {
            case CODE -> ChunkType.CODE;
            case CONFIG -> ChunkType.CONFIG;
            case TABLE -> ChunkType.TABLE;
            default -> ChunkType.SECTION;
        };
    }

    private int measure(String text) {
        return options.getLengthMeasure().length(text);
    }

    /**
     * 并箱中的块
     */
    private final class Box {

        private final List<String> breadcrumb;

        private final StringBuilder text = new StringBuilder();

        /** 各元素类型贡献的字符数，用于判定主导类型 */
        private final Map<DocElementType, Integer> typeWeights = new LinkedHashMap<>();

        private Box(List<String> breadcrumb) {
            this.breadcrumb = List.copyOf(breadcrumb);
        }

        private boolean isEmpty() {
            return text.length() == 0;
        }

        private boolean fits(String candidate, DocElementType type) {
            if (isEmpty()) {
                return true;
            }
            return measure(text + separatorFor(type) + candidate) <= options.getMaxChunkSize();
        }

        private void append(String candidate, DocElementType type) {
            if (!isEmpty()) {
                text.append(separatorFor(type));
            }
            text.append(candidate);
            typeWeights.merge(type, candidate.length(), Integer::sum);
        }

        /** 列表项与记录之间用单换行，段落之间用空行，拼出来的文本才像原文 */
        private String separatorFor(DocElementType type) {
            return type == DocElementType.LIST_ITEM || type == DocElementType.RECORD
                    ? "\n" : "\n\n";
        }

        private DocElementType dominantType() {
            DocElementType dominant = null;
            int best = -1;
            for (Map.Entry<DocElementType, Integer> entry : typeWeights.entrySet()) {
                if (entry.getValue() > best) {
                    best = entry.getValue();
                    dominant = entry.getKey();
                }
            }
            return dominant;
        }
    }

    /**
     * 待产出的块内容
     */
    private record Draft(String text, List<String> breadcrumb, DocElementType type,
                         String tablePart) {

        private Draft {
            breadcrumb = List.copyOf(breadcrumb);
        }
    }
}
