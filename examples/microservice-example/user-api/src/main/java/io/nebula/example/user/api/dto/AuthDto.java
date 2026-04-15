package io.nebula.example.user.api.dto;

import lombok.Data;
public class AuthDto {
  @Data
  public static class Request {
    private String username;
    private String password;
  }

  @Data
  public static class Response {
    private String token;
  }
}
