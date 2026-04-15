package io.nebula.example.modules.data.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.nebula.example.modules.data.entity.dos.Product;
import io.nebula.example.modules.data.entity.dto.CreateProductDto;
import io.nebula.example.modules.data.entity.dto.GetProductDto;
import io.nebula.example.modules.data.entity.dto.UpdateProductDto;
import io.nebula.example.modules.data.entity.dto.DeleteProductDto;
import io.nebula.example.modules.data.entity.dto.GetProductsDto;


/**
 * 数据访问演示服务接口
 * 演示 Nebula 数据访问层的各种功能
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
public interface DataDemoService {
    
    /**
     * 创建产品
     * 
     * @param productDto 产品信息
     * @return 创建的产品
     */
    CreateProductDto.Response createProduct(CreateProductDto.Request request);
    
    /**
     * 根据ID获取产品
     * 
     * @param id 产品ID
     * @return 产品信息
     */
    GetProductDto.Response getProductById( GetProductDto.Request request );
    
    /**
     * 更新产品
     * 
     * @param productDto 产品信息
     * @return 更新的产品
     */
    UpdateProductDto.Response updateProduct(UpdateProductDto.Request request);
    
    /**
     * 删除产品
     * 
     * @param id 产品ID
     * @return 是否删除成功
     */
    DeleteProductDto.Response deleteProduct(DeleteProductDto.Request request);
    
    /**
     * 分页查询产品
     * 
     * @param request 请求参数
     * @return 分页结果
     */
    GetProductsDto.Response getProducts(GetProductsDto.Request request);

}
