# Nebula Starter AI Example

> 使用 `nebula-starter-ai` 的 AI 应用示例

## 功能特性

- 基于 `nebula-starter-ai`，集成 Spring AI
- 对话服务（`ChatService`）
- 文本嵌入（`EmbeddingService`）
- 向量存储（Chroma 向量数据库）
- AI 功能关闭时返回 `AI disabled`，且不创建远程客户端

## 项目结构

```
starter-ai-example/
├── pom.xml
└── src/main/
    ├── java/io/nebula/examples/ai/
    │   ├── AiApplication.java            # 启动类
    │   └── controller/
    │       └── AiController.java         # AI 接口控制器
    └── resources/
        └── application.yml               # 应用配置
```

## 前置条件

- JDK 21+
- Maven 3.8+
- OpenAI API Key（启用 AI 功能时需要）
- Chroma 向量数据库（启用向量存储时需要，默认连接 `localhost:9002`）

## 快速开始

```bash
# 1. 安装框架到本地仓库（首次需要）
cd /path/to/nebula
mvn install -DskipTests

# 2. 启动应用（端口 8083，AI 默认禁用）
mvn -q -f examples/starter-ai-example spring-boot:run

# 3. 启用 AI 功能
export AI_ENABLED=true
export OPENAI_API_KEY='<test-api-key>'
export CHROMA_HOST=localhost
export CHROMA_PORT=9002
mvn -q -f examples/starter-ai-example spring-boot:run

# 4. 执行完整 E2E；密钥只从环境变量读取
E2E_MODE=full examples/starter-ai-example/e2e-test.sh
```

## 接口测试

```bash
# AI 回显接口（AI 禁用时返回 "AI disabled"）
curl "http://localhost:8083/ai/echo?q=hello"
# 响应中的 data 为 "AI disabled"

# 查看三个 AI 服务是否已创建
curl "http://localhost:8083/ai/status"

# 启用 AI 后的真实对话
curl "http://localhost:8083/ai/echo?q=介绍一下Java21的新特性"
# 响应: {"code":200,"message":"success","data":"Java 21 引入了虚拟线程..."}
```

## 配置说明

```yaml
server:
  port: 8083

nebula:
  data:
    cache:
      enabled: false
  ai:
    enabled: ${AI_ENABLED:false}
    openai:
      api-key: ${OPENAI_API_KEY:}
      base-url: ${OPENAI_BASE_URL:https://api.openai.com/v1}
      chat:
        options:
          model: ${OPENAI_CHAT_MODEL:gpt-4o-mini}
          max-retries: ${OPENAI_MAX_RETRIES:0}
      embedding:
        options:
          model: ${OPENAI_EMBEDDING_MODEL:text-embedding-3-small}
          max-retries: ${OPENAI_MAX_RETRIES:0}
    vector-store:
      chroma:
        host: ${CHROMA_HOST:localhost}
        port: ${CHROMA_PORT:9002}
        collection-name: ${CHROMA_COLLECTION:nebula_vectors}
        initialize-schema: true
```

## 核心代码

```java
@RestController
public class AiController {
    private final ChatService chatService;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;

    public AiController(ObjectProvider<ChatService> chatServiceProvider,
                        ObjectProvider<EmbeddingService> embeddingServiceProvider,
                        ObjectProvider<VectorStoreService> vectorStoreServiceProvider) {
        this.chatService = chatServiceProvider.getIfAvailable();
        this.embeddingService = embeddingServiceProvider.getIfAvailable();
        this.vectorStoreService = vectorStoreServiceProvider.getIfAvailable();
    }

    @GetMapping("/ai/echo")
    public Result<String> echo(@RequestParam(defaultValue = "hello") String q) {
        if (chatService == null) {
            return Result.success("AI disabled");
        }
        String r = chatService.chat(q).getContent();
        return Result.success(r);
    }
}
```

完整 E2E 会执行 1 次聊天、1 次显式 embedding、1 次向量写入和 1 次向量查询，共 4 次外部模型请求。
测试 collection 使用 `nebula_e2e_` 前缀，并在应用停止后删除及复核。

## 相关文档

- [Nebula Examples 总览](../README.md)
- [nebula-starter-ai](../../starter/nebula-starter-ai/pom.xml)
- [AI 功能测试指南](../fullstack-example/docs/nebula-ai-test.md)
