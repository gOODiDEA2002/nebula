package io.nebula.example.modules.data.entity.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductVo {
  private Long id;
  private String name;
  private String category;
  private BigDecimal price;
  private Integer stockQuantity;
  private String status;
  private String description;
}
