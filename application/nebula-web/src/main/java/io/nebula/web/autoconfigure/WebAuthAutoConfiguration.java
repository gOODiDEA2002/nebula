package io.nebula.web.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nebula.web.auth.AuthService;
import io.nebula.web.auth.DefaultAuthService;
import io.nebula.web.auth.JwtUtils;
import io.nebula.web.interceptor.AuthInterceptor;
import io.nebula.web.mask.DataMaskingStrategyManager;
import io.nebula.web.mask.SensitiveDataAnnotationIntrospector;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 认证与数据脱敏自动配置
 */
@Configuration(proxyBeanMethods = false)
class WebAuthAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "nebula.web.auth.enabled", havingValue = "true")
    public JwtUtils jwtUtils(WebProperties webProperties, ObjectMapper objectMapper) {
        WebProperties.Auth config = webProperties.getAuth();
        return new JwtUtils(config.getJwtSecret(), config.getJwtExpiration(), objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "nebula.web.auth.enabled", havingValue = "true")
    public AuthService authService(JwtUtils jwtUtils) {
        return new DefaultAuthService(jwtUtils);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "nebula.web.auth.enabled", havingValue = "true")
    public WebMvcConfigurer authWebMvcConfigurer(AuthService authService,
                                                WebProperties webProperties,
                                                ObjectMapper objectMapper) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                WebProperties.Auth config = webProperties.getAuth();
                registry.addInterceptor(new AuthInterceptor(authService, config, objectMapper))
                       .addPathPatterns("/**")
                       .order(-1);
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "nebula.web.data-masking.enabled", havingValue = "true")
    public DataMaskingStrategyManager dataMaskingStrategyManager() {
        return new DataMaskingStrategyManager();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "nebula.web.data-masking.enabled", havingValue = "true")
    public SensitiveDataAnnotationIntrospector sensitiveDataAnnotationIntrospector(
            DataMaskingStrategyManager strategyManager) {
        return new SensitiveDataAnnotationIntrospector(strategyManager);
    }

    @Bean("dataMaskingObjectMapper")
    @ConditionalOnProperty(name = "nebula.web.data-masking.enabled", havingValue = "true")
    public ObjectMapper dataMaskingObjectMapper(SensitiveDataAnnotationIntrospector introspector) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setAnnotationIntrospector(introspector);
        return mapper;
    }
}
