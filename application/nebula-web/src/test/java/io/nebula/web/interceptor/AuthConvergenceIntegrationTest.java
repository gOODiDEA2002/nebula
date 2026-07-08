package io.nebula.web.interceptor;

import io.nebula.core.common.result.Result;
import io.nebula.security.authentication.JwtAuthenticationFilter;
import io.nebula.security.authentication.SecurityContext;
import io.nebula.security.config.SecurityProperties;
import io.nebula.security.jwt.DefaultJwtService;
import io.nebula.security.jwt.JwtService;
import io.nebula.web.auth.AuthContext;
import io.nebula.web.autoconfigure.WebProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 认证收敛集成测试：验证 JWT 只在 Filter 层解析一次，AuthInterceptor 仅消费 SecurityContext。
 *
 * <p>显式注册 JwtAuthenticationFilter（Filter 层）和 AuthInterceptor（Interceptor 层），
 * 不依赖 WebAutoConfiguration 子配置的 @ConditionalOnMissingBean 链，以隔离测试认证收敛逻辑。</p>
 */
@SpringBootTest(
        classes = AuthConvergenceIntegrationTest.TestApp.class,
        properties = {
                "nebula.web.auth.enabled=true",
                "nebula.web.auth.ignore-paths=/public/**,/actuator/**",
                "nebula.security.enabled=true",
                "nebula.security.jwt.enabled=true",
                "nebula.security.jwt.secret=this-is-a-test-secret-key-at-least-32-chars-long"
        }
)
@AutoConfigureMockMvc
class AuthConvergenceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CountingJwtService countingJwtService;

    @Test
    void protectedEndpoint_withValidToken_jwtParsedOnlyOnce() throws Exception {
        String token = countingJwtService.generateAccessToken("42", Map.of(
                "username", "testuser",
                "roles", List.of("USER"),
                "permissions", List.of("data:read")
        ));

        countingJwtService.resetCount();

        mockMvc.perform(get("/api/protected")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value("42"))
                .andExpect(jsonPath("$.data.username").value("testuser"));

        assertThat(countingJwtService.getValidateCount())
                .as("validateAccessToken 应只调用一次（Filter 层）")
                .isEqualTo(1);
    }

    @Test
    void protectedEndpoint_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/protected"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whitelistPath_noToken_passes() throws Exception {
        mockMvc.perform(get("/public/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("public"));
    }

    @Test
    void protectedEndpoint_authContextMatchesSecurityContext() throws Exception {
        String token = countingJwtService.generateAccessToken("99", Map.of(
                "username", "admin",
                "roles", List.of("ADMIN"),
                "permissions", List.of("admin:all")
        ));

        mockMvc.perform(get("/api/context-check")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.authContextUserId").value("99"))
                .andExpect(jsonPath("$.data.securityContextUserId").value(99))
                .andExpect(jsonPath("$.data.match").value(true));
    }

    /**
     * 计数版 JwtService：委托真实实现但记录 validateAccessToken 调用次数
     */
    static class CountingJwtService extends DefaultJwtService {
        private final AtomicInteger validateCount = new AtomicInteger(0);

        CountingJwtService(SecurityProperties properties) {
            super(properties);
        }

        @Override
        public String validateAccessToken(String token) {
            validateCount.incrementAndGet();
            return super.validateAccessToken(token);
        }

        int getValidateCount() {
            return validateCount.get();
        }

        void resetCount() {
            validateCount.set(0);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration(excludeName = {
            "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
            "io.nebula.web.autoconfigure.WebAutoConfiguration"
    })
    @EnableConfigurationProperties({SecurityProperties.class, WebProperties.class})
    @Import({ProtectedController.class, PublicController.class, ContextCheckController.class})
    static class TestApp {

        @Bean
        @Primary
        CountingJwtService jwtService(SecurityProperties properties) {
            return new CountingJwtService(properties);
        }

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService, SecurityProperties properties) {
            return new JwtAuthenticationFilter(jwtService, properties.getJwt());
        }

        @Bean
        WebMvcConfigurer authInterceptorConfigurer(WebProperties webProperties, ObjectMapper objectMapper) {
            return new WebMvcConfigurer() {
                @Override
                public void addInterceptors(InterceptorRegistry registry) {
                    registry.addInterceptor(new AuthInterceptor(webProperties.getAuth(), objectMapper))
                            .addPathPatterns("/**")
                            .order(InterceptorOrders.AUTH);
                }
            };
        }
    }

    @RestController
    static class ProtectedController {
        @GetMapping("/api/protected")
        public Result<Map<String, String>> protectedEndpoint() {
            var user = AuthContext.getCurrentUser();
            return Result.success(Map.of(
                    "userId", user != null ? user.getUserId() : "null",
                    "username", user != null ? user.getUsername() : "null"
            ));
        }
    }

    @RestController
    static class PublicController {
        @GetMapping("/public/info")
        public Result<String> publicInfo() {
            return Result.success("public");
        }
    }

    @RestController
    static class ContextCheckController {
        @GetMapping("/api/context-check")
        public Result<Map<String, Object>> contextCheck() {
            var authUser = AuthContext.getCurrentUser();
            Long secUserId = SecurityContext.getCurrentUserId();

            String authUserId = authUser != null ? authUser.getUserId() : null;
            boolean match = authUserId != null && secUserId != null
                    && authUserId.equals(secUserId.toString());

            return Result.success(Map.of(
                    "authContextUserId", authUserId != null ? authUserId : "null",
                    "securityContextUserId", secUserId != null ? secUserId : "null",
                    "match", match
            ));
        }
    }
}
