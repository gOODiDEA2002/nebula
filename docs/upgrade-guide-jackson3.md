# Nebula 2.1.x Jackson 3 升级指南

> 适用版本：Nebula 2.1.0 及以上
> 前置版本：Nebula 2.0.x（Jackson 2）

## 1. 概述

Nebula 2.1.x 完成了 Jackson 2（`com.fasterxml.jackson`）到 Jackson 3（`tools.jackson`）的全量迁移。
框架内部所有 JSON 序列化/反序列化均已切换到 Jackson 3，Spring MVC 响应走 Jackson 3 默认转换器，
`@SensitiveData` 脱敏、日期格式化等定制均已在 Jackson 3 上重新实现。

升级后，下游应用需关注以下几个方面。

## 2. 必须操作：清理 Redis 缓存

> 对应技术决策：Q5（2026-07-07 拍板，不做双读兼容层）

Jackson 3 的序列化格式与 Jackson 2 不完全兼容（类型标识、日期格式等均有差异）。
**升级部署前必须清除 Redis 中由 Nebula 缓存模块写入的全部缓存条目**，否则反序列化会报错。

```bash
# 使用 SCAN 遍历删除（KEYS 命令会阻塞 Redis，生产环境禁用）
# keyPrefix 默认为 nebula:cache:
redis-cli --scan --pattern "nebula:cache:*" | while IFS= read -r key; do redis-cli UNLINK "$key"; done

# proud-day 应用的缓存前缀
redis-cli --scan --pattern "app:cache:*" | while IFS= read -r key; do redis-cli UNLINK "$key"; done
```

如果使用了自定义 `keyPrefix`，请替换为实际前缀。

**滚动升级不可行**：新旧实例并存期间，新实例写入的 Jackson 3 格式缓存无法被旧实例读取，反之亦然。
建议采用**停机发布**或**蓝绿部署**，一次性切换全部实例后再开启流量。

## 3. 坐标与包名变更

| Jackson 2 | Jackson 3 | 说明 |
|---|---|---|
| `com.fasterxml.jackson.core:jackson-databind` | `tools.jackson.core:jackson-databind` | Maven 坐标 |
| `com.fasterxml.jackson.databind.*` | `tools.jackson.databind.*` | 包名 |
| `com.fasterxml.jackson.core.*` | `tools.jackson.core.*` | 包名 |
| `com.fasterxml.jackson.datatype:jackson-datatype-jsr310` | 不再需要（并入 databind） | java.time 开箱支持 |
| **`com.fasterxml.jackson.annotation.*`** | **不变** | 注解包保留原包名 |

## 4. 下游应用代码迁移要点

### 4.1 ObjectMapper 构建

Jackson 3 的 `ObjectMapper` 为不可变对象，构建方式改变：

```java
// Jackson 2
ObjectMapper mapper = new ObjectMapper();
mapper.registerModule(new JavaTimeModule());
mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

// Jackson 3
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

ObjectMapper mapper = JsonMapper.builder()
        .build();
// java.time 支持已内置，无需 JavaTimeModule
// WRITE_DATES_AS_TIMESTAMPS 已移除（默认 ISO-8601）
```

### 4.2 异常处理

Jackson 3 的 `JacksonException` 改为非受检异常（`RuntimeException` 子类）：

```java
// Jackson 2
try {
    String json = mapper.writeValueAsString(obj);
} catch (JsonProcessingException e) {  // 受检异常
    // ...
}

// Jackson 3
try {
    String json = mapper.writeValueAsString(obj);
} catch (tools.jackson.core.JacksonException e) {  // 非受检异常
    // ...
}
```

### 4.3 JsonNode API 变化

```java
// Jackson 2
Iterator<Map.Entry<String, JsonNode>> it = node.fields();

// Jackson 3
Iterable<Map.Entry<String, JsonNode>> props = node.properties();
```

### 4.4 注解无需迁移

`@JsonProperty`、`@JsonIgnore`、`@JsonFormat` 等注解的包名 `com.fasterxml.jackson.annotation`
在 Jackson 3 中保持不变（官方兼容设计），**无需修改任何注解 import**。

## 5. Spring Boot 集成变化

### 5.1 MVC JSON 转换器

Nebula 2.1.x 已移除 `spring.http.converters.preferred-json-mapper=jackson2` 配置注入，
MVC 默认走 Jackson 3 转换器。`@SensitiveData` 脱敏通过 `JsonMapperBuilderCustomizer` 注册到
Jackson 3 的 `JsonMapper` 上，无需额外配置。

### 5.2 Redis 序列化器

缓存模块的 Redis 值序列化器从 `GenericJackson2JsonRedisSerializer` 切换到
`GenericJacksonJsonRedisSerializer`（spring-data-redis 4.1 提供），多态类型安全校验等级不变。

### 5.3 Elasticsearch 客户端

ES 客户端的 JSON 映射器从 `JacksonJsonpMapper` 切换到 `Jackson3JsonpMapper`
（elasticsearch-java 9.4.2 提供）。

## 6. 已知的 Jackson 2 运行时残留

| 来源 | 说明 | 影响 |
|---|---|---|
| `io.jsonwebtoken:jjwt-jackson:0.13.0` | JJWT 尚无 Jackson 3 适配（issue #1029） | 仅影响 JWT 内部序列化，不经过框架 ObjectMapper，无外部可见影响 |
| `org.springframework.ai:spring-ai-model` | Spring AI 2.0 传递 Jackson 2 databind | 第三方传递依赖，框架代码不直接使用 |

后续升级 JJWT 前，应重新核对其 Jackson 3 支持状态和迁移说明，再决定是否移除该兼容依赖。
