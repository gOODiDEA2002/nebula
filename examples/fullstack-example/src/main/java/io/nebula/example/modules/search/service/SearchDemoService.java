package io.nebula.example.modules.search.service;

import io.nebula.example.modules.search.entity.dto.*;

/**
 * 搜索演示服务接口
 *
 * @author nebula
 */
public interface SearchDemoService {
    
    /**
     * 创建产品索引
     */
    CreateIndexDto.Response createProductIndex(CreateIndexDto.Request request);
    
    /**
     * 删除产品索引
     */
    DeleteIndexDto.Response deleteProductIndex(DeleteIndexDto.Request request);
    
    /**
     * 检查产品索引是否存在
     */
    IndexExistsDto.Response productIndexExists(IndexExistsDto.Request request);
    
    /**
     * 索引单个产品
     */
    IndexProductDto.Response indexProduct(IndexProductDto.Request request);
    
    /**
     * 批量索引产品
     */
    BulkIndexProductsDto.Response bulkIndexProducts(BulkIndexProductsDto.Request request);
    
    /**
     * 搜索产品
     */
    SearchProductsDto.Response searchProducts(SearchProductsDto.Request request);
    
    /**
     * 删除产品索引文档
     */
    DeleteProductIndexDto.Response deleteProductIndex(DeleteProductIndexDto.Request request);
    
    /**
     * 搜索建议
     */
    SuggestProductsDto.Response suggestProducts(SuggestProductsDto.Request request);
}
