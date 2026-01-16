# Nebula Starter MCP - 配置参考

本文档说明 MCP Server 启动器的核心配置项。

## 配置前缀

- `nebula.ai`（AI 能力）
- `spring.ai.mcp.server`（MCP Server）

## AI 配置示例

```yaml
nebula:
  ai:
    enabled: true
    openai:
      api-key: ${OPENAI_API_KEY}
      base-url: ${OPENAI_BASE_URL:https://api.openai.com}
    vector-store:
      chroma:
        host: localhost
        port: 9002
        collection-name: docs
```

## MCP Server 配置示例

```yaml
spring:
  ai:
    mcp:
      server:
        enabled: true
```
