# Nebula Starter MCP - 测试指南

## 测试前提

- MCP Server 已启动
- AI 配置已生效（OpenAI 或其他 Provider）
- 向量库可访问（如 Chroma）

## 测试步骤

1. 启动应用并确认日志中 MCP Server 启用提示。
2. 使用 MCP 客户端或 curl 进行简单调用：

```bash
curl -X POST http://localhost:8080/mcp/tools/searchDocs \
  -H "Content-Type: application/json" \
  -d '{"query":"nebula 分布式锁怎么用"}'
```

3. 校验返回内容是否包含预期文档片段。

## 常见问题

- 返回为空：检查向量库是否已索引内容。
- 连接失败：确认 `spring.ai.mcp.server.enabled=true`，且端口未被占用。
