package io.nebula.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.time.Duration;

/**
 * Gateway Redis 自动配置
 * <p>
 * 将 nebula.gateway.rate-limit.redis 配置桥接到 Spring Data Redis
 */
@Slf4j
@Configuration
@ConditionalOnClass(ReactiveRedisConnectionFactory.class)
@ConditionalOnProperty(prefix = "nebula.gateway.rate-limit.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
public class GatewayRedisAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ReactiveRedisConnectionFactory reactiveRedisConnectionFactory(GatewayProperties gatewayProperties) {
        GatewayProperties.RedisConfig redisConfig = gatewayProperties.getRateLimit().getRedis();
        
        RedisStandaloneConfiguration serverConfig = new RedisStandaloneConfiguration();
        serverConfig.setHostName(redisConfig.getHost());
        serverConfig.setPort(redisConfig.getPort());
        serverConfig.setDatabase(redisConfig.getDatabase());
        
        if (redisConfig.getPassword() != null && !redisConfig.getPassword().isEmpty()) {
            serverConfig.setPassword(redisConfig.getPassword());
        }
        
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofMillis(redisConfig.getTimeout()))
                .build();
        
        log.info("配置 Gateway Redis: {}:{}/{}", 
                redisConfig.getHost(), redisConfig.getPort(), redisConfig.getDatabase());
        
        return new LettuceConnectionFactory(serverConfig, clientConfig);
    }
}

