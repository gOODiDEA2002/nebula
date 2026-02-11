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
 *       connection-acquisition-timeout: 60s
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

    /** 连接获取超时（秒） */
    private int connectionAcquisitionTimeoutSeconds = 60;

    /** Cypher DSL 方言: NEO4J_5 / DEFAULT */
    private String cypherDslDialect = "NEO4J_5";

    /** 是否开启健康检查 */
    private boolean healthCheckEnabled = true;
}
