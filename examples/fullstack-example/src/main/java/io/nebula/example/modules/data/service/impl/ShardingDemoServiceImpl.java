package io.nebula.example.modules.data.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.nebula.example.modules.data.entity.dos.Order;
import io.nebula.example.modules.data.entity.dto.CreateShardingOrderDto;
import io.nebula.example.modules.data.entity.dto.GetShardingOrderDto;
import io.nebula.example.modules.data.entity.dto.GetShardingOrdersDto;
import io.nebula.example.modules.data.entity.vo.OrderVo;
import io.nebula.example.modules.data.mapper.OrderMapper;
import io.nebula.example.modules.data.service.ShardingDemoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 分片演示服务实现
 * 演示 Nebula 数据访问层的分库分表功能
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShardingDemoServiceImpl implements ShardingDemoService {

    private final OrderMapper orderMapper;

    @Override
    @Transactional // 写操作通常需要事务
    public CreateShardingOrderDto.Response createOrder(CreateShardingOrderDto.Request request) {
        Order order = new Order();
        BeanUtils.copyProperties(request, order);
        
        // 设置创建时间
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        order.setDeleted(false);
        
        // 如果没有指定订单ID，由ShardingSphere的雪花算法自动生成
        if (request.getOrderId() != null) {
            order.setId(request.getOrderId());
        }
        
        orderMapper.insert(order);
        
        log.info("订单创建成功，订单ID: {}, 用户ID: {}, 路由规则: 用户ID {} -> ds{}, 订单ID {} -> t_order_{}", 
                order.getId(), order.getUserId(),
                order.getUserId(), order.getUserId() % 2,
                order.getId(), order.getId() % 2);
        
        CreateShardingOrderDto.Response response = new CreateShardingOrderDto.Response();
        response.setOrderId(order.getId());
        return response;
    }

    @Override
    public GetShardingOrderDto.Response getOrderById(GetShardingOrderDto.Request request) {
        // 使用分片键进行精确路由查询
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getId, request.getOrderId())
               .eq(Order::getUserId, request.getUserId())  // 分片键，用于精确路由
               .eq(Order::getDeleted, false);
        
        Order order = orderMapper.selectOne(wrapper);
        
        log.info("订单查询，订单ID: {}, 用户ID: {}, 路由规则: 用户ID {} -> ds{}, 订单ID {} -> t_order_{}", 
                request.getOrderId(), request.getUserId(),
                request.getUserId(), request.getUserId() % 2,
                request.getOrderId(), request.getOrderId() % 2);
        
        GetShardingOrderDto.Response response = new GetShardingOrderDto.Response();
        if (order != null) {
            OrderVo orderVo = new OrderVo();
            BeanUtils.copyProperties(order, orderVo);
            response.setOrder(orderVo);
            log.info("订单查询成功，订单ID: {}", request.getOrderId());
        } else {
            log.warn("订单未找到或已删除，订单ID: {}, 用户ID: {}", request.getOrderId(), request.getUserId());
        }
        
        return response;
    }

    @Override
    public GetShardingOrdersDto.Response getOrdersByUserId(GetShardingOrdersDto.Request request) {
        // 根据用户ID查询，会路由到对应的单个数据库，但可能跨表查询
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, request.getUserId())  // 分片键，路由到单个库
               .eq(Order::getDeleted, false)
               .orderByDesc(Order::getCreateTime);
        
        Page<Order> page = new Page<>(request.getPage(), request.getSize());
        Page<Order> orderPage = orderMapper.selectPage(page, wrapper);
        
        log.info("用户订单查询，用户ID: {}, 页码: {}, 每页大小: {}, 路由规则: 用户ID {} -> ds{}", 
                request.getUserId(), request.getPage(), request.getSize(),
                request.getUserId(), request.getUserId() % 2);
        
        // 转换为VO
        List<OrderVo> orderVos = orderPage.getRecords().stream()
                .map(order -> {
                    OrderVo orderVo = new OrderVo();
                    BeanUtils.copyProperties(order, orderVo);
                    return orderVo;
                })
                .collect(Collectors.toList());
        
        GetShardingOrdersDto.Response response = new GetShardingOrdersDto.Response();
        response.setOrders(orderVos);
        response.setTotal(orderPage.getTotal());
        response.setPage(request.getPage());
        response.setSize(request.getSize());
        
        log.info("用户订单查询成功，用户ID: {}, 返回 {} 条记录", request.getUserId(), orderVos.size());
        
        return response;
    }
}
