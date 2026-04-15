package io.nebula.example.modules.data.entity.dto;

import io.nebula.example.modules.data.entity.vo.ProductVo;
import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

/**
 * 更新产品（读写分离演示）接口DTO
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
public class UpdateReadWriteProductDto {

    /**
     * 更新产品（读写分离演示）请求
     */
    @Data
    public static class Request {
        /**
         * 产品ID
         */
        @NotNull(message = "产品ID不能为空")
        private Long id;
        
        /**
         * 产品名称
         */
        @Size(max = 100, message = "产品名称长度不能超过100个字符")
        private String name;
        
        /**
         * 产品描述
         */
        @Size(max = 500, message = "产品描述长度不能超过500个字符")
        private String description;
        
        /**
         * 产品价格
         */
        @DecimalMin(value = "0.01", message = "产品价格必须大于0")
        @DecimalMax(value = "999999.99", message = "产品价格不能超过999999.99")
        private BigDecimal price;
        
        /**
         * 产品分类
         */
        private String category;
        
        /**
         * 库存数量
         */
        @Min(value = 0, message = "库存数量不能为负数")
        @Max(value = 999999, message = "库存数量不能超过999999")
        private Integer stock;
    }

    /**
     * 更新产品（读写分离演示）响应
     */
    @Data
    public static class Response {
        /**
         * 更新后的产品信息
         */
        private ProductVo product;
    }
}
