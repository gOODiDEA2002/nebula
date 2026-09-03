# 三级启用策略与配置覆盖

## 自动配置的三级启用策略

Nebula 的自动配置按依赖的外部条件分三级，核心区别在 `matchIfMissing`。

### Level 1：默认启用

纯内存组件（如 Security 注解切面）采用 `matchIfMissing = true`，不配置也生效。

### Level 2：默认禁用，需要外部服务

依赖数据库、Redis、消息队列、Elasticsearch 等外部服务的模块采用 `matchIfMissing = false`，必须显式打开，避免无中间件时启动即报错。

### Level 3：默认禁用，特定部署形态

RPC、Gateway、AI、Crawler 等只在特定形态下需要的模块同样默认禁用，由 Starter 默认值或用户配置按需打开。

## 配置覆盖优先级

从高到低依次为：

1. 命令行参数
2. 环境变量
3. 应用配置文件（application.yml）
4. Starter 默认值（nebula-defaults.properties）
5. 框架默认配置

## 模块启用开关怎么配

每个模块的开关形如 `nebula.<模块>.enabled`。例如打开 AI 与 RAG：

- `nebula.ai.enabled=true`
- `nebula.ai.rag.enabled=true`

关闭某个 Starter 默认启用的模块，只需在 application.yml 里把对应 `enabled` 显式设为 `false`。
