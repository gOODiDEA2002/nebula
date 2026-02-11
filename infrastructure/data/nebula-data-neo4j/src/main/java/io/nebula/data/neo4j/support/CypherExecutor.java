package io.nebula.data.neo4j.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Cypher 执行工具类
 * <p>
 * 封装 Neo4j Driver 的 Session 管理和 Cypher 执行操作，
 * 提供简洁的 API 供业务层调用。
 */
@Slf4j
@RequiredArgsConstructor
public class CypherExecutor {

    private final Driver driver;

    /**
     * 执行写操作 Cypher（无返回值）
     *
     * @param cypher Cypher 语句
     * @param params 参数映射
     */
    public void execute(String cypher, Map<String, Object> params) {
        try (Session session = driver.session()) {
            session.executeWrite(tx -> {
                tx.run(cypher, params);
                return null;
            });
        }
    }

    /**
     * 执行读操作 Cypher 并映射结果
     *
     * @param cypher  Cypher 语句
     * @param params  参数映射
     * @param mapper  结果映射函数
     * @return 映射后的结果列表
     */
    public <T> List<T> query(String cypher, Map<String, Object> params,
                             Function<org.neo4j.driver.Record, T> mapper) {
        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                Result result = tx.run(cypher, params);
                List<T> list = new ArrayList<>();
                while (result.hasNext()) {
                    list.add(mapper.apply(result.next()));
                }
                return list;
            });
        }
    }

    /**
     * 执行读操作 Cypher 返回单个结果
     *
     * @param cypher Cypher 语句
     * @param params 参数映射
     * @param mapper 结果映射函数
     * @return 单个结果，无结果返回 null
     */
    public <T> T queryOne(String cypher, Map<String, Object> params,
                          Function<org.neo4j.driver.Record, T> mapper) {
        List<T> results = query(cypher, params, mapper);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * 执行 Cypher 返回受影响的行数
     *
     * @param cypher Cypher 语句
     * @param params 参数映射
     * @return 受影响的行数
     */
    public long executeAndCount(String cypher, Map<String, Object> params) {
        try (Session session = driver.session()) {
            return session.executeWrite(tx -> {
                Result result = tx.run(cypher, params);
                var counters = result.consume().counters();
                return (long) counters.nodesCreated() + counters.relationshipsCreated();
            });
        }
    }

    /**
     * 批量执行 Cypher（在同一个事务中）
     *
     * @param statements Cypher 语句和参数的列表
     */
    public void executeBatch(List<Map.Entry<String, Map<String, Object>>> statements) {
        try (Session session = driver.session()) {
            session.executeWrite(tx -> {
                for (Map.Entry<String, Map<String, Object>> stmt : statements) {
                    tx.run(stmt.getKey(), stmt.getValue());
                }
                return null;
            });
        }
    }

    /**
     * 检查 Neo4j 连接是否正常
     *
     * @return true 连接正常
     */
    public boolean isAvailable() {
        try {
            driver.verifyConnectivity();
            return true;
        } catch (Exception e) {
            log.warn("Neo4j 连接检查失败", e);
            return false;
        }
    }
}
