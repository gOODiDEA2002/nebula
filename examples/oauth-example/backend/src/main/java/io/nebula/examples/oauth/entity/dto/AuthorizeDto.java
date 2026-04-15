package io.nebula.examples.oauth.entity.dto;

import lombok.Data;

import io.swagger.v3.oas.annotations.media.Schema;

public class AuthorizeDto {
      /**
     * 登录请求
     */
      @Data
      @Schema(description = "OAuth登录请求")
      public static class Request {
        /**
         * 权限范围
         */
        private String scope;
      }

      @Data
      public static class Response {
        /**
         * 授权URL
         */
        private String authUrl;
      }
}
