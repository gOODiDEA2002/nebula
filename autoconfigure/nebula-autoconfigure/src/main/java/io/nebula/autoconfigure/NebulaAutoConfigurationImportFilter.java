package io.nebula.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfigurationImportFilter;
import org.springframework.boot.autoconfigure.AutoConfigurationMetadata;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Nebula 自动配置导入过滤器
 * 统一排除 Nebula 框架不需要的 Spring Boot 默认自动配置
 * 
 * <p>设计理念：</p>
 * <ul>
 *   <li>使用 Nebula 框架的应用，默认不需要某些 Spring Boot 原生自动配置</li>
 *   <li>Nebula 提供了自己的条件化自动配置来替代这些功能</li>
 *   <li>如果应用确实需要某些被排除的功能，可以在 Nebula 框架内实现</li>
 * </ul>
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
public class NebulaAutoConfigurationImportFilter implements AutoConfigurationImportFilter {
    
    /** 
     * 是否为 Gateway 项目（检测 Spring Cloud Gateway 类路径）
     */
    private static final boolean IS_GATEWAY_PROJECT = isGatewayProject();
    
    /**
     * 需要排除的自动配置类列表
     */
    private static final Set<String> EXCLUDED_AUTO_CONFIGURATIONS = new HashSet<>(Arrays.asList(
        // ========================================
        // 服务发现相关（Nebula 有自己的服务发现实现）
        // ========================================
        "org.springframework.cloud.client.serviceregistry.AutoServiceRegistrationAutoConfiguration",
        "org.springframework.cloud.consul.serviceregistry.ConsulAutoServiceRegistrationAutoConfiguration",
        "org.springframework.cloud.zookeeper.serviceregistry.ZookeeperAutoServiceRegistrationAutoConfiguration",
        
        // ========================================
        // 数据持久化相关（非持久化应用不需要）
        // ========================================
        // 注意：这些只在没有显式配置 nebula.data.persistence.enabled=true 时排除
        // Nebula 的 DataPersistenceAutoConfiguration 会根据配置决定是否启用
        "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
        "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration",
        "org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration",
        "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration",
        "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration",
        
        // MyBatis（Nebula 在需要时会通过 DataPersistenceAutoConfiguration 配置）
        "org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration",
        "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration",
        
        // ========================================
        // Redis Repositories（Nebula 使用 Redis 做缓存，不用做仓储）
        // ========================================
        "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration",
        
        // ========================================
        // MongoDB Repositories（Nebula 暂不支持 MongoDB 仓储模式）
        // ========================================
        "org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration",
        
        // ========================================
        // Elasticsearch Repositories（Nebula 有自己的搜索实现）
        // ========================================
        "org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration",
        
        // ========================================
        // LDAP（大多数应用不需要）
        // ========================================
        "org.springframework.boot.autoconfigure.ldap.LdapAutoConfiguration",
        
        // ========================================
        // Solr（Nebula 不支持 Solr）
        // ========================================
        "org.springframework.boot.autoconfigure.solr.SolrAutoConfiguration",
        
        // ========================================
        // Neo4j（Nebula 不支持 Neo4j）
        // ========================================
        "org.springframework.boot.autoconfigure.data.neo4j.Neo4jDataAutoConfiguration",
        "org.springframework.boot.autoconfigure.data.neo4j.Neo4jRepositoriesAutoConfiguration",
        
        // ========================================
        // Cassandra（Nebula 不支持 Cassandra）
        // ========================================
        "org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration",
        "org.springframework.boot.autoconfigure.data.cassandra.CassandraDataAutoConfiguration",
        "org.springframework.boot.autoconfigure.data.cassandra.CassandraRepositoriesAutoConfiguration",
        
        // ========================================
        // Couchbase（Nebula 不支持 Couchbase）
        // ========================================
        "org.springframework.boot.autoconfigure.couchbase.CouchbaseAutoConfiguration",
        "org.springframework.boot.autoconfigure.data.couchbase.CouchbaseDataAutoConfiguration",
        "org.springframework.boot.autoconfigure.data.couchbase.CouchbaseRepositoriesAutoConfiguration",
        
        // ========================================
        // ActiveMQ（Nebula 使用 RabbitMQ）
        // ========================================
        "org.springframework.boot.autoconfigure.jms.activemq.ActiveMQAutoConfiguration",
        "org.springframework.boot.autoconfigure.jms.artemis.ArtemisAutoConfiguration",
        
        // ========================================
        // Batch（需要时可以手动启用）
        // ========================================
        "org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration",
        
        // ========================================
        // Quartz（Nebula 有自己的任务调度 nebula-task）
        // ========================================
        "org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration",
        
        // ========================================
        // WebSocket（需要时可以手动启用）
        // ========================================
        "org.springframework.boot.autoconfigure.websocket.servlet.WebSocketServletAutoConfiguration",
        "org.springframework.boot.autoconfigure.websocket.reactive.WebSocketReactiveAutoConfiguration",
        
        // ========================================
        // Reactive 相关（Nebula 主要支持 Servlet 模式）
        // ========================================
        "org.springframework.boot.autoconfigure.data.r2dbc.R2dbcDataAutoConfiguration",
        "org.springframework.boot.autoconfigure.data.r2dbc.R2dbcRepositoriesAutoConfiguration",
        "org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration",
        "org.springframework.boot.autoconfigure.r2dbc.R2dbcTransactionManagerAutoConfiguration",
        
        // ========================================
        // Flyway & Liquibase（数据库迁移工具，需要时手动启用）
        // ========================================
        "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
        "org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration",
        
        // ========================================
        // Security（Nebula 有自己的安全模块）
        // ========================================
        // 注意：这个排除可能需要根据实际情况调整
        // "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration",
        // "org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration",
        
        // ========================================
        // GraphQL（需要时手动启用）
        // ========================================
        "org.springframework.boot.autoconfigure.graphql.GraphQlAutoConfiguration",
        "org.springframework.boot.autoconfigure.graphql.servlet.GraphQlWebMvcAutoConfiguration",
        "org.springframework.boot.autoconfigure.graphql.reactive.GraphQlWebFluxAutoConfiguration"
    ));
    
    /**
     * Gateway 项目需要额外排除的自动配置类
     * <p>
     * gRPC Server 自动配置仅适用于后端微服务，网关项目不需要启动 gRPC 服务器
     * 网关仅作为 gRPC 客户端使用，将 HTTP 请求转换为 gRPC 调用
     */
    private static final Set<String> GATEWAY_EXCLUDED_AUTO_CONFIGURATIONS = new HashSet<>(Arrays.asList(
        // ========================================
        // grpc-spring-boot-starter 服务器端配置
        // 网关不需要启动 gRPC 服务器，只需要客户端功能
        // ========================================
        "net.devh.boot.grpc.server.autoconfigure.GrpcServerAutoConfiguration",
        "net.devh.boot.grpc.server.autoconfigure.GrpcServerFactoryAutoConfiguration",
        "net.devh.boot.grpc.server.autoconfigure.GrpcServerSecurityAutoConfiguration"
    ));
    
    /**
     * 检测是否为 Gateway 项目
     * 通过检测 Spring Cloud Gateway 核心类是否在类路径中
     */
    private static boolean isGatewayProject() {
        try {
            Class.forName("org.springframework.cloud.gateway.filter.GatewayFilter");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * 过滤自动配置
     * 
     * @param autoConfigurationClasses 自动配置类数组
     * @param autoConfigurationMetadata 自动配置元数据
     * @return 过滤结果数组，true 表示应该导入，false 表示应该排除
     */
    @Override
    public boolean[] match(String[] autoConfigurationClasses, AutoConfigurationMetadata autoConfigurationMetadata) {
        boolean[] matches = new boolean[autoConfigurationClasses.length];
        
        for (int i = 0; i < autoConfigurationClasses.length; i++) {
            String autoConfigurationClass = autoConfigurationClasses[i];
            // 检查通用排除列表
            if (EXCLUDED_AUTO_CONFIGURATIONS.contains(autoConfigurationClass)) {
                matches[i] = false;
                continue;
            }
            // 如果是 Gateway 项目，额外检查 Gateway 专用排除列表
            if (IS_GATEWAY_PROJECT && GATEWAY_EXCLUDED_AUTO_CONFIGURATIONS.contains(autoConfigurationClass)) {
                matches[i] = false;
                continue;
            }
            matches[i] = true;
        }
        
        return matches;
    }
}

