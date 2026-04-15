package io.nebula.example.modules.ai.controller;

import io.nebula.core.common.result.Result;
import io.nebula.example.modules.ai.entity.dto.*;
import io.nebula.example.modules.ai.service.AIDemoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * AI功能演示控制器
 * 演示 Nebula AI 模块的完整功能
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Validated
@ConditionalOnProperty(prefix = "nebula.ai", name = "enabled", havingValue = "true")
@Tag(name = "AI功能演示", description = "Nebula AI模块功能演示API")
public class AIController {
    
    private final AIDemoService aiDemoService;
    
    @Operation(summary = "智能聊天", description = "与AI助手进行对话")
    @PostMapping("/chat")
    public Result<ChatDto.Response> chat(@Valid @RequestBody ChatDto.Request request) {
        log.info("接收智能聊天请求: {}", request.getMessage());
        ChatDto.Response response = aiDemoService.chat(request);
        return Result.success(response, "聊天成功");
    }
    
    @Operation(summary = "文本嵌入", description = "将文本转换为向量")
    @PostMapping("/embed")
    public Result<EmbedTextDto.Response> embedText(@Valid @RequestBody EmbedTextDto.Request request) {
        log.info("接收文本嵌入请求，文本数量: {}", request.getTexts().size());
        EmbedTextDto.Response response = aiDemoService.embedText(request);
        return Result.success(response, "文本嵌入成功");
    }
    
    @Operation(summary = "计算文本相似度", description = "计算两个文本之间的相似度")
    @PostMapping("/similarity")
    public Result<CalculateSimilarityDto.Response> calculateSimilarity(
            @Valid @RequestBody CalculateSimilarityDto.Request request) {
        log.info("计算文本相似度");
        CalculateSimilarityDto.Response response = aiDemoService.calculateSimilarity(request);
        return Result.success(response, "相似度计算成功");
    }
    
    @Operation(summary = "添加文档", description = "添加文档到向量存储")
    @PostMapping("/documents")
    public Result<AddDocumentDto.Response> addDocument(@Valid @RequestBody AddDocumentDto.Request request) {
        log.info("添加文档到向量存储");
        AddDocumentDto.Response response = aiDemoService.addDocument(request);
        return Result.success(response, "文档添加成功");
    }
    
    @Operation(summary = "搜索文档", description = "语义搜索相似文档")
    @PostMapping("/documents/search")
    public Result<SearchDocumentDto.Response> searchDocuments(@Valid @RequestBody SearchDocumentDto.Request request) {
        log.info("搜索文档: {}", request.getQuery());
        SearchDocumentDto.Response response = aiDemoService.searchDocuments(request);
        return Result.success(response, "文档搜索成功");
    }
    
    @Operation(summary = "文档问答", description = "基于文档的智能问答（RAG）")
    @PostMapping("/qa")
    public Result<DocumentQADto.Response> documentQA(@Valid @RequestBody DocumentQADto.Request request) {
        log.info("文档智能问答: {}", request.getQuestion());
        DocumentQADto.Response response = aiDemoService.documentQA(request);
        return Result.success(response, "问答成功");
    }
}

