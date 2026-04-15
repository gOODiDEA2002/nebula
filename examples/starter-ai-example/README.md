# Nebula Starter AI Example

> 使用 `nebula-starter-ai` 的 AI 应用示例

## 功能特性

- 基于 `nebula-starter-ai`，集成 Spring AI
- 对话服务（`ChatService`）
- 文本嵌入（`EmbeddingService`）
- 向量存储（Chroma 向量数据库）
- AI 功能优雅降级（未配置 API Key 时返回 "AI disabled"）

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
- Chroma 向量数据库（可选，用于 RAG）

## 快速开始

```bash
# 1. 安装框架到本地仓库（首次需要）
cd /path/to/nebula
mvn install -DskipTests

# 2. 启动应用（端口 8083，AI 默认禁用）
mvn -q -f examples/starter-ai-example spring-boot:run

# 3. 启用 AI 功能（设置 API Key）
mvn -q -f examples/starter-ai-example spring-boot:run \
  -Dspring-boot.run.arguments="--nebula.ai.enabled=true --nebula.ai.openai.api-key=sk-xxx"
```

## 接口测试

```bash
# AI 回显接口（AI 禁用时返回 "AI disabled"）
curl "http://localhost:8083/ai/echo?q=hello"
# 响应: {"code":200,"message":"success","data":"AI disabled",...}

# 启用 AI 后的真实对话
curl "http://localhost:8083/ai/echo?q=介绍一下Java21的新特性"
# 响应: {"code":200,"message":"success","data":"Java 21 引入了虚拟线程..."}
```

## 配置说明

```yaml
server:
  port: 8083

nebula:
  ai:
    enabled: false                     # 默认禁用，启用需设置 API Key
    openai:
      api-key:                         # OpenAI API Key
      base-url: https://api.openai.com # API 地址（可替换为兼容接口）
      chat:
        enabled: true                  # 启用对话
      embedding:
        enabled: true                  # 启用嵌入
    vector-store:
      chroma:
        url: http://localhost:8000     # Chroma 地址
        collection-name: nebula_vectors
        initialize-schema: true
```

## 核心代码

```java
@RestController
public class AiController {
    @Autowired(required = false)
    private ChatService chatService;

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

## 相关文档

- [Nebula Examples 总览](../README.md)
- [nebula-starter-ai](../../starter/nebula-starter-ai/pom.xml)
- [AI 功能测试指南](../fullstack-example/docs/nebula-ai-test.md)
