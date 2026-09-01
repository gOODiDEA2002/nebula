package io.nebula.ai.rag.index;

import java.util.List;

/**
 * Sink 写入部分失败异常（J8）
 * <p>
 * 携带已成功的块 ID 清单，便于上层重入时对齐「哪些已经落库」。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class IndexSinkException extends RuntimeException {

    private final String sinkName;
    private final String docId;
    private final List<String> succeededIds;

    public IndexSinkException(String sinkName, String docId, List<String> succeededIds,
                              String message, Throwable cause) {
        super("[" + sinkName + "] 文档 " + docId + " 写入部分失败: " + message
                + "; 已成功 ID=" + succeededIds, cause);
        this.sinkName = sinkName;
        this.docId = docId;
        this.succeededIds = List.copyOf(succeededIds);
    }

    public String getSinkName() {
        return sinkName;
    }

    public String getDocId() {
        return docId;
    }

    /** 已成功写入的块 ID 清单 */
    public List<String> getSucceededIds() {
        return succeededIds;
    }
}
