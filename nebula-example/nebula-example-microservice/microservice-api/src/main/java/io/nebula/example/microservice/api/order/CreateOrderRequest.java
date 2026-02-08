package io.nebula.example.microservice.api.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 创建订单请求
 * 
 * @author Nebula Framework Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {
    
    /**
     * 用户 ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    
    /**
     * 商品名称
     */
    @NotBlank(message = "商品名称不能为空")
    private String productName;
    
    /**
     * 数量
     */
    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量必须大于0")
    private Integer quantity;
    
    /**
     * 金额
     */
    @NotNull(message = "金额不能为空")
    private BigDecimal amount;
}
