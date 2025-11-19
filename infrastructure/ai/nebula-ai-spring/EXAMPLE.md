# nebula-ai-spring 模块示例

## 模块简介

`nebula-ai-spring` 是 Nebula 框架 AI 能力的 Spring Boot 集成实现。它实现了 `nebula-ai-core` 定义的接口，并提供了对 OpenAI, Ollama, Chroma 等主流 AI 服务和向量数据库的自动配置。

## 核心功能示例

### 1. 配置 AI 服务

在 `application.yml` 中配置模型提供商和参数。

**`application.yml`**:

```yaml
nebula:
  ai:
    enabled: true
    
    # 聊天服务配置
    chat:
      default-provider: openai # 或 ollama
      providers:
        openai:
          api-key: ${OPENAI_API_KEY}
          model: gpt-4
          base-url: https://api.openai.com
          options:
            temperature: 0.7
            max-tokens: 2000
        ollama:
          base-url: http://localhost:11434
          model: llama3
    
    # 嵌入服务配置
    embedding:
      default-provider: openai
      providers:
        openai:
          api-key: ${OPENAI_API_KEY}
          model: text-embedding-3-small
    
    # 向量存储配置 (RAG)
    vector-store:
      default-provider: chroma
      chroma:
        host: localhost
        port: 8000
        collection-name: my-knowledge-base
        initialize-schema: true
```

### 2. 启动应用

引入模块后，相关服务会自动注入到 Spring 容器中。

**`io.nebula.example.ai.AiApplication`**:

```java
package io.nebula.example.ai;

import io.nebula.ai.core.chat.ChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@Slf4j
@SpringBootApplication
public class AiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiApplication.class, args);
    }

    @Bean
    public CommandLineRunner chatDemo(ChatService chatService) {
        return args -> {
            log.info("开始对话 (模型: {})...", chatService.getCurrentModel());
            
            String response = chatService.chat("用一句话介绍你自己").getContent();
            log.info("AI: {}", response);
        };
    }
}
```

### 3. 切换本地模型 (Ollama)

只需修改配置即可从 OpenAI 切换到本地运行的 Ollama 模型，代码无需变动。

```yaml
nebula:
  ai:
    chat:
      default-provider: ollama # 切换为 ollama
```

## 进阶特性

### 1. MCP (Model Context Protocol) 集成

本模块支持自动连接 MCP Server。需要在配置中指定 MCP Server 的连接信息（通常是 SSE 或 Stdio 方式，具体取决于实现细节）。连接成功后，AI 模型可以通过 `McpClientService` 发现并调用外部工具。

### 2. 多模型并存

可以在代码中动态选择使用哪个 Provider。虽然 `ChatService` 默认使用 `default-provider`，但底层实现通常允许通过限定符或工厂方法获取特定 Provider 的实例。

## 总结

`nebula-ai-spring` 让 Java 开发者能够以 Spring Boot 的原生方式快速构建 AI 应用，无论是对接商业模型 API 还是本地私有化模型，都能提供一致的开发体验。

