package io.nebula.example.modules.ai.service;

import io.nebula.example.modules.ai.entity.dto.*;

/**
 * AI功能演示服务接口
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
public interface AIDemoService {
    
    /**
     * 智能聊天
     * 
     * @param request 聊天请求
     * @return 聊天响应
     */
    ChatDto.Response chat(ChatDto.Request request);
    
    /**
     * 文本嵌入（向量化）
     * 
     * @param request 嵌入请求
     * @return 嵌入响应
     */
    EmbedTextDto.Response embedText(EmbedTextDto.Request request);
    
    /**
     * 计算文本相似度
     * 
     * @param request 相似度计算请求
     * @return 相似度计算响应
     */
    CalculateSimilarityDto.Response calculateSimilarity(CalculateSimilarityDto.Request request);
    
    /**
     * 添加文档到向量存储
     * 
     * @param request 添加文档请求
     * @return 添加文档响应
     */
    AddDocumentDto.Response addDocument(AddDocumentDto.Request request);
    
    /**
     * 搜索相似文档
     * 
     * @param request 搜索请求
     * @return 搜索响应
     */
    SearchDocumentDto.Response searchDocuments(SearchDocumentDto.Request request);
    
    /**
     * 文档智能问答（RAG）
     * 
     * @param request 问答请求
     * @return 问答响应
     */
    DocumentQADto.Response documentQA(DocumentQADto.Request request);
}

