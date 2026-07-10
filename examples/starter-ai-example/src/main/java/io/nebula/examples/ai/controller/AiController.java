package io.nebula.examples.ai.controller;

import io.nebula.ai.core.chat.ChatService;
import io.nebula.ai.core.embedding.EmbeddingService;
import io.nebula.ai.core.model.Document;
import io.nebula.ai.core.model.EmbeddingResponse;
import io.nebula.ai.core.model.SearchResult;
import io.nebula.ai.core.vectorstore.VectorStoreService;
import io.nebula.core.common.result.Result;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AiController {

    private final ChatService chatService;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;

    public AiController(ObjectProvider<ChatService> chatServiceProvider,
                        ObjectProvider<EmbeddingService> embeddingServiceProvider,
                        ObjectProvider<VectorStoreService> vectorStoreServiceProvider) {
        this.chatService = chatServiceProvider.getIfAvailable();
        this.embeddingService = embeddingServiceProvider.getIfAvailable();
        this.vectorStoreService = vectorStoreServiceProvider.getIfAvailable();
    }

    @GetMapping("/ai/status")
    public Result<AiStatus> status() {
        return Result.success(new AiStatus(
                chatService != null,
                embeddingService != null,
                vectorStoreService != null));
    }

    @GetMapping("/ai/echo")
    public Result<String> echo(@RequestParam(defaultValue = "hello") String q) {
        if (chatService == null) {
            return Result.success("AI disabled");
        }
        String r = chatService.chat(q).getContent();
        return Result.success(r);
    }

    @GetMapping("/ai/embedding")
    public Result<EmbeddingInfo> embedding(@RequestParam String q) {
        if (embeddingService == null) {
            return Result.businessError("AI disabled");
        }
        EmbeddingResponse response = embeddingService.embed(q);
        return Result.success(new EmbeddingInfo(
                response.getModel(),
                response.getFirstVector().size()));
    }

    @PostMapping("/ai/vector/documents")
    public Result<VectorMutation> addDocument(@RequestBody DocumentRequest request) {
        if (vectorStoreService == null) {
            return Result.businessError("AI disabled");
        }
        Document document = Document.builder()
                .id(request.id())
                .content(request.content())
                .addMetadata("source", "starter-ai-example")
                .build();
        return Result.success(new VectorMutation(document.getId(), vectorStoreService.add(document)));
    }

    @GetMapping("/ai/vector/search")
    public Result<SearchResult> search(@RequestParam String q,
                                       @RequestParam(defaultValue = "1") int topK) {
        if (vectorStoreService == null) {
            return Result.businessError("AI disabled");
        }
        return Result.success(vectorStoreService.search(q, topK));
    }

    @DeleteMapping("/ai/vector/documents/{id}")
    public Result<VectorMutation> deleteDocument(@PathVariable String id) {
        if (vectorStoreService == null) {
            return Result.businessError("AI disabled");
        }
        return Result.success(new VectorMutation(id, vectorStoreService.delete(id)));
    }

    public record AiStatus(boolean chat, boolean embedding, boolean vectorStore) {
    }

    public record EmbeddingInfo(String model, int dimension) {
    }

    public record DocumentRequest(String id, String content) {
    }

    public record VectorMutation(String id, boolean success) {
    }
}
