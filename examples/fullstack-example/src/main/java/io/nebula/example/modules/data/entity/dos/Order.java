package io.nebula.example.modules.data.entity.dos;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体类 - 用于分片演示
 * 逻辑表名为 t_order，实际表名为 t_order_0, t_order_1
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Data
@TableName("t_order")
public class Order {
    
    /**
     * 订单ID（分表键，雪花算法生成）
     * 在分片配置中作为分表键使用
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    /**
     * 订单号
     */
    private String orderNo;
    
    /**
     * 用户ID（分库键）
     * 在分片配置中作为分库键使用
     */
    private Long userId;
    
    /**
     * 产品ID
     */
    private Long productId;
    
    /**
     * 产品名称（冗余字段）
     */
    private String productName;
    
    /**
     * 购买数量
     */
    private Integer quantity;
    
    /**
     * 单价
     */
    private BigDecimal unitPrice;
    
    /**
     * 订单金额（分片演示中使用的字段）
     */
    private BigDecimal amount;
    
    /**
     * 总金额
     */
    private BigDecimal totalAmount;
    
    /**
     * 订单状态：PENDING, PAID, SHIPPED, COMPLETED, CANCELLED
     */
    private String status;
    
    /**
     * 支付方式
     */
    private String paymentMethod;
    
    /**
     * 收货地址
     */
    private String shippingAddress;
    
    /**
     * 备注
     */
    private String remark;
    
    /**
     * 逻辑删除标记
     */
    private Boolean deleted;
    
    /**
     * 创建时间（用于分表）
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
