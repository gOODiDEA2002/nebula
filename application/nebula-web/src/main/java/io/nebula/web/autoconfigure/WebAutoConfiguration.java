package io.nebula.web.autoconfigure;

import io.nebula.web.exception.GlobalExceptionHandler;
import io.nebula.web.filter.RequestLoggingFilter;
import io.nebula.web.filter.ResponseCacheFilter;
import io.nebula.web.interceptor.RequestLoggingInterceptor;
import io.nebula.web.interceptor.RateLimitInterceptor;
import io.nebula.web.interceptor.ResponseCacheInterceptor;
import io.nebula.web.interceptor.AuthInterceptor;
import io.nebula.web.ratelimit.DefaultRateLimitKeyGenerator;
import io.nebula.web.ratelimit.MemoryRateLimiter;
import io.nebula.web.ratelimit.RateLimiter;
import io.nebula.web.ratelimit.RateLimitKeyGenerator;
import io.nebula.web.cache.ResponseCache;
import io.nebula.web.cache.MemoryResponseCache;
import io.nebula.web.cache.CacheKeyGenerator;
import io.nebula.web.cache.DefaultCacheKeyGenerator;
import io.nebula.web.auth.AuthService;
import io.nebula.web.auth.DefaultAuthService;
import io.nebula.web.auth.JwtUtils;
import io.nebula.web.mask.DataMaskingStrategyManager;
import io.nebula.web.mask.SensitiveDataAnnotationIntrospector;
import io.nebula.web.performance.PerformanceMonitor;
import io.nebula.web.performance.DefaultPerformanceMonitor;
import io.nebula.web.interceptor.PerformanceMonitorInterceptor;
import io.nebula.web.health.HealthCheckService;
import io.nebula.web.health.HealthChecker;
import io.nebula.web.health.checkers.ApplicationHealthChecker;
import io.nebula.web.health.checkers.MemoryHealthChecker;
import io.nebula.web.health.checkers.DiskSpaceHealthChecker;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import org.springframework.context.ApplicationContext;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

/**
 * Nebula Web 自动配置
 * 自动配置 Web 相关的组件
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(DispatcherServlet.class)
@EnableConfigurationProperties(WebProperties.class)
public class WebAutoConfiguration {
    
    /**
     * 全局异常处理器
     * 提供统一的异常处理机制
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "nebula.web.exception-handler.enabled", havingValue = "true", matchIfMissing = true)
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
    
    /**
     * OpenAPI 文档配置
     */
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
    
    /**
     * CORS 配置
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "nebula.web.cors.enabled", havingValue = "true")
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
    
    /**
     * Web MVC 配置
     */
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
                
                // 限流拦截器将由单独的配置方法处理
            }
        };
    }
    
    /**
     * 请求日志过滤器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "nebula.web.request-logging.enabled", havingValue = "true")
    public FilterRegistrationBean<RequestLoggingFilter> requestLoggingFilter(WebProperties webProperties) {
        FilterRegistrationBean<RequestLoggingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RequestLoggingFilter(webProperties.getRequestLogging()));
        registration.addUrlPatterns("/*");
        registration.setName("requestLoggingFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return registration;
    }
    
    /**
     * 限流器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "nebula.web.rate-limit.enabled", havingValue = "true")
    public RateLimiter rateLimiter(WebProperties webProperties) {
        WebProperties.RateLimit config = webProperties.getRateLimit();
        return new MemoryRateLimiter(
            config.getDefaultRequestsPerSecond(),
            config.getTimeWindow() * 1000L);
    }
    
    /**
     * 限流键生成器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "nebula.web.rate-limit.enabled", havingValue = "true")
    public RateLimitKeyGenerator rateLimitKeyGenerator(WebProperties webProperties) {
        WebProperties.RateLimit config = webProperties.getRateLimit();
        return new DefaultRateLimitKeyGenerator(config.getKeyStrategy());
    }
    
    /**
     * 限流拦截器配置
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "nebula.web.rate-limit.enabled", havingValue = "true")
    public WebMvcConfigurer rateLimitWebMvcConfigurer(RateLimiter rateLimiter, 
                                                     RateLimitKeyGenerator keyGenerator,
                                                     WebProperties webProperties,
                                                     ObjectMapper objectMapper) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                WebProperties.RateLimit config = webProperties.getRateLimit();
                registry.addInterceptor(new RateLimitInterceptor(rateLimiter, keyGenerator, config, objectMapper))
                       .addPathPatterns("/**");
            }
        };
    }
    
    /**
     * 响应缓存
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "nebula.web.cache.enabled", havingValue = "true")
    public ResponseCache responseCache(WebProperties webProperties) {
        WebProperties.Cache config = webProperties.getCache();
        return new MemoryResponseCache(config.getMaxSize());
    }
    
    /**
     * 缓存键生成器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "nebula.web.cache.enabled", havingValue = "true")
    public CacheKeyGenerator cacheKeyGenerator(WebProperties webProperties) {
        WebProperties.Cache config = webProperties.getCache();
        return new DefaultCacheKeyGenerator(config.getKeyPrefix());
    }
    
    /**
     * 响应缓存过滤器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "nebula.web.cache.enabled", havingValue = "true")
    public FilterRegistrationBean<ResponseCacheFilter> responseCacheFilter(WebProperties webProperties) {
        FilterRegistrationBean<ResponseCacheFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new ResponseCacheFilter(webProperties.getCache()));
        registration.addUrlPatterns("/*");
        registration.setName("responseCacheFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 2);
        return registration;
    }
    
    /**
     * 响应缓存拦截器配置
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "nebula.web.cache.enabled", havingValue = "true")
    public WebMvcConfigurer responseCacheWebMvcConfigurer(ResponseCache responseCache,
                                                         CacheKeyGenerator cacheKeyGenerator,
                                                         WebProperties webProperties) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                WebProperties.Cache config = webProperties.getCache();
                registry.addInterceptor(new ResponseCacheInterceptor(responseCache, cacheKeyGenerator, config))
                       .addPathPatterns("/**");
            }
        };
    }
    
    /**
     * JWT 工具类
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "nebula.web.auth.enabled", havingValue = "true")
    public JwtUtils jwtUtils(WebProperties webProperties, ObjectMapper objectMapper) {
        WebProperties.Auth config = webProperties.getAuth();
        return new JwtUtils(config.getJwtSecret(), config.getJwtExpiration(), objectMapper);
    }
    
    /**
     * 认证服务
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "nebula.web.auth.enabled", havingValue = "true")
    public AuthService authService(JwtUtils jwtUtils) {
        return new DefaultAuthService(jwtUtils);
    }
    
    /**
     * 认证拦截器配置
     */
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
                       .order(-1); // 认证拦截器优先级最高
            }
        };
    }
    
    /**
     * 数据脱敏策略管理器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "nebula.web.data-masking.enabled", havingValue = "true")
    public DataMaskingStrategyManager dataMaskingStrategyManager() {
        return new DataMaskingStrategyManager();
    }
    
    /**
     * 敏感数据注解内省器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "nebula.web.data-masking.enabled", havingValue = "true")
    public SensitiveDataAnnotationIntrospector sensitiveDataAnnotationIntrospector(
            DataMaskingStrategyManager strategyManager) {
        return new SensitiveDataAnnotationIntrospector(strategyManager);
    }
    
    /**
     * 支持数据脱敏的 ObjectMapper
     */
    @Bean("dataMaskingObjectMapper")
    @ConditionalOnProperty(name = "nebula.web.data-masking.enabled", havingValue = "true")
    public ObjectMapper dataMaskingObjectMapper(SensitiveDataAnnotationIntrospector introspector) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setAnnotationIntrospector(introspector);
        return mapper;
    }
    
    /**
     * 性能监控器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "nebula.web.performance.enabled", havingValue = "true")
    public PerformanceMonitor performanceMonitor(WebProperties webProperties) {
        WebProperties.Performance config = webProperties.getPerformance();
        return new DefaultPerformanceMonitor(config.getSlowRequestThreshold());
    }
    
    /**
     * 性能监控拦截器配置
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "nebula.web.performance.enabled", havingValue = "true")
    public WebMvcConfigurer performanceWebMvcConfigurer(PerformanceMonitor performanceMonitor,
                                                       WebProperties webProperties) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                WebProperties.Performance config = webProperties.getPerformance();
                String[] ignorePaths = {"/performance/**", "/actuator/**", "/swagger-ui/**", "/v3/api-docs/**"};
                registry.addInterceptor(new PerformanceMonitorInterceptor(performanceMonitor, ignorePaths))
                       .addPathPatterns("/**")
                       .order(0); // 性能监控拦截器优先级较高
            }
        };
    }
    
    /**
     * 应用程序健康检查器
     */
    @Bean
    @ConditionalOnMissingBean(name = "applicationHealthChecker")
    @ConditionalOnProperty(name = "nebula.web.health.enabled", havingValue = "true", matchIfMissing = true)
    public HealthChecker applicationHealthChecker(ApplicationContext applicationContext) {
        return new ApplicationHealthChecker(applicationContext);
    }
    
    /**
     * 内存健康检查器
     */
    @Bean
    @ConditionalOnMissingBean(name = "memoryHealthChecker")
    @ConditionalOnProperty(name = "nebula.web.health.enabled", havingValue = "true", matchIfMissing = true)
    public HealthChecker memoryHealthChecker() {
        return new MemoryHealthChecker();
    }
    
    /**
     * 磁盘空间健康检查器
     */
    @Bean
    @ConditionalOnMissingBean(name = "diskSpaceHealthChecker")
    @ConditionalOnProperty(name = "nebula.web.health.enabled", havingValue = "true", matchIfMissing = true)
    public HealthChecker diskSpaceHealthChecker() {
        return new DiskSpaceHealthChecker();
    }
    
    /**
     * 健康检查服务
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "nebula.web.health.enabled", havingValue = "true", matchIfMissing = true)
    public HealthCheckService healthCheckService(List<HealthChecker> healthCheckers, WebProperties webProperties) {
        WebProperties.Health config = webProperties.getHealth();
        return new HealthCheckService(healthCheckers, config.isShowDetails());
    }
}
