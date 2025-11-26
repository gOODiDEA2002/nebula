# Nebula AI Spring 配置指南

> Spring AI集成配置说明

## 概述

`nebula-ai-spring` 提供 Spring AI 的集成支持。

## 基本配置

### Maven依赖

```xml
<dependency>
    <groupId>com.andy.nebula</groupId>
    <artifactId>nebula-ai-spring</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>
```

### AI配置

```yaml
nebula:
  ai:
    spring:
      enabled: true
      # OpenAI配置
      openai:
        api-key: ${OPENAI_API_KEY}
        model: gpt-4
        temperature: 0.7
```

## 票务系统场景

### 智能客服

```java
@Service
public class CustomerServiceAI {
    
    @Autowired
    private ChatClient chatClient;
    
    public String answer(String question) {
        return chatClient.call(question);
    }
}
```

---

**最后更新**: 2025-11-20

