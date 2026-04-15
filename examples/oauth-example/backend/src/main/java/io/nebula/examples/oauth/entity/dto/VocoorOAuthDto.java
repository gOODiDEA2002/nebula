package io.nebula.examples.oauth.entity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Vocoor OAuth 相关 DTO
 */
public class VocoorOAuthDto {

    /**
     * 发起授权请求
     */
    @Data
    public static class AuthorizeRequest {
        /**
         * 权限范围
         */
        private String scope;
    }

    /**
     * 授权回调请求
     */
    @Data
    public static class CallbackRequest {
        /**
         * 授权码
         */
        private String code;

        /**
         * 状态参数
         */
        private String state;
    }

    /**
     * Token 响应（从 Vocoor 返回）
     */
    @Data
    public static class TokenResponse {
        private Boolean success;
        private TokenResult result;
        private String error;
        @JsonProperty("error_description")
        private String errorDescription;
    }

    @Data
    public static class TokenResult {
        @JsonProperty("access_token")
        private String accessToken;
        
        @JsonProperty("refresh_token")
        private String refreshToken;
        
        @JsonProperty("token_type")
        private String tokenType;
        
        @JsonProperty("expires_in")
        private Integer expiresIn;
        
        private String scope;
    }

    /**
     * 用户信息响应（从 Vocoor 返回）
     */
    @Data
    public static class UserInfoResponse {
        private Boolean success;
        private VocoorUser result;
        private String error;
        @JsonProperty("error_description")
        private String errorDescription;
    }

    @Data
    public static class VocoorUser {
        private String openid;
        private String sub;
        private String nickname;
        private String avatar;
        private String mobile;
        private String email;
        private VocoorCompany company;
    }

    @Data
    public static class VocoorCompany {
        private String id;
        private String name;
    }
}

