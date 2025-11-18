# Spring AI BOM 集成说明

## 概述

为了统一管理 Spring AI 依赖版本，Nebula Framework 在父 POM (`nebula-parent`) 中集成了 Spring AI BOM。

## 版本管理

### 当前版本
- **Spring AI**: 1.0.3

### 配置位置

#### nebula-parent/pom.xml

```xml
<properties>
    <!-- AI -->
    <spring-ai.version>1.0.3</spring-ai.version>
</properties>

<dependencyManagement>
    <dependencies>
        <!-- Spring AI BOM -->
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>${spring-ai.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

## 优势

### 1. 统一版本管理
- 所有 Nebula 模块使用相同的 Spring AI 版本
- 避免版本冲突
- 简化版本升级

### 2. 简化依赖声明
模块中无需指定 Spring AI 依赖的版本：

```xml
<!-- ❌ 旧方式：需要指定版本 -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-openai</artifactId>
    <version>1.0.3</version>
</dependency>

<!-- ✅ 新方式：版本由父 POM 管理 -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-openai</artifactId>
</dependency>
```

### 3. 兼容性保证
- Spring AI BOM 确保所有 Spring AI 组件之间的兼容性
- 减少由于版本不匹配导致的问题

## 使用指南

### 在 Nebula 模块中使用

如果您的模块需要使用 Spring AI，只需添加相应的依赖（无需指定版本）：

```xml
<dependencies>
    <!-- OpenAI Chat Model -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-model-openai</artifactId>
    </dependency>

    <!-- Chroma Vector Store -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-vector-store-chroma</artifactId>
    </dependency>

    <!-- Ollama -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-ollama</artifactId>
    </dependency>

    <!-- Pinecone Vector Store -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-pinecone</artifactId>
    </dependency>
</dependencies>
```

### 在应用项目中使用

如果您的应用使用 Nebula Starter，Spring AI BOM 会自动生效：

```xml
<parent>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-parent</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</parent>

<dependencies>
    <!-- Nebula AI Starter -->
    <dependency>
        <groupId>io.nebula</groupId>
        <artifactId>nebula-starter-ai</artifactId>
    </dependency>

    <!-- 额外的 Spring AI 组件（无需指定版本） -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-anthropic</artifactId>
    </dependency>
</dependencies>
```

## Spring AI 可用组件

### Chat Models（对话模型）
- `spring-ai-starter-model-openai` - OpenAI (GPT-3.5, GPT-4)
- `spring-ai-ollama` - Ollama (本地 LLM)
- `spring-ai-anthropic` - Anthropic Claude
- `spring-ai-azure-openai` - Azure OpenAI
- `spring-ai-huggingface` - Hugging Face

### Embedding Models（嵌入模型）
- `spring-ai-starter-model-openai` - OpenAI Embeddings
- `spring-ai-ollama` - Ollama Embeddings
- `spring-ai-transformers` - Transformers.js

### Vector Stores（向量数据库）
- `spring-ai-starter-vector-store-chroma` - Chroma
- `spring-ai-pinecone` - Pinecone
- `spring-ai-weaviate` - Weaviate
- `spring-ai-qdrant` - Qdrant
- `spring-ai-milvus` - Milvus
- `spring-ai-pgvector` - PostgreSQL + pgvector

### Document Loaders（文档加载器）
- `spring-ai-pdf-document-reader` - PDF
- `spring-ai-tika-document-reader` - Apache Tika (支持多种格式)

### Other Components
- `spring-ai-retry` - 重试机制
- `spring-ai-observability` - 可观测性

## 版本升级

### 升级 Spring AI 版本

只需修改父 POM 中的版本号：

```xml
<properties>
    <spring-ai.version>1.1.0</spring-ai.version> <!-- 升级到新版本 -->
</properties>
```

所有使用 Spring AI 的模块会自动使用新版本。

### 兼容性检查

升级前请检查：
1. **Spring Boot 版本**: Spring AI 1.0.x 需要 Spring Boot 3.2+
2. **Java 版本**: Spring AI 需要 Java 17+（Nebula 使用 Java 21）
3. **Breaking Changes**: 查看 [Spring AI Release Notes](https://github.com/spring-projects/spring-ai/releases)

## 示例项目

### nebula-doc-mcp-server

该项目是 Spring AI BOM 的完整示例：

```xml
<parent>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-parent</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</parent>

<dependencies>
    <dependency>
        <groupId>io.nebula</groupId>
        <artifactId>nebula-starter-ai</artifactId>
    </dependency>
    
    <!-- Spring AI 组件（版本由 BOM 管理） -->
    <!-- 无需额外配置，nebula-starter-ai 已包含 -->
</dependencies>
```

## 相关文档

- [Spring AI 官方文档](https://docs.spring.io/spring-ai/reference/)
- [Spring AI GitHub](https://github.com/spring-projects/spring-ai)
- [Nebula AI 模块文档](../infrastructure/ai/nebula-ai-core/README.md)
- [Nebula AI Starter 文档](../starter/nebula-starter-ai/README.md)

## 常见问题

### Q: 为什么需要 Spring AI BOM？
A: BOM (Bill of Materials) 确保所有 Spring AI 组件版本一致，避免版本冲突。类似于 Spring Boot 的 `spring-boot-dependencies`。

### Q: 可以覆盖 BOM 中的版本吗？
A: 可以，但不推荐。如需特定版本，在模块的 `<dependencies>` 中显式指定：
```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-ollama</artifactId>
    <version>1.0.4</version> <!-- 覆盖 BOM 版本 -->
</dependency>
```

### Q: Spring AI BOM 包含哪些依赖？
A: 查看 [spring-ai-bom/pom.xml](https://github.com/spring-projects/spring-ai/blob/main/spring-ai-bom/pom.xml)

### Q: 如何查看当前使用的 Spring AI 版本？
A: 运行 `mvn dependency:tree | grep spring-ai`

---

**文档版本**: 2.0.1-SNAPSHOT  
**最后更新**: 2025-11-14  
**维护者**: Nebula Team

