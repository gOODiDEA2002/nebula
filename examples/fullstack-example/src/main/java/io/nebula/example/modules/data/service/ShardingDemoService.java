package io.nebula.example.modules.data.service;

import io.nebula.example.modules.data.entity.dto.CreateShardingOrderDto;
import io.nebula.example.modules.data.entity.dto.GetShardingOrderDto;
import io.nebula.example.modules.data.entity.dto.GetShardingOrdersDto;

/**
 * 分片演示服务接口
 * 演示 Nebula 数据访问层的分库分表功能
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
public interface ShardingDemoService {

    /**
     * 创建订单
     * 根据分片规则自动路由到对应的数据库和表
     *
     * @param request 订单信息
     * @return 创建的订单
     */
    CreateShardingOrderDto.Response createOrder(CreateShardingOrderDto.Request request);

    /**
     * 根据订单ID和用户ID获取订单
     * 精确路由：通过分片键快速定位到具体的库表
     *
     * @param request 订单查询请求
     * @return 订单信息
     */
    GetShardingOrderDto.Response getOrderById(GetShardingOrderDto.Request request);

    /**
     * 根据用户ID获取订单列表
     * 单库查询：根据用户ID路由到对应的数据库，跨表查询
     *
     * @param request 用户订单查询请求
     * @return 订单列表
     */
    GetShardingOrdersDto.Response getOrdersByUserId(GetShardingOrdersDto.Request request);
}
