package io.nebula.web.autoconfigure;

import tools.jackson.databind.ObjectMapper;
import io.nebula.web.auth.AuthService;
import io.nebula.web.auth.DefaultAuthService;
import io.nebula.web.auth.JwtUtils;
import io.nebula.web.interceptor.AuthInterceptor;
import io.nebula.web.interceptor.InterceptorOrders;
import io.nebula.web.mask.DataMaskingStrategyManager;
import io.nebula.web.mask.SensitiveDataAnnotationIntrospector;
import io.nebula.security.jwt.JwtService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import tools.jackson.databind.AnnotationIntrospector;
import tools.jackson.databind.introspect.AnnotationIntrospectorPair;

/**
 * Web 认证与数据脱敏自动配置
 */
@Configuration(proxyBeanMethods = false)
class WebAuthAutoConfiguration {

    @Bean
    @ConditionalOnProperty(name = "nebula.web.auth.enabled", havingValue = "true")
    public JwtUtils jwtUtils(WebProperties webProperties, ObjectMapper objectMapper) {
        WebProperties.Auth config = webProperties.getAuth();
        return new JwtUtils(config.getJwtSecret(), config.getJwtExpiration(), objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "nebula.web.auth.enabled", havingValue = "true")
    public AuthService authService(JwtService jwtService) {
        return new DefaultAuthService(jwtService);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "nebula.web.auth.enabled", havingValue = "true")
    public WebMvcConfigurer authWebMvcConfigurer(WebProperties webProperties,
                                                ObjectMapper objectMapper) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                WebProperties.Auth config = webProperties.getAuth();
                registry.addInterceptor(new AuthInterceptor(config, objectMapper))
                       .addPathPatterns("/**")
                       .order(InterceptorOrders.AUTH);
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

    /**
     * 将脱敏 introspector 挂到 Spring MVC 使用的主 JsonMapper（Jackson 3 路径）。
     * 使用 AnnotationIntrospectorPair.create() 与默认 introspector 链接
     * (脱敏优先、其余回退默认), 避免覆盖掉默认注解处理。
     */
    @Bean
    @ConditionalOnProperty(name = "nebula.web.data-masking.enabled", havingValue = "true")
    public JsonMapperBuilderCustomizer sensitiveDataMaskingCustomizer(
            SensitiveDataAnnotationIntrospector introspector) {
        return builder -> {
            AnnotationIntrospector current = builder.annotationIntrospector();
            builder.annotationIntrospector(
                    AnnotationIntrospectorPair.create(introspector, current));
        };
    }
}
