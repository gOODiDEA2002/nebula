package io.nebula.example.modules.search.controller;

import io.nebula.core.common.result.Result;
import io.nebula.example.modules.search.entity.dto.*;
import io.nebula.example.modules.search.service.SearchDemoService;
import io.nebula.search.core.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.*;

/**
 * 搜索演示控制器
 *
 * @author nebula
 */
@Slf4j
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
@Tag(name = "搜索功能", description = "Elasticsearch 搜索功能演示")
@ConditionalOnBean(SearchService.class)
public class SearchController {

    private final SearchDemoService searchDemoService;

    /**
     * 创建产品索引
     */
    @Operation(summary = "创建产品索引", description = "创建 Elasticsearch 产品索引(可选参数:indexName, shards, replicas)")
    @PostMapping("/index/create")
    public Result<CreateIndexDto.Response> createProductIndex(@RequestBody(required = false) CreateIndexDto.Request request) {
        if (request == null) {
            request = CreateIndexDto.Request.builder().build();
        }
        log.info("创建产品索引: indexName={}, shards={}, replicas={}", 
                 request.getIndexName(), request.getShards(), request.getReplicas());
        CreateIndexDto.Response response = searchDemoService.createProductIndex(request);
        return Result.success(response, response.getSuccess() ? "索引创建成功" : "索引创建失败");
    }

    /**
     * 删除产品索引
     */
    @Operation(summary = "删除产品索引", description = "删除 Elasticsearch 产品索引(可选参数:indexName)")
    @DeleteMapping("/index/delete")
    public Result<DeleteIndexDto.Response> deleteProductIndex(@RequestBody(required = false) DeleteIndexDto.Request request) {
        if (request == null) {
            request = new DeleteIndexDto.Request();
        }
        log.info("删除产品索引: indexName={}", request.getIndexName());
        DeleteIndexDto.Response response = searchDemoService.deleteProductIndex(request);
        return Result.success(response, response.getSuccess() ? "索引删除成功" : "索引删除失败");
    }

    /**
     * 检查产品索引是否存在
     */
    @Operation(summary = "检查产品索引是否存在", description = "检查 Elasticsearch 产品索引是否存在(可选参数:indexName)")
    @PostMapping("/index/exists")
    public Result<IndexExistsDto.Response> productIndexExists(@RequestBody(required = false) IndexExistsDto.Request request) {
        if (request == null) {
            request = new IndexExistsDto.Request();
        }
        log.info("检查产品索引是否存在: indexName={}", request.getIndexName());
        IndexExistsDto.Response response = searchDemoService.productIndexExists(request);
        return Result.success(response);
    }

    /**
     * 索引单个产品
     */
    @Operation(summary = "索引单个产品", description = "将单个产品添加到 Elasticsearch 索引")
    @PostMapping("/products/index")
    public Result<IndexProductDto.Response> indexProduct(@Valid @RequestBody IndexProductDto.Request request) {
        log.info("索引产品, ID: {}", request.getProductId());
        IndexProductDto.Response response = searchDemoService.indexProduct(request);
        return Result.success(response, response.getSuccess() ? "产品索引成功" : "产品索引失败");
    }

    /**
     * 批量索引产品
     */
    @Operation(summary = "批量索引产品", description = "批量将产品添加到 Elasticsearch 索引")
    @PostMapping("/products/bulk-index")
    public Result<BulkIndexProductsDto.Response> bulkIndexProducts(@Valid @RequestBody BulkIndexProductsDto.Request request) {
        log.info("批量索引产品, 数量: {}", request.getProductIds().size());
        BulkIndexProductsDto.Response response = searchDemoService.bulkIndexProducts(request);
        return Result.success(response, "批量索引完成");
    }

    /**
     * 搜索产品
     */
    @Operation(summary = "搜索产品", description = "根据多种条件搜索产品")
    @PostMapping("/products/search")
    public Result<SearchProductsDto.Response> searchProducts(@Valid @RequestBody SearchProductsDto.Request request) {
        log.info("搜索产品, 关键词: {}", request.getKeyword());
        SearchProductsDto.Response response = searchDemoService.searchProducts(request);
        return Result.success(response, response.getSuccess() ? "搜索成功" : "搜索失败");
    }

    /**
     * 删除产品索引文档
     */
    @Operation(summary = "删除产品索引文档", description = "从 Elasticsearch 索引中删除指定产品")
    @DeleteMapping("/products/index")
    public Result<DeleteProductIndexDto.Response> deleteProductIndex(@Valid @RequestBody DeleteProductIndexDto.Request request) {
        log.info("删除产品索引文档, ID: {}", request.getProductId());
        DeleteProductIndexDto.Response response = searchDemoService.deleteProductIndex(request);
        return Result.success(response, response.getSuccess() ? "删除成功" : "删除失败");
    }

    /**
     * 获取搜索建议
     */
    @Operation(summary = "获取搜索建议", description = "根据输入获取搜索建议")
    @PostMapping("/products/suggest")
    public Result<SuggestProductsDto.Response> suggestProducts(@Valid @RequestBody SuggestProductsDto.Request request) {
        log.info("获取搜索建议, 文本: {}", request.getText());
        SuggestProductsDto.Response response = searchDemoService.suggestProducts(request);
        return Result.success(response, response.getSuccess() ? "获取建议成功" : "获取建议失败");
    }
}
