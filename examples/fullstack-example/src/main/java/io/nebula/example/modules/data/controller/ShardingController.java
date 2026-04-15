package io.nebula.example.modules.data.controller;

import io.nebula.core.common.result.Result;
import io.nebula.example.modules.data.entity.dto.CreateShardingOrderDto;
import io.nebula.example.modules.data.entity.dto.GetShardingOrderDto;
import io.nebula.example.modules.data.entity.dto.GetShardingOrdersDto;
import io.nebula.example.modules.data.service.ShardingDemoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 分片演示控制器
 * 演示 Nebula 数据访问层分库分表功能，严格遵循 DTO 规范
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
@RestController
@RequestMapping("/sharding")
@RequiredArgsConstructor
@Validated
@Tag(name = "分片演示", description = "Nebula 数据访问层分库分表功能演示API，使用专用DTO")
public class ShardingController {

    private final ShardingDemoService shardingDemoService;

    @Operation(summary = "创建订单", description = "创建新的订单信息，将根据分片规则路由到不同的库和表")
    @PostMapping("/orders")
    public Result<CreateShardingOrderDto.Response> createOrder(@Valid @RequestBody CreateShardingOrderDto.Request request) {
        log.info("接收创建订单请求 (分片演示): userId={}, orderId={}", request.getUserId(), request.getOrderId());
        CreateShardingOrderDto.Response response = shardingDemoService.createOrder(request);
        return Result.success(response, "订单创建成功");
    }

    @Operation(summary = "获取订单详情", description = "根据订单ID和用户ID获取详细信息，将根据分片规则路由到对应的库和表")
    @GetMapping("/orders/")
    public Result<GetShardingOrderDto.Response> getOrder(@Valid GetShardingOrderDto.Request request) {
        log.info("获取订单详情 (分片演示): userId={}, orderId={}", request.getUserId(), request.getOrderId());
        GetShardingOrderDto.Response response = shardingDemoService.getOrderById(request);
        return Result.success(response, "获取订单详情成功");
    }

    @Operation(summary = "查询用户订单列表", description = "根据用户ID查询订单列表，将根据分片规则路由到对应的库")
    @GetMapping("/orders/user/{userId}")
    public Result<GetShardingOrdersDto.Response> getOrdersByUserId(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer size) {
        log.info("查询用户订单列表 (分片演示): userId={}, page={}, size={}", userId, page, size);
        
        GetShardingOrdersDto.Request request = new GetShardingOrdersDto.Request();
        request.setUserId(userId);
        request.setPage(page);
        request.setSize(size);
        
        GetShardingOrdersDto.Response response = shardingDemoService.getOrdersByUserId(request);
        return Result.success(response, "查询用户订单列表成功");
    }
}