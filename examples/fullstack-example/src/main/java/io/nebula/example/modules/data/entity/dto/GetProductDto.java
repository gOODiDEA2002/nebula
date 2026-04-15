package io.nebula.example.modules.data.entity.dto;

import lombok.Data;
import io.nebula.example.modules.data.entity.vo.ProductVo;

public class GetProductDto {
  @Data
  public static class Request {
    private Long id;
  }

  @Data
  public static class Response {
    private ProductVo product;
  }
}
