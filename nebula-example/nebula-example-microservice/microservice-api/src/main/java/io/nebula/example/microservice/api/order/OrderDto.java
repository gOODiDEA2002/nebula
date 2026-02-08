package io.nebula.example.microservice.api.order;

import io.nebula.example.microservice.api.user.UserDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单 DTO
 * 
 * @author Nebula Framework Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {
    
    /**
     * 订单 ID
     */
    private Long id;
    
    /**
     * 订单号
     */
    private String orderNo;
    
    /**
     * 用户 ID
     */
    private Long userId;
    
    /**
     * 用户信息（通过 RPC 调用用户服务获取）
     */
    private UserDto user;
    
    /**
     * 商品名称
     */
    private String productName;
    
    /**
     * 数量
     */
    private Integer quantity;
    
    /**
     * 金额
     */
    private BigDecimal amount;
    
    /**
     * 状态（0=待支付, 1=已支付, 2=已发货, 3=已完成, -1=已取消）
     */
    private Integer status;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
