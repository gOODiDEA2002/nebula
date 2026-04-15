package io.nebula.example.modules.data.entity.dto;

import lombok.Data;

import java.math.BigDecimal;
import io.nebula.example.modules.data.entity.vo.ProductVo;

public class UpdateProductDto {
  @Data
  public static class Request {
    private Long id;
    private String name;
    private String category;
    private BigDecimal price;
    private Integer stock;
    private String description;
  }

  @Data
  public static class Response {
    private ProductVo product;
  }
}
