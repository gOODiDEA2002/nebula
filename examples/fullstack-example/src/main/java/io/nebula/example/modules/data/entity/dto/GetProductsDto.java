package io.nebula.example.modules.data.entity.dto;

import lombok.Data;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import io.nebula.example.modules.data.entity.vo.ProductVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import java.math.BigDecimal;

public class GetProductsDto {
  @Data
  public static class Request {
    /**
     * 产品分类
     */
    private String category;

    /**
     * 产品状态
     */
    private String status;

    /**
     * 最小价格
     */
    private BigDecimal minPrice;

    /**
     * 最大价格
     */
    private BigDecimal maxPrice;

    /**
     * 名称关键字
     */
    private String keyword;

    /**
     * 分页页码
     */
    @Min(value = 1, message = "分页页码不能为负数")
    private Integer page;

    /**
     * 分页大小
     */
    @Min(value = 1, message = "分页大小不能为负数")
    @Max(value = 100, message = "分页大小不能超过100")
    private Integer size;
  }

  @Data
  public static class Response {
    private IPage<ProductVo> products;
  }
}
