# Nebula Starter 选择与默认值机制

## 如何选择 Starter

Nebula 按应用形态提供多个 Starter，引入对应 Starter 即可开箱即用，无需逐个声明模块开关。

| Starter | 适用形态 | 默认启用模块 |
|---|---|---|
| nebula-starter-minimal | 仅核心工具 | 无 |
| nebula-starter-web | 单体 Web 应用 | persistence, cache |
| nebula-starter-service | 微服务节点 | persistence, cache, http-rpc, rpc-discovery, nacos, lock |
| nebula-starter-ai | AI / RAG 应用 | ai, cache |
| nebula-starter-all | 全功能单体 | 绝大多数模块 |

## 默认值注入原理

每个 Starter 通过 `META-INF/nebula-defaults.properties` 声明默认启用的模块，由 `NebulaStarterDefaultsPostProcessor`（一个 `EnvironmentPostProcessor`）以最低优先级注入 Environment。

```properties
# 示例：nebula-starter-web 的 nebula-defaults.properties
nebula.data.persistence.enabled=true
nebula.data.cache.enabled=true
```

用户在 `application.yml` 中的配置始终可以覆盖 Starter 默认值，因为应用配置的优先级高于 `EnvironmentPostProcessor` 注入的默认值。

## 常见问题

引入 Starter 后仍提示模块未启用，通常是应用配置里显式写了 `enabled: false`，或引入的 Starter 不包含该模块。核对 Starter 的 `pom.xml` 依赖清单即可确认。
