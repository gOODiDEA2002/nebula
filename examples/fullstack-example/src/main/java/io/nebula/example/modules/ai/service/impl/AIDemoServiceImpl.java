package io.nebula.example.modules.ai.service.impl;

import io.nebula.ai.core.chat.ChatService;
import io.nebula.ai.core.embedding.EmbeddingService;
import io.nebula.ai.core.model.*;
import io.nebula.ai.core.vectorstore.VectorStoreService;
import io.nebula.example.modules.ai.entity.dto.*;
import io.nebula.example.modules.ai.service.AIDemoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI功能演示服务实现类
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "nebula.ai", name = "enabled", havingValue = "true")
public class AIDemoServiceImpl implements AIDemoService {
    
    private final ChatService chatService;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    
    @Override
    public ChatDto.Response chat(ChatDto.Request request) {
        log.info("接收智能聊天请求: {}", request.getMessage());
        
        // 构建聊天请求
        ChatRequest.Builder builder = ChatRequest.builder()
                .addMessage(ChatMessage.user(request.getMessage()));
        
        if (request.getModel() != null) {
            builder.model(request.getModel());
        }
        if (request.getTemperature() != null) {
            builder.temperature(request.getTemperature());
        }
        if (request.getMaxTokens() != null) {
            builder.maxTokens(request.getMaxTokens());
        }
        
        ChatRequest chatRequest = builder.build();
        
        // 调用聊天服务
        ChatResponse chatResponse = chatService.chat(chatRequest);
        
        // 转换响应
        ChatDto.Response response = new ChatDto.Response();
        response.setId(chatResponse.getId());
        response.setContent(chatResponse.getContent());
        response.setModel(chatResponse.getModel());
        response.setFinishReason(chatResponse.getFinishReason());
        response.setTimestamp(chatResponse.getTimestamp() != null ? 
                chatResponse.getTimestamp().toString() : LocalDateTime.now().toString());
        
        if (chatResponse.getUsage() != null) {
            ChatDto.Response.TokenUsage usage = new ChatDto.Response.TokenUsage();
            usage.setPromptTokens(chatResponse.getUsage().getPromptTokens());
            usage.setCompletionTokens(chatResponse.getUsage().getCompletionTokens());
            usage.setTotalTokens(chatResponse.getUsage().getTotalTokens());
            response.setUsage(usage);
        }
        
        return response;
    }
    
    @Override
    public EmbedTextDto.Response embedText(EmbedTextDto.Request request) {
        log.info("接收文本嵌入请求，文本数量: {}", request.getTexts().size());
        
        // 构建嵌入请求
        EmbeddingRequest.Builder builder = EmbeddingRequest.builder()
                .texts(request.getTexts());
        
        if (request.getModel() != null) {
            builder.model(request.getModel());
        }
        
        EmbeddingRequest embeddingRequest = builder.build();
        
        // 调用嵌入服务
        EmbeddingResponse embeddingResponse = embeddingService.embed(embeddingRequest);
        
        // 转换响应
        EmbedTextDto.Response response = new EmbedTextDto.Response();
        response.setId(embeddingResponse.getId());
        response.setModel(embeddingResponse.getModel());
        response.setDimension(embeddingService.getDimension());
        response.setTimestamp(embeddingResponse.getTimestamp() != null ? 
                embeddingResponse.getTimestamp().toString() : LocalDateTime.now().toString());
        
        // 转换嵌入结果
        List<EmbedTextDto.Response.EmbeddingResult> embeddings = embeddingResponse.getEmbeddings().stream()
                .map(e -> {
                    EmbedTextDto.Response.EmbeddingResult result = new EmbedTextDto.Response.EmbeddingResult();
                    result.setIndex(e.getIndex());
                    result.setVector(e.getVector());
                    result.setText(e.getText());
                    return result;
                })
                .collect(Collectors.toList());
        response.setEmbeddings(embeddings);
        
        if (embeddingResponse.getUsage() != null) {
            EmbedTextDto.Response.TokenUsage usage = new EmbedTextDto.Response.TokenUsage();
            usage.setPromptTokens(embeddingResponse.getUsage().getPromptTokens());
            usage.setTotalTokens(embeddingResponse.getUsage().getTotalTokens());
            response.setUsage(usage);
        }
        
        return response;
    }
    
    @Override
    public CalculateSimilarityDto.Response calculateSimilarity(CalculateSimilarityDto.Request request) {
        log.info("计算文本相似度");
        
        // 对两个文本进行向量化
        EmbeddingResponse response1 = embeddingService.embed(request.getText1());
        EmbeddingResponse response2 = embeddingService.embed(request.getText2());
        
        List<Double> vector1 = response1.getFirstVector();
        List<Double> vector2 = response2.getFirstVector();
        
        // 计算相似度
        double similarity = embeddingService.similarity(vector1, vector2);
        
        // 构建响应
        CalculateSimilarityDto.Response response = new CalculateSimilarityDto.Response();
        response.setSimilarity(similarity);
        response.setText1(request.getText1());
        response.setText2(request.getText2());
        response.setTimestamp(LocalDateTime.now().toString());
        
        return response;
    }
    
    @Override
    public AddDocumentDto.Response addDocument(AddDocumentDto.Request request) {
        log.info("添加文档到向量存储: {}", request.getContent().substring(0, Math.min(50, request.getContent().length())));
        
        // 构建文档
        Document document = Document.builder()
                .content(request.getContent())
                .metadata(request.getMetadata())
                .build();
        
        // 添加到向量存储
        boolean success = vectorStoreService.add(document);
        
        // 构建响应
        AddDocumentDto.Response response = new AddDocumentDto.Response();
        response.setDocumentId(document.getId());
        response.setSuccess(success);
        response.setTimestamp(LocalDateTime.now().toString());
        
        return response;
    }
    
    @Override
    public SearchDocumentDto.Response searchDocuments(SearchDocumentDto.Request request) {
        log.info("搜索文档: query={}, topK={}", request.getQuery(), request.getTopK());
        
        // 构建搜索请求
        SearchRequest.Builder builder = SearchRequest.builder()
                .query(request.getQuery())
                .topK(request.getTopK());
        
        if (request.getSimilarityThreshold() != null) {
            builder.similarityThreshold(request.getSimilarityThreshold());
        }
        if (request.getFilter() != null) {
            builder.filter(request.getFilter());
        }
        
        SearchRequest searchRequest = builder.build();
        
        // 执行搜索
        SearchResult searchResult = vectorStoreService.search(searchRequest);
        
        // 转换响应
        SearchDocumentDto.Response response = new SearchDocumentDto.Response();
        response.setQuery(searchResult.getQuery());
        response.setTotalFound(searchResult.getTotalFound());
        response.setMaxScore(searchResult.getMaxScore());
        response.setMinScore(searchResult.getMinScore());
        response.setTimestamp(searchResult.getTimestamp() != null ? 
                searchResult.getTimestamp().toString() : LocalDateTime.now().toString());
        
        // 转换文档结果
        List<SearchDocumentDto.Response.DocumentResult> documents = searchResult.getDocuments().stream()
                .map(doc -> {
                    SearchDocumentDto.Response.DocumentResult result = new SearchDocumentDto.Response.DocumentResult();
                    result.setId(doc.getId());
                    result.setContent(doc.getContent());
                    result.setScore(doc.getScore());
                    result.setMetadata(doc.getMetadata());
                    return result;
                })
                .collect(Collectors.toList());
        response.setDocuments(documents);
        
        return response;
    }
    
    @Override
    public DocumentQADto.Response documentQA(DocumentQADto.Request request) {
        log.info("文档智能问答: {}", request.getQuestion());
        
        // 1. 搜索相关文档
        SearchRequest.Builder searchBuilder = SearchRequest.builder()
                .query(request.getQuestion())
                .topK(request.getContextSize());
        
        if (request.getSimilarityThreshold() != null) {
            searchBuilder.similarityThreshold(request.getSimilarityThreshold());
        }
        
        SearchResult searchResult = vectorStoreService.search(searchBuilder.build());
        
        // 2. 构建上下文
        String context = searchResult.getContents()
                .stream()
                .collect(Collectors.joining("\n\n"));
        
        // 3. 构建提示消息
        List<ChatMessage> messages = List.of(
            ChatMessage.system("你是一个专业的助手。请根据以下文档内容回答用户的问题。\n\n文档内容:\n" + context),
            ChatMessage.user(request.getQuestion())
        );
        
        // 4. 调用聊天服务获取回答
        ChatRequest.Builder chatBuilder = ChatRequest.builder()
                .messages(messages);
        
        if (request.getTemperature() != null) {
            chatBuilder.temperature(request.getTemperature());
        }
        
        ChatResponse chatResponse = chatService.chat(chatBuilder.build());
        
        // 5. 构建响应
        DocumentQADto.Response response = new DocumentQADto.Response();
        response.setAnswer(chatResponse.getContent());
        response.setQuestion(request.getQuestion());
        response.setModel(chatResponse.getModel());
        response.setTimestamp(chatResponse.getTimestamp() != null ? 
                chatResponse.getTimestamp().toString() : LocalDateTime.now().toString());
        
        // 转换上下文文档
        List<DocumentQADto.Response.ContextDocument> contextDocuments = searchResult.getDocuments().stream()
                .map(doc -> {
                    DocumentQADto.Response.ContextDocument contextDoc = new DocumentQADto.Response.ContextDocument();
                    contextDoc.setId(doc.getId());
                    contextDoc.setContent(doc.getContent());
                    contextDoc.setScore(doc.getScore());
                    return contextDoc;
                })
                .collect(Collectors.toList());
        response.setContextDocuments(contextDocuments);
        
        if (chatResponse.getUsage() != null) {
            DocumentQADto.Response.TokenUsage usage = new DocumentQADto.Response.TokenUsage();
            usage.setPromptTokens(chatResponse.getUsage().getPromptTokens());
            usage.setCompletionTokens(chatResponse.getUsage().getCompletionTokens());
            usage.setTotalTokens(chatResponse.getUsage().getTotalTokens());
            response.setUsage(usage);
        }
        
        return response;
    }
}

