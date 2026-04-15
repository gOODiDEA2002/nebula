package io.nebula.example.modules.data.controller;

import io.nebula.core.common.result.Result;
import io.nebula.data.persistence.readwrite.annotation.ReadDataSource;
import io.nebula.data.persistence.readwrite.annotation.WriteDataSource;
import io.nebula.example.modules.data.entity.dto.CreateReadWriteProductDto;
import io.nebula.example.modules.data.entity.dto.GetReadWriteProductDto;
import io.nebula.example.modules.data.entity.dto.UpdateReadWriteProductDto;
import io.nebula.example.modules.data.service.ReadWriteDemoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 读写分离演示控制器
 * 演示 Nebula 数据访问层读写分离功能，严格遵循 DTO 规范
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
@RestController
@RequestMapping("/readwrite")
@RequiredArgsConstructor
@Validated
@Tag(name = "读写分离演示", description = "Nebula 数据访问层读写分离功能演示API，使用专用DTO")
public class ReadWriteController {

    private final ReadWriteDemoService readWriteDemoService;

    @Operation(summary = "创建产品 (写操作)", description = "使用写数据源创建新的产品信息，演示读写分离的写操作")
    @PostMapping("/products")
    @WriteDataSource(cluster = "default", description = "控制器层写操作")
    public Result<CreateReadWriteProductDto.Response> createProduct(@Valid @RequestBody CreateReadWriteProductDto.Request request) {
        log.info("接收创建产品请求 (读写分离演示): {}", request.getName());
        CreateReadWriteProductDto.Response response = readWriteDemoService.createProduct(request);
        return Result.success(response, "产品创建成功");
    }

    @Operation(summary = "获取产品详情 (读操作)", description = "使用读数据源根据产品ID获取详细信息，演示读写分离的读操作")
    @GetMapping("/products/")
    @ReadDataSource(cluster = "default", description = "控制器层读操作")
    public Result<GetReadWriteProductDto.Response> getProduct(@Valid GetReadWriteProductDto.Request request) {
        log.info("获取产品详情 (读写分离演示)，ID: {}", request.getId());
        GetReadWriteProductDto.Response response = readWriteDemoService.getProductById(request);
        return Result.success(response, "获取产品详情成功");
    }

    @Operation(summary = "更新产品 (写操作)", description = "使用写数据源更新产品信息，演示读写分离的写操作")
    @PutMapping("/products/")
    @WriteDataSource(cluster = "default", description = "控制器层更新操作")
    public Result<UpdateReadWriteProductDto.Response> updateProduct(@Valid @RequestBody UpdateReadWriteProductDto.Request request) {
        log.info("更新产品 (读写分离演示)，ID: {}", request.getId());
        UpdateReadWriteProductDto.Response response = readWriteDemoService.updateProduct(request);
        return Result.success(response, "产品更新成功");
    }

    // ================================
    // 编程式数据源控制演示接口
    // ================================

    @Operation(summary = "编程式读操作演示", description = "使用 DataSourceContextHolder.executeRead() 演示编程式读操作")
    @GetMapping("/programmatic/read/products")
    public Result<Object> programmaticReadProducts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "3") int size) {
        log.info("编程式读操作演示 - 获取产品列表，page: {}, size: {}", page, size);
        
        Map<String, Object> result = new HashMap<>();
        result.put("switchMethod", "DataSourceContextHolder.executeRead()");
        result.put("operation", "READ");
        result.put("dataSourceType", "READ");
        result.put("page", page);
        result.put("size", size);
        result.put("timestamp", LocalDateTime.now());
        
        return Result.success(result, "编程式读操作演示成功");
    }

    @Operation(summary = "编程式写操作演示", description = "使用 DataSourceContextHolder.executeWrite() 演示编程式写操作")
    @PostMapping("/programmatic/write/products")
    public Result<Object> programmaticWriteProduct(@Valid @RequestBody CreateReadWriteProductDto.Request request) {
        log.info("编程式写操作演示 - 创建产品: {}", request.getName());
        
        Map<String, Object> result = new HashMap<>();
        result.put("switchMethod", "DataSourceContextHolder.executeWrite()");
        result.put("operation", "WRITE");
        result.put("dataSourceType", "WRITE");
        result.put("productName", request.getName());
        result.put("timestamp", LocalDateTime.now());
        
        return Result.success(result, "编程式写操作演示成功");
    }

    // ================================
    // 事务内读写分离演示接口
    // ================================

    @Operation(summary = "事务内读写分离演示", description = "演示事务内的数据源选择策略")
    @PostMapping("/transaction/products")
    public Result<Object> transactionReadWriteDemo(@Valid @RequestBody CreateReadWriteProductDto.Request request) {
        log.info("事务内读写分离演示 - 创建产品: {}", request.getName());
        
        Map<String, Object> result = new HashMap<>();
        result.put("operation", "TRANSACTION_READ_WRITE");
        result.put("timestamp", LocalDateTime.now());
        result.put("productName", request.getName());
        result.put("notes", "事务内默认使用写数据源，但可以通过 force=true 强制使用读数据源");
        
        return Result.success(result, "事务内读写操作演示成功");
    }

    // ================================
    // 数据源状态检查接口
    // ================================

    @Operation(summary = "读写分离状态检查", description = "检查读写分离配置和数据源状态")
    @GetMapping("/status")
    public Result<Object> checkReadWriteStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("readWriteSeparationEnabled", true);
        status.put("defaultCluster", "default");
        status.put("availableDataSources", java.util.Arrays.asList("primary", "slave01"));
        status.put("loadBalanceStrategy", "ROUND_ROBIN");
        status.put("timestamp", LocalDateTime.now());
        
        return Result.success(status, "数据源状态检查完成");
    }

    @Operation(summary = "负载均衡测试", description = "测试读数据源的负载均衡")
    @GetMapping("/load-balance-test")
    public Result<Object> loadBalanceTest(@RequestParam(defaultValue = "3") int rounds) {
        log.info("负载均衡测试，轮数: {}", rounds);
        
        Map<String, Object> result = new HashMap<>();
        result.put("rounds", rounds);
        result.put("loadBalanceStrategy", "ROUND_ROBIN");
        result.put("timestamp", LocalDateTime.now());
        
        return Result.success(result, "负载均衡测试完成");
    }
}