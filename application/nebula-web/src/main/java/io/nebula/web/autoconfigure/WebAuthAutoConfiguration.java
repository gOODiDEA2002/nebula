package io.nebula.web.autoconfigure;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import io.nebula.web.auth.AuthService;
import io.nebula.web.auth.DefaultAuthService;
import io.nebula.web.auth.JwtUtils;
import io.nebula.web.interceptor.AuthInterceptor;
import io.nebula.web.interceptor.InterceptorOrders;
import io.nebula.web.mask.DataMaskingStrategyManager;
import io.nebula.web.mask.SensitiveDataAnnotationIntrospector;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jackson2.autoconfigure.Jackson2ObjectMapperBuilderCustomizer;
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
     * 将脱敏 introspector 挂到 Spring MVC 使用的主 ObjectMapper。
     * 此前脱敏只挂在独立的 dataMaskingObjectMapper 上, 控制器正常返回时走主 ObjectMapper 并不脱敏,
     * 导致 @SensitiveData 事实上不生效。用 AnnotationIntrospectorPair 与默认 introspector 链接
     * (脱敏优先、其余回退默认), 避免像旧独立 mapper 那样 setAnnotationIntrospector 覆盖掉默认注解处理。
     */
    @Bean
    @ConditionalOnProperty(name = "nebula.web.data-masking.enabled", havingValue = "true")
    public Jackson2ObjectMapperBuilderCustomizer sensitiveDataMaskingCustomizer(
            SensitiveDataAnnotationIntrospector introspector) {
        return builder -> builder.postConfigurer(objectMapper -> {
            AnnotationIntrospector primary = objectMapper.getSerializationConfig().getAnnotationIntrospector();
            objectMapper.setAnnotationIntrospector(AnnotationIntrospectorPair.pair(introspector, primary));
        });
    }
}
