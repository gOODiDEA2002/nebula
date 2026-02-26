package io.nebula.data.neo4j.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nebula Neo4j 配置属性
 * <p>
 * 配置前缀: nebula.data.neo4j
 * <p>
 * 示例配置:
 * <pre>
 * nebula:
 *   data:
 *     neo4j:
 *       enabled: true
 *       uri: bolt://localhost:7687
 *       username: neo4j
 *       password: secret
 *       database: neo4j
 *       max-connection-pool-size: 100
 *       connection-acquisition-timeout-seconds: 60
 *       connection-timeout-seconds: 30
 *       max-transaction-retry-time-seconds: 30
 *       connection-liveness-check-timeout-seconds: 60
 *       max-connection-lifetime-seconds: 3600
 *       cypher-dsl-dialect: NEO4J_5
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "nebula.data.neo4j")
public class Neo4jProperties {

    /** 是否启用 Neo4j */
    private boolean enabled = true;

    /** Neo4j URI (bolt://host:port) */
    private String uri;

    /** 用户名 */
    private String username;

    /** 密码 */
    private String password;

    /** 数据库名称（CE版固定neo4j） */
    private String database = "neo4j";

    /** 最大连接池大小 */
    private int maxConnectionPoolSize = 100;

    /** 连接获取超时（秒），从连接池获取连接的最大等待时间 */
    private int connectionAcquisitionTimeoutSeconds = 60;

    /** 建立 TCP 连接超时（秒），防止连接建立阶段无限等待 */
    private int connectionTimeoutSeconds = 30;

    /** 事务重试最大时间（秒），限制事务因瞬时错误反复重试的总时长 */
    private int maxTransactionRetryTimeSeconds = 30;

    /**
     * 连接活性检查超时（秒）。
     * 从连接池取出连接时，若空闲超过此时间则先做活性检测，避免使用已死连接。
     * 设为 -1 表示不检查（不推荐）。
     */
    private int connectionLivenessCheckTimeoutSeconds = 60;

    /** 连接最大生命周期（秒），超过此时间的连接将被关闭并重建 */
    private int maxConnectionLifetimeSeconds = 3600;

    /** Cypher DSL 方言: NEO4J_5 / DEFAULT */
    private String cypherDslDialect = "NEO4J_5";

    /** 是否开启健康检查 */
    private boolean healthCheckEnabled = true;
}
