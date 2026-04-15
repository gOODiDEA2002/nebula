package io.nebula.example.modules.data.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.nebula.example.modules.data.entity.dos.Product;
import io.nebula.example.modules.data.entity.dto.CreateProductDto;
import io.nebula.example.modules.data.entity.dto.GetProductDto;
import io.nebula.example.modules.data.entity.dto.UpdateProductDto;
import io.nebula.example.modules.data.entity.dto.DeleteProductDto;
import io.nebula.example.modules.data.entity.dto.GetProductsDto;
import io.nebula.example.modules.data.entity.vo.ProductVo;
import io.nebula.example.modules.data.mapper.ProductMapper;
import io.nebula.example.modules.data.service.DataDemoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataDemoServiceImpl implements DataDemoService {

    private final ProductMapper productMapper;

    @Override
    @Transactional
    public CreateProductDto.Response createProduct(CreateProductDto.Request request) {
        Product product = new Product();
        BeanUtils.copyProperties(request, product);
        productMapper.insert(product);
        // 转换为响应对象
        CreateProductDto.Response response = new CreateProductDto.Response();
        response.setId(product.getId());
        return response;
    }

    @Override
    public GetProductDto.Response getProductById(GetProductDto.Request request) {
        Product product = productMapper.selectById(request.getId());
        if (product == null || product.getDeleted()) {
            return new GetProductDto.Response();
        }
        // 转换为响应对象
        ProductVo productVo = new ProductVo();
        BeanUtils.copyProperties(product, productVo);
        GetProductDto.Response response = new GetProductDto.Response();
        response.setProduct(productVo);
        return response;
    }

    @Override
    @Transactional
    public UpdateProductDto.Response updateProduct(UpdateProductDto.Request request) {
        Product existingProduct = productMapper.selectById(request.getId());
        if (existingProduct == null || existingProduct.getDeleted()) {
            return new UpdateProductDto.Response();
        }
        BeanUtils.copyProperties(request, existingProduct);
        existingProduct.setUpdateTime(LocalDateTime.now());
        productMapper.updateById(existingProduct);
        // 转换为响应对象
        UpdateProductDto.Response response = new UpdateProductDto.Response();
        ProductVo productVo = new ProductVo();
        BeanUtils.copyProperties(existingProduct, productVo);
        response.setProduct(productVo);
        return response;
    }

    @Override
    @Transactional
    public DeleteProductDto.Response deleteProduct(DeleteProductDto.Request request) {
        List<Product> existingProducts = productMapper.selectBatchIds(request.getIds()).stream().filter(product -> !product.getDeleted()).collect(Collectors.toList());
        if (existingProducts == null || existingProducts.isEmpty()) {
            return new DeleteProductDto.Response();
        }
        // 逻辑删除
        existingProducts.forEach(product -> {
            product.setDeleted(true);
            product.setUpdateTime(LocalDateTime.now());
            productMapper.updateById(product);
        });
        // 转换为响应对象
        DeleteProductDto.Response response = new DeleteProductDto.Response();
        response.setDeletedCount(existingProducts.size());
        return response;
    }

    @Override
    public GetProductsDto.Response getProducts(GetProductsDto.Request request) {
        Page<Product> productPage = new Page<>(request.getPage(), request.getSize());
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Product::getDeleted, false);
        if (StringUtils.hasText(request.getCategory())) {
            queryWrapper.eq(Product::getCategory, request.getCategory());
        }
        if (StringUtils.hasText(request.getStatus())) {
            queryWrapper.eq(Product::getStatus, request.getStatus());
        }
        if (request.getMinPrice() != null) {
            queryWrapper.ge(Product::getPrice, request.getMinPrice());
        }
        if (request.getMaxPrice() != null) {
            queryWrapper.le(Product::getPrice, request.getMaxPrice());
        }
        if (StringUtils.hasText(request.getKeyword())) {
            queryWrapper.and(wrapper -> wrapper.like(Product::getName, request.getKeyword())
                    .or().like(Product::getDescription, request.getKeyword()));
        }
        //
        GetProductsDto.Response response = new GetProductsDto.Response();
        IPage<Product> resultPage = productMapper.selectPage(productPage, queryWrapper);
        // 使用 convert 方法直接转换分页记录类型
        IPage<ProductVo> voPage = resultPage.convert(product -> {
            ProductVo productVo = new ProductVo();
            BeanUtils.copyProperties(product, productVo);
            return productVo;
         });
        response.setProducts(voPage);
        return response;
    }
}