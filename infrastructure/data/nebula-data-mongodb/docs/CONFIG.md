# Nebula Data MongoDB 配置指南

> MongoDB数据访问配置说明

## 概述

`nebula-data-mongodb` 提供 MongoDB 的集成支持,适用于存储非结构化数据、日志、审计信息等场景。

## 基本配置

### Maven依赖

```xml
<dependency>
    <groupId>com.andy.nebula</groupId>
    <artifactId>nebula-data-mongodb</artifactId>
    <version>2.0.0-SNAPSHOT</version>
</dependency>
```

### 单机配置

```yaml
nebula:
  data:
    mongodb:
      enabled: true
      uri: mongodb://localhost:27017/ticket_db
      username: ${MONGO_USERNAME}
      password: ${MONGO_PASSWORD}
      # 数据库名称
      database: ticket_db
      # 认证数据库
      authentication-database: admin
```

### 副本集配置

```yaml
nebula:
  data:
    mongodb:
      enabled: true
      # 副本集连接字符串
      uri: mongodb://mongo1:27017,mongo2:27017,mongo3:27017/ticket_db?replicaSet=rs0
      username: ${MONGO_USERNAME}
      password: ${MONGO_PASSWORD}
      database: ticket_db
      authentication-database: admin
      # 读取偏好: primary(主节点) / primaryPreferred / secondary / secondaryPreferred / nearest
      read-preference: secondaryPreferred
      # 写关注: w1(单节点确认) / majority(多数节点确认)
      write-concern: majority
```

## 票务系统场景

### 操作日志存储

```yaml
nebula:
  data:
    mongodb:
      enabled: true
      uri: mongodb://mongo1:27017,mongo2:27017,mongo3:27017/ticket_logs
      database: ticket_logs
      # 日志场景配置
      collections:
        operation_logs:
          # 自动创建索引
          indexes:
            - keys: { userId: 1, createTime: -1 }
            - keys: { action: 1 }
            - keys: { createTime: 1 }
              expireAfterSeconds: 7776000  # 90天后自动删除
```

### 使用示例

```java
@Document(collection = "operation_logs")
@Data
public class OperationLog {
    @Id
    private String id;
    private Long userId;
    private String username;
    private String action;
    private String resource;
    private Map<String, Object> params;
    private LocalDateTime createTime;
}

@Repository
public interface OperationLogRepository extends MongoRepository<OperationLog, String> {
    List<OperationLog> findByUserIdOrderByCreateTimeDesc(Long userId, Pageable pageable);
    List<OperationLog> findByActionAndCreateTimeBetween(String action, LocalDateTime start, LocalDateTime end);
}
```

## 性能优化

### 连接池配置

```yaml
nebula:
  data:
    mongodb:
      # 连接池配置
      min-pool-size: 10
      max-pool-size: 100
      max-wait-time: 2000
      max-connection-idle-time: 60000
      max-connection-life-time: 120000
      # 连接超时
      connect-timeout: 10000
      socket-timeout: 60000
```

### 索引配置

```java
@Document(collection = "operation_logs")
@CompoundIndexes({
    @CompoundIndex(name = "user_time_idx", def = "{'userId': 1, 'createTime': -1}"),
    @CompoundIndex(name = "action_idx", def = "{'action': 1}")
})
public class OperationLog {
    // ...
}
```

## 环境配置

### 开发环境

```yaml
nebula:
  data:
    mongodb:
      enabled: true
      uri: mongodb://localhost:27017/ticket_dev
      database: ticket_dev
```

### 生产环境

```yaml
nebula:
  data:
    mongodb:
      enabled: true
      uri: mongodb://${MONGO_HOST1}:27017,${MONGO_HOST2}:27017,${MONGO_HOST3}:27017/ticket_prod?replicaSet=rs0&retryWrites=true&w=majority
      username: ${MONGO_USERNAME}
      password: ${MONGO_PASSWORD}
      database: ticket_prod
      authentication-database: admin
      read-preference: secondaryPreferred
      write-concern: majority
      max-pool-size: 100
```

---

**最后更新**: 2025-11-20  
**文档版本**: v1.0

