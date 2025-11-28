package io.nebula.mcp.core.indexer;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * 文档索引器接口
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
public interface DocumentIndexer {
    
    /**
     * 索引单个文档
     * 
     * @param filePath 文档路径
     * @return 索引结果
     */
    IndexResult indexDocument(Path filePath);
    
    /**
     * 批量索引文档
     * 
     * @param directory 目录路径
     * @return 索引结果列表
     */
    List<IndexResult> indexDirectory(Path directory);
    
    /**
     * 删除文档索引
     * 
     * @param documentId 文档ID
     * @return 是否删除成功
     */
    boolean deleteDocument(String documentId);
    
    /**
     * 清空所有索引
     */
    void clearAll();
    
    /**
     * 获取索引统计信息
     * 
     * @return 统计信息
     */
    IndexStats getStats();
    
    /**
     * 索引结果
     */
    record IndexResult(
            String documentId,
            String filePath,
            int chunkCount,
            boolean success,
            String message
    ) {}
    
    /**
     * 索引统计
     */
    record IndexStats(
            int totalDocuments,
            int totalChunks,
            Map<String, Integer> documentsByType
    ) {}
}

