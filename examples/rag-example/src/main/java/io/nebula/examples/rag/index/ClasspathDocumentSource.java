package io.nebula.examples.rag.index;

import io.nebula.ai.rag.config.RagProperties;
import io.nebula.ai.rag.index.DocumentSource;
import io.nebula.ai.rag.index.Sha256ContentHash;
import io.nebula.ai.rag.index.SourceDocument;
import io.nebula.ai.rag.chunking.parse.MarkdownStructureParser;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 读 classpath {@code docs/*.md} 的文档源。
 * <p>
 * 快照语义：每次 {@link #snapshot()} 返回全部文档，供框架规划器对齐增量与删除。
 * <p>
 * <b>contentHash 设计要点</b>：把切块参数掺进哈希（{@code content + "\n" + size + "/" + overlap}）。
 * 框架规划器以 contentHash 判「内容是否变化」，切块参数不在其中；内置文档内容永不变，
 * 若只哈希内容，改 {@code CHUNK_SIZE} 后 index 会判「未变」直接跳过，调优教程失效。
 * 掺入切块参数后，改参数即判「更新」，管线自动先删旧块再写新块，无孤儿块、无顺序要求。
 * 通用原则：哈希应覆盖所有影响产出块的输入。
 *
 * @author Nebula Framework
 */
public class ClasspathDocumentSource implements DocumentSource {

    /** 源名称，用作状态库分区键 */
    public static final String SOURCE_NAME = "rag-example-docs";

    private static final String DOCS_PATTERN = "classpath:docs/*.md";

    private final RagProperties ragProperties;
    private final ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    /** 令快照返回空表，用于演示「快照里没有即删除」的对齐语义 */
    private volatile boolean cleared = false;

    public ClasspathDocumentSource(RagProperties ragProperties) {
        this.ragProperties = ragProperties;
    }

    @Override
    public String name() {
        return SOURCE_NAME;
    }

    @Override
    public List<SourceDocument> snapshot() {
        if (cleared) {
            return List.of();
        }
        int size = ragProperties.getChunking().getSize();
        int overlap = ragProperties.getChunking().getOverlap();
        List<SourceDocument> docs = new ArrayList<>();
        try {
            Resource[] resources = resolver.getResources(DOCS_PATTERN);
            List<Resource> ordered = new ArrayList<>(List.of(resources));
            // 固定顺序，保证块序号与状态在多次运行间稳定
            ordered.sort(Comparator.comparing(r -> safeFilename(r)));
            for (Resource resource : ordered) {
                String filename = safeFilename(resource);
                if (filename == null || !filename.endsWith(".md")) {
                    continue;
                }
                String id = filename.substring(0, filename.length() - ".md".length());
                String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                SourceDocument doc = new SourceDocument(id, content, MarkdownStructureParser.FORMAT);
                doc.setContentHash(Sha256ContentHash.of(content + "\n" + size + "/" + overlap));
                docs.add(doc);
            }
        } catch (Exception e) {
            throw new IllegalStateException("读取 classpath docs/*.md 失败: " + e.getMessage(), e);
        }
        return docs;
    }

    /** 令后续快照返回空表（DELETE /rag/documents 使用） */
    public void clear() {
        this.cleared = true;
    }

    /** 恢复正常快照 */
    public void restore() {
        this.cleared = false;
    }

    private static String safeFilename(Resource resource) {
        try {
            return resource.getFilename();
        } catch (Exception e) {
            return null;
        }
    }
}
