# nebula-ai-core 模块单元测试清单

## 模块说明

AI核心抽象层，定义了Chat、Embedding、VectorStore、MCP等核心接口和模型。由于主要是接口定义，测试重点在于模型的正确性和工具类的功能。

## 核心功能

1. AI模型抽象（ChatService, EmbeddingService）
2. 向量存储抽象（VectorStoreService, Document）
3. MCP协议抽象（Tool, Resource, McpClientService, McpServerService）
4. 消息模型（ChatMessage, ChatRequest, ChatResponse）

## 测试类清单

### 1. MessageModelTest

**测试类路径**: `io.nebula.ai.core.model.ChatMessageTest` (假设存在)
**测试目的**: 验证消息模型的构建和序列化

| 测试方法 | 被测试方法 | 测试目的 |
|---------|-----------|---------|
| testUserMessage() | ChatMessage构造 | 验证用户消息构建 |
| testAssistantMessage() | ChatMessage构造 | 验证助手消息构建 |
| testSystemMessage() | ChatMessage构造 | 验证系统消息构建 |
| testMessageSerialization() | - | 验证消息的JSON序列化/反序列化 |

### 2. DocumentModelTest

**测试类路径**: `io.nebula.ai.core.model.DocumentTest` (假设存在)
**测试目的**: 验证文档模型的构建和元数据处理

| 测试方法 | 被测试方法 | 测试目的 |
|---------|-----------|---------|
| testDocumentBuilder() | Document.builder() | 验证文档构建器 |
| testMetadata() | getMetadata() | 验证元数据操作 |

### 3. McpToolModelTest

**测试类路径**: `io.nebula.ai.core.mcp.McpToolTest` (假设存在)
**测试目的**: 验证MCP工具模型的定义

| 测试方法 | 被测试方法 | 测试目的 |
|---------|-----------|---------|
| testToolDefinition() | McpTool构造 | 验证工具名称、描述、参数定义 |

## 测试执行

```bash
mvn test -pl nebula/infrastructure/ai/nebula-ai-core
```

## 验收标准

- 所有模型类的构建和基本操作测试通过
- 序列化测试通过
