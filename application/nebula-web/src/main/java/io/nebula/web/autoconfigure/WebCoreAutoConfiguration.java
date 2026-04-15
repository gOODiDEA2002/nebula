package io.nebula.web.autoconfigure;

import io.nebula.web.exception.GlobalExceptionHandler;
import io.nebula.web.filter.RequestLoggingFilter;
import io.nebula.web.interceptor.RequestLoggingInterceptor;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

/**
 * Web 核心自动配置：全局异常处理、OpenAPI、CORS、请求日志
 */
@Configuration(proxyBeanMethods = false)
class WebCoreAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "nebula.web.exception-handler.enabled", havingValue = "true", matchIfMissing = true)
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "io.swagger.v3.oas.models.OpenAPI")
    @ConditionalOnProperty(name = "nebula.web.api-doc.enabled", havingValue = "true", matchIfMissing = true)
    public OpenAPI openAPI(WebProperties webProperties) {
        WebProperties.ApiDoc apiDoc = webProperties.getApiDoc();

        Contact contact = new Contact()
                .name(apiDoc.getContactName())
                .email(apiDoc.getContactEmail());

        Info info = new Info()
                .title(apiDoc.getTitle())
                .description(apiDoc.getDescription())
                .version(apiDoc.getVersion())
                .contact(contact);

        return new OpenAPI().info(info);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "nebula.web.cors.enabled", havingValue = "true", matchIfMissing = true)
    public CorsConfigurationSource corsConfigurationSource(WebProperties webProperties) {
        WebProperties.Cors corsConfig = webProperties.getCors();

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList(corsConfig.getAllowedOrigins()));
        configuration.setAllowedMethods(Arrays.asList(corsConfig.getAllowedMethods()));
        configuration.setAllowedHeaders(Arrays.asList(corsConfig.getAllowedHeaders()));
        configuration.setAllowCredentials(corsConfig.isAllowCredentials());
        configuration.setMaxAge(corsConfig.getMaxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    @ConditionalOnMissingBean
    public WebMvcConfigurer nebulaWebMvcConfigurer(WebProperties webProperties) {
        return new WebMvcConfigurer() {

            @Override
            public void addCorsMappings(CorsRegistry registry) {
                WebProperties.Cors corsConfig = webProperties.getCors();
                if (corsConfig.isEnabled()) {
                    registry.addMapping("/**")
                            .allowedOriginPatterns(corsConfig.getAllowedOrigins())
                            .allowedMethods(corsConfig.getAllowedMethods())
                            .allowedHeaders(corsConfig.getAllowedHeaders())
                            .allowCredentials(corsConfig.isAllowCredentials())
                            .maxAge(corsConfig.getMaxAge());
                }
            }

            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                WebProperties.RequestLogging requestLoggingConfig = webProperties.getRequestLogging();
                if (requestLoggingConfig.isEnabled()) {
                    registry.addInterceptor(new RequestLoggingInterceptor(requestLoggingConfig))
                           .addPathPatterns("/**");
                }
            }

            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                registry.addResourceHandler("/favicon.ico")
                        .addResourceLocations("classpath:/static/", "classpath:/public/")
                        .setCachePeriod(0)
                        .resourceChain(false);
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "nebula.web.request-logging.enabled", havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean<RequestLoggingFilter> requestLoggingFilter(WebProperties webProperties) {
        FilterRegistrationBean<RequestLoggingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RequestLoggingFilter(webProperties.getRequestLogging()));
        registration.addUrlPatterns("/*");
        registration.setName("requestLoggingFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return registration;
    }
}
