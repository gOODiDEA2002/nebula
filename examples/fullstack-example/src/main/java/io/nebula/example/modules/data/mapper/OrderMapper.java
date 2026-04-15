package io.nebula.example.modules.data.mapper;

import io.nebula.data.persistence.mapper.BaseMapper;
import io.nebula.example.modules.data.entity.dos.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 订单Mapper接口 - 用于分片演示
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {
    
    /**
     * 根据用户ID统计订单信息
     * 这个查询会路由到对应用户的分库
     */
    @Select("SELECT " +
            "  COUNT(*) as total_orders, " +
            "  SUM(total_amount) as total_amount, " +
            "  AVG(total_amount) as avg_amount " +
            "FROM t_order " +
            "WHERE user_id = #{userId} AND deleted = 0")
    Map<String, Object> getOrderStatsByUserId(@Param("userId") Long userId);
    
    /**
     * 根据时间范围查询订单统计
     * 这个查询可能会跨多个分表
     */
    @Select("SELECT " +
            "  status, " +
            "  COUNT(*) as count, " +
            "  SUM(total_amount) as total_amount " +
            "FROM t_order " +
            "WHERE create_time >= #{startTime} " +
            "  AND create_time <= #{endTime} " +
            "  AND deleted = 0 " +
            "GROUP BY status")
    List<Map<String, Object>> getOrderStatsByTimeRange(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查询用户在指定时间段内的订单
     * 这个查询同时涉及分库（用户ID）和分表（时间）
     */
    @Select("SELECT * FROM t_order " +
            "WHERE user_id = #{userId} " +
            "  AND create_time >= #{startTime} " +
            "  AND create_time <= #{endTime} " +
            "  AND deleted = 0 " +
            "ORDER BY create_time DESC " +
            "LIMIT #{limit}")
    List<Order> getUserOrdersByTimeRange(
            @Param("userId") Long userId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("limit") Integer limit);
    
    /**
     * 查询指定产品的销售统计
     * 这个查询可能会跨所有分库分表
     */
    @Select("SELECT " +
            "  COUNT(*) as order_count, " +
            "  SUM(quantity) as total_quantity, " +
            "  SUM(total_amount) as total_sales " +
            "FROM t_order " +
            "WHERE product_id = #{productId} " +
            "  AND status IN ('PAID', 'SHIPPED', 'COMPLETED') " +
            "  AND deleted = 0")
    Map<String, Object> getProductSalesStats(@Param("productId") Long productId);
    
    /**
     * 查询热门产品排行
     * 跨分片聚合查询
     */
    @Select("SELECT " +
            "  product_id, " +
            "  product_name, " +
            "  COUNT(*) as order_count, " +
            "  SUM(quantity) as total_quantity " +
            "FROM t_order " +
            "WHERE status IN ('PAID', 'SHIPPED', 'COMPLETED') " +
            "  AND create_time >= #{startTime} " +
            "  AND deleted = 0 " +
            "GROUP BY product_id, product_name " +
            "ORDER BY total_quantity DESC " +
            "LIMIT #{limit}")
    List<Map<String, Object>> getHotProducts(
            @Param("startTime") LocalDateTime startTime,
            @Param("limit") Integer limit);
}
