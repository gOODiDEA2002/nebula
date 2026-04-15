package io.nebula.example.modules.data.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;

import io.nebula.example.modules.data.entity.dto.CreateProductDto;
import io.nebula.example.modules.data.entity.dto.GetProductDto;
import io.nebula.example.modules.data.entity.dto.GetProductsDto;
import io.nebula.example.modules.data.entity.dto.UpdateProductDto;
import io.nebula.example.modules.data.entity.dto.DeleteProductDto;
import io.nebula.example.modules.data.service.DataDemoService;  
import io.nebula.core.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import io.nebula.example.modules.data.entity.dos.Product;
import io.nebula.example.modules.data.entity.vo.ProductVo;
import org.springframework.beans.BeanUtils;

/**
 * 数据访问演示控制器
 * 演示 Nebula 数据访问层的完整功能
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
@RestController
@RequestMapping("/data")
@RequiredArgsConstructor
@Validated
@Tag(name = "数据访问演示", description = "Nebula 数据访问层功能演示API")
public class DataController {
    
    private final DataDemoService dataDemoService;
    
    @Operation(summary = "获取产品详情", description = "根据产品ID获取详细信息")
    @GetMapping("/products/")
    public Result<GetProductsDto.Response> getProducts() {
        log.info("获取产品列表");
        GetProductsDto.Request request = new GetProductsDto.Request();
        request.setPage(1);
        request.setSize(10);
        GetProductsDto.Response response = dataDemoService.getProducts(request);
        return Result.success(response, "获取产品列表成功");
    }

    @Operation(summary = "获取产品详情", description = "根据产品ID获取详细信息")
    @GetMapping("/products/test")
    public GetProductsDto.Response getProducts2() {
        log.info("获取产品列表");
        GetProductsDto.Request request = new GetProductsDto.Request();
        request.setPage(1);
        request.setSize(10);
        GetProductsDto.Response response = dataDemoService.getProducts(request);
        return response;
    }

    @Operation(summary = "创建产品", description = "创建新的产品信息")
    @PostMapping("/products")
    public Result<CreateProductDto.Response> createProduct(@Valid @RequestBody CreateProductDto.Request request) {
        log.info("接收创建产品请求: {}", request.getName());
        CreateProductDto.Response response = dataDemoService.createProduct(request);
        return Result.success(response);
    }
    
    @Operation(summary = "获取产品详情", description = "根据产品ID获取详细信息")
    @GetMapping("/products")
    public Result<GetProductDto.Response> getProduct( @Valid GetProductDto.Request request ) {
        log.info("获取产品详情，ID: {}", request.getId());
        GetProductDto.Response response = dataDemoService.getProductById(request);
        return Result.success(response, "获取产品详情成功");
    }
    
    @Operation(summary = "更新产品", description = "更新产品信息")
    @PutMapping("/products")
    public Result<UpdateProductDto.Response> updateProduct( @Valid @RequestBody UpdateProductDto.Request request) {
        log.info("更新产品，ID: {}", request.getId());
        UpdateProductDto.Response response = dataDemoService.updateProduct(request);
        return Result.success(response, "产品更新成功");
    }
    
    @Operation(summary = "删除产品", description = "逻辑删除产品")
    @DeleteMapping("/products")
    public Result<DeleteProductDto.Response> deleteProduct( @Valid @RequestBody DeleteProductDto.Request request) {
        log.info("删除产品，ID: {}", request.getIds());
        DeleteProductDto.Response response = dataDemoService.deleteProduct(request);
        return Result.success(response, "产品删除成功");
    }
    
    @Operation(summary = "分页查询", description = "根据多个条件查询产品")
    @PostMapping("/products/list")
    public Result<GetProductsDto.Response> getProducts( @Valid GetProductsDto.Request request) {
        log.info("复杂条件查询产品");
        GetProductsDto.Response response = dataDemoService.getProducts(request);
        return Result.success(response, "复杂条件查询成功");
    }

}
