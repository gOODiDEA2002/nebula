package io.nebula.examples.oauth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * OAuth 客户端配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "vocoor.oauth")
public class OAuthClientConfig {

    /**
     * Vocoor OAuth 服务器地址
     */
    private String serverUrl = "http://localhost:8080";

    /**
     * 客户端ID
     */
    private String clientId;

    /**
     * 客户端密钥
     */
    private String clientSecret;

    /**
     * 回调地址
     */
    private String redirectUri;

    /**
     * 默认请求的权限范围
     */
    private String defaultScope = "profile phone company";

    /**
     * 前端应用地址
     */
    private String frontendUrl = "http://localhost:5173";

    /**
     * 创建 WebClient 用于调用 Vocoor OAuth API
     */
    @Bean
    public WebClient vocoorWebClient() {
        return WebClient.builder()
                .baseUrl(serverUrl)
                .build();
    }
}


