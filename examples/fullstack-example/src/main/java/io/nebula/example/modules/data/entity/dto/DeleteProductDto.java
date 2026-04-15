package io.nebula.example.modules.data.entity.dto;

import lombok.Data;

import java.util.List;

public class DeleteProductDto {
  @Data
  public static class Request {
    private List<Long> ids;
  }

  @Data
  public static class Response {
    private Integer deletedCount;
  }
}
