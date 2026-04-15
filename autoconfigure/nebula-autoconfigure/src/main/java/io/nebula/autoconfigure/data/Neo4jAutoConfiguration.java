package io.nebula.autoconfigure.data;

import io.nebula.core.common.diagnostic.NebulaComponentSummary;
import io.nebula.core.common.diagnostic.SimpleComponentSummary;
import io.nebula.data.neo4j.config.Neo4jProperties;
import io.nebula.data.neo4j.support.CypherExecutor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.cypherdsl.core.renderer.Configuration;
import org.neo4j.cypherdsl.core.renderer.Dialect;
import org.neo4j.driver.*;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

/**
 * Neo4j 图数据库自动配置
 * <p>
 * 当 classpath 中存在 Neo4j Driver 时默认生效（无需显式配置 enabled=true）。
 * 可通过 nebula.data.neo4j.enabled=false 禁用。
 * <p>
 * 配置示例:
 * 
 * <pre>
 * nebula:
 *   data:
 *     neo4j:
 *       uri: bolt://localhost:7687
 *       username: neo4j
 *       password: secret
 * </pre>
 *
 * @author Nebula Framework
 * @since 2.0.1
 */
@Slf4j
@AutoConfiguration(before = org.springframework.boot.autoconfigure.neo4j.Neo4jAutoConfiguration.class)
@ConditionalOnClass(Driver.class)
@ConditionalOnProperty(prefix = "nebula.data.neo4j", name = "enabled", havingValue = "true", matchIfMissing = false)
@EnableConfigurationProperties(Neo4jProperties.class)
public class Neo4jAutoConfiguration {

    /**
     * 创建 Neo4j Driver
     * <p>
     * 如果 spring.neo4j.* 已配置（Spring Boot 自动配置），则不重复创建。
     * 当 nebula.data.neo4j.uri 配置时，优先使用 Nebula 配置。
     */
    @Bean
    @ConditionalOnMissingBean(Driver.class)
    public Driver neo4jDriver(Neo4jProperties properties) {
        log.info("初始化 Neo4j Driver: uri={}, database={}, poolSize={}, " +
                        "connTimeout={}s, acquireTimeout={}s, livenessCheck={}s, " +
                        "maxLifetime={}s, txRetry={}s",
                properties.getUri(), properties.getDatabase(),
                properties.getMaxConnectionPoolSize(),
                properties.getConnectionTimeoutSeconds(),
                properties.getConnectionAcquisitionTimeoutSeconds(),
                properties.getConnectionLivenessCheckTimeoutSeconds(),
                properties.getMaxConnectionLifetimeSeconds(),
                properties.getMaxTransactionRetryTimeSeconds());

        AuthToken authToken = AuthTokens.basic(
                properties.getUsername(),
                properties.getPassword());

        Config config = Config.builder()
                .withMaxConnectionPoolSize(properties.getMaxConnectionPoolSize())
                .withConnectionAcquisitionTimeout(
                        properties.getConnectionAcquisitionTimeoutSeconds(), TimeUnit.SECONDS)
                .withConnectionTimeout(
                        properties.getConnectionTimeoutSeconds(), TimeUnit.SECONDS)
                .withMaxTransactionRetryTime(
                        properties.getMaxTransactionRetryTimeSeconds(), TimeUnit.SECONDS)
                .withConnectionLivenessCheckTimeout(
                        properties.getConnectionLivenessCheckTimeoutSeconds(), TimeUnit.SECONDS)
                .withMaxConnectionLifetime(
                        properties.getMaxConnectionLifetimeSeconds(), TimeUnit.SECONDS)
                .withLogging(Logging.slf4j())
                .build();

        Driver driver = GraphDatabase.driver(properties.getUri(), authToken, config);

        // 验证连接
        try {
            driver.verifyConnectivity();
            log.info("Neo4j 连接成功: {}", properties.getUri());
        } catch (Exception e) {
            log.warn("Neo4j 连接验证失败（服务启动后重试）: {}", e.getMessage());
        }

        return driver;
    }

    /**
     * Cypher DSL 方言配置
     */
    @Bean
    @ConditionalOnMissingBean(Configuration.class)
    @ConditionalOnClass(Configuration.class)
    public Configuration cypherDslConfiguration(Neo4jProperties properties) {
        Dialect dialect;
        try {
            dialect = Dialect.valueOf(properties.getCypherDslDialect().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("未知的 Cypher DSL 方言: {}，使用默认 NEO4J_5", properties.getCypherDslDialect());
            dialect = Dialect.NEO4J_5;
        }

        return Configuration.newConfig()
                .withDialect(dialect)
                .build();
    }

    /**
     * Cypher 执行工具
     */
    @Bean
    @ConditionalOnMissingBean(CypherExecutor.class)
    public CypherExecutor cypherExecutor(Driver driver) {
        log.info("初始化 CypherExecutor");
        return new CypherExecutor(driver);
    }

    /**
     * 组件摘要: Neo4j
     */
    @Bean
    NebulaComponentSummary neo4jSummary(Neo4jProperties properties) {
        var details = new java.util.LinkedHashMap<String, String>();
        details.put("URI", properties.getUri());
        details.put("Database", properties.getDatabase());
        return new SimpleComponentSummary("Data", "Neo4j", true, 320, details);
    }
}
