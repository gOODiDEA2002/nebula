package io.nebula.example.modules.web.entity.dto;

import lombok.Data;
import java.time.LocalDateTime;
@Data
public class TokenDto {

  @Data
  public static class Request {
    private String token;
  }

  @Data
  public static class Response {
    private String token;
    private LocalDateTime refreshTime;
  }
}
