package io.nebula.web.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nebula.web.interceptor.RateLimitInterceptor;
import io.nebula.web.ratelimit.DefaultRateLimitKeyGenerator;
import io.nebula.web.ratelimit.MemoryRateLimiter;
import io.nebula.web.ratelimit.RateLimiter;
import io.nebula.web.ratelimit.RateLimitKeyGenerator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 限流自动配置
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "nebula.web.rate-limit.enabled", havingValue = "true")
class WebRateLimitAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RateLimiter rateLimiter(WebProperties webProperties) {
        WebProperties.RateLimit config = webProperties.getRateLimit();
        return new MemoryRateLimiter(
            config.getDefaultRequestsPerSecond(),
            config.getTimeWindow() * 1000L);
    }

    @Bean
    @ConditionalOnMissingBean
    public RateLimitKeyGenerator rateLimitKeyGenerator(WebProperties webProperties) {
        WebProperties.RateLimit config = webProperties.getRateLimit();
        return new DefaultRateLimitKeyGenerator(config.getKeyStrategy());
    }

    @Bean
    @ConditionalOnMissingBean
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
}
