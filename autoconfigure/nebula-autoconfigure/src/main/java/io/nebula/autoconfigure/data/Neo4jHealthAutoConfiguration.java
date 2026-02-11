package io.nebula.autoconfigure.data;

import io.nebula.data.neo4j.support.Neo4jHealthIndicator;
import org.neo4j.driver.Driver;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Neo4j 健康检查自动配置
 * <p>
 * 独立于 {@link Neo4jAutoConfiguration}，仅当 Actuator 在 classpath 时生效。
 * 避免 Actuator 不存在时因 {@link HealthIndicator} 类加载失败导致主配置不可用。
 *
 * @author Nebula Framework
 * @since 2.0.1
 */
@AutoConfiguration(after = Neo4jAutoConfiguration.class)
@ConditionalOnClass({Driver.class, HealthIndicator.class})
@ConditionalOnProperty(prefix = "nebula.data.neo4j", name = "health-check-enabled",
        havingValue = "true", matchIfMissing = true)
public class Neo4jHealthAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(Neo4jHealthIndicator.class)
    public Neo4jHealthIndicator neo4jHealthIndicator(Driver driver) {
        return new Neo4jHealthIndicator(driver);
    }
}
