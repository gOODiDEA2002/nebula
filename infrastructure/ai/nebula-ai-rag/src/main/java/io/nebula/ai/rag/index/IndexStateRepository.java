package io.nebula.ai.rag.index;

import java.util.Map;

/**
 * 索引状态库
 * <p>
 * 逐文档保存，不做批量事务假设 —— 索引管线依赖它「写一个存一个」，
 * 以便半途中断后重入时对齐已完成的部分。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public interface IndexStateRepository {

    /**
     * 加载某个源的全部文档状态（docId → 状态）
     */
    Map<String, DocIndexState> load(String sourceName);

    /**
     * 保存单个文档状态
     */
    void save(String sourceName, DocIndexState state);

    /**
     * 删除单个文档状态
     */
    void remove(String sourceName, String docId);
}
