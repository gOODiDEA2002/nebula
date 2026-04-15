package io.nebula.example.modules.web.entity.dto;

import lombok.Data;
import java.time.LocalDateTime;
import io.nebula.example.modules.web.entity.dos.UserInfo;
@Data
public class LoginDto {

  @Data
  public static class Request {
    private String username;
    private String password;
  }

  @Data
  public static class Response {
    private String token;
    private UserInfo user;
    private LocalDateTime loginTime;
  }
  
}
