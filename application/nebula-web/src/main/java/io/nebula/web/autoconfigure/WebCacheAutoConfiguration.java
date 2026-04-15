package io.nebula.web.autoconfigure;

import io.nebula.web.cache.CacheKeyGenerator;
import io.nebula.web.cache.DefaultCacheKeyGenerator;
import io.nebula.web.cache.MemoryResponseCache;
import io.nebula.web.cache.ResponseCache;
import io.nebula.web.filter.ResponseCacheFilter;
import io.nebula.web.interceptor.ResponseCacheInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 响应缓存自动配置
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "nebula.web.cache.enabled", havingValue = "true", matchIfMissing = true)
class WebCacheAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ResponseCache responseCache(WebProperties webProperties) {
        WebProperties.Cache config = webProperties.getCache();
        return new MemoryResponseCache(config.getMaxSize());
    }

    @Bean
    @ConditionalOnMissingBean
    public CacheKeyGenerator cacheKeyGenerator(WebProperties webProperties) {
        WebProperties.Cache config = webProperties.getCache();
        return new DefaultCacheKeyGenerator(config.getKeyPrefix());
    }

    @Bean
    @ConditionalOnMissingBean
    public FilterRegistrationBean<ResponseCacheFilter> responseCacheFilter(WebProperties webProperties) {
        FilterRegistrationBean<ResponseCacheFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new ResponseCacheFilter(webProperties.getCache()));
        registration.addUrlPatterns("/*");
        registration.setName("responseCacheFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 2);
        return registration;
    }

    @Bean
    @ConditionalOnMissingBean
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
}
