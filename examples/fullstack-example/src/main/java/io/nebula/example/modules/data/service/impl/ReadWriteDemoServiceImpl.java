package io.nebula.example.modules.data.service.impl;

import io.nebula.data.persistence.readwrite.annotation.ReadDataSource;
import io.nebula.data.persistence.readwrite.annotation.WriteDataSource;
import io.nebula.example.modules.data.entity.dos.Product;
import io.nebula.example.modules.data.entity.dto.CreateReadWriteProductDto;
import io.nebula.example.modules.data.entity.dto.GetReadWriteProductDto;
import io.nebula.example.modules.data.entity.dto.UpdateReadWriteProductDto;
import io.nebula.example.modules.data.entity.vo.ProductVo;
import io.nebula.example.modules.data.mapper.ProductMapper;
import io.nebula.example.modules.data.service.ReadWriteDemoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 读写分离演示服务实现
 * 严格遵循 DTO 规范，演示读写分离功能
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReadWriteDemoServiceImpl implements ReadWriteDemoService {

    private final ProductMapper productMapper;

    @Override
    @WriteDataSource(cluster = "default", description = "创建产品-写操作")
    @Transactional(rollbackFor = Exception.class)
    public CreateReadWriteProductDto.Response createProduct(CreateReadWriteProductDto.Request request) {
        log.info("开始创建产品（读写分离演示）: {}", request.getName());
        
        Product product = new Product();
        BeanUtils.copyProperties(request, product);
        product.setCreateTime(LocalDateTime.now());
        product.setUpdateTime(LocalDateTime.now());
        product.setDeleted(false);
        
        productMapper.insert(product);
        
        log.info("产品创建成功（读写分离演示），ID: {}，数据源: 主库", product.getId());
        
        CreateReadWriteProductDto.Response response = new CreateReadWriteProductDto.Response();
        response.setId(product.getId());
        return response;
    }

    @Override
    @ReadDataSource(cluster = "default", description = "获取产品详情-读操作")
    public GetReadWriteProductDto.Response getProductById(GetReadWriteProductDto.Request request) {
        log.info("开始获取产品详情（读写分离演示），ID: {}", request.getId());
        
        Product product = productMapper.selectById(request.getId());
        
        GetReadWriteProductDto.Response response = new GetReadWriteProductDto.Response();
        if (product != null && !product.getDeleted()) {
            ProductVo productVo = new ProductVo();
            BeanUtils.copyProperties(product, productVo);
            response.setProduct(productVo);
            log.info("产品详情获取成功（读写分离演示），ID: {}，数据源: 从库", request.getId());
        } else {
            log.warn("产品未找到或已删除（读写分离演示），ID: {}", request.getId());
        }
        
        return response;
    }

    @Override
    @WriteDataSource(cluster = "default", description = "更新产品-写操作")
    @Transactional(rollbackFor = Exception.class)
    public UpdateReadWriteProductDto.Response updateProduct(UpdateReadWriteProductDto.Request request) {
        log.info("开始更新产品（读写分离演示），ID: {}", request.getId());
        
        Product existingProduct = productMapper.selectById(request.getId());
        if (existingProduct == null || existingProduct.getDeleted()) {
            log.warn("更新失败：产品未找到或已删除（读写分离演示），ID: {}", request.getId());
            return new UpdateReadWriteProductDto.Response();
        }
        
        // 只更新非空字段
        if (request.getName() != null) {
            existingProduct.setName(request.getName());
        }
        if (request.getDescription() != null) {
            existingProduct.setDescription(request.getDescription());
        }
        if (request.getPrice() != null) {
            existingProduct.setPrice(request.getPrice());
        }
        if (request.getCategory() != null) {
            existingProduct.setCategory(request.getCategory());
        }
        if (request.getStock() != null) {
            existingProduct.setStockQuantity(request.getStock());
        }
        existingProduct.setUpdateTime(LocalDateTime.now());
        
        productMapper.updateById(existingProduct);
        
        log.info("产品更新成功（读写分离演示），ID: {}，数据源: 主库", existingProduct.getId());
        
        UpdateReadWriteProductDto.Response response = new UpdateReadWriteProductDto.Response();
        ProductVo productVo = new ProductVo();
        BeanUtils.copyProperties(existingProduct, productVo);
        response.setProduct(productVo);
        
        return response;
    }
}