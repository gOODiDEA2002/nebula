package io.nebula.data.neo4j.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

/**
 * Neo4j 健康检查指示器
 * <p>
 * 检查 Neo4j 连接状态和版本信息，集成到 Spring Boot Actuator。
 */
@Slf4j
@RequiredArgsConstructor
public class Neo4jHealthIndicator implements HealthIndicator {

    private final Driver driver;

    @Override
    public Health health() {
        try {
            driver.verifyConnectivity();

            // 获取 Neo4j 版本信息
            try (Session session = driver.session()) {
                Result result = session.run("CALL dbms.components() YIELD name, versions RETURN name, versions[0] AS version");
                if (result.hasNext()) {
                    org.neo4j.driver.Record record = result.next();
                    String name = record.get("name").asString();
                    String version = record.get("version").asString();

                    return Health.up()
                            .withDetail("server", name)
                            .withDetail("version", version)
                            .build();
                }
            }

            return Health.up().build();
        } catch (Exception e) {
            log.warn("Neo4j 健康检查失败", e);
            return Health.down()
                    .withException(e)
                    .build();
        }
    }
}
