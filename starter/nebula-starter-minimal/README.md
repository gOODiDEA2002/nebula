# Nebula Starter Minimal

最小化的Nebula框架Starter，适用于CLI应用、批处理任务和工具库。

## 适用场景

- 🔧 CLI命令行工具
- 📊 批处理/数据处理脚本
- 📦 工具库项目
- 🔄 定时任务/Job

## 包含模块

| 模块 | 描述 |
|------|------|
| `nebula-foundation` | 基础工具类、异常处理、结果封装 |
| `nebula-autoconfigure` | 自动配置支持 |
| Spring Boot Starter | Spring Boot核心依赖 |

## 功能特性

- ✅ 基础工具类
  - 字符串处理、日期时间、加密解密
  - JSON序列化、文件操作
- ✅ 统一异常处理
- ✅ 统一结果封装 (`Result<T>`)
- ✅ 参数验证支持
- ✅ 日志框架集成

## 内存占用

**~100MB** (基础组件)

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-starter-minimal</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>
```

### 2. 创建主类

```java
package com.example.cli;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CliApplication implements CommandLineRunner {
    
    public static void main(String[] args) {
        SpringApplication.run(CliApplication.class, args);
    }
    
    @Override
    public void run(String... args) {
        System.out.println("Hello Nebula Minimal!");
        // 你的业务逻辑
    }
}
```

### 3. 使用工具类

```java
import io.nebula.core.util.StringUtils;
import io.nebula.core.util.JsonUtils;
import io.nebula.core.common.result.Result;

// 字符串工具
boolean isEmpty = StringUtils.isEmpty("test");

// JSON工具
String json = JsonUtils.toJson(object);
Object obj = JsonUtils.fromJson(json, Object.class);

// 结果封装
Result<String> success = Result.success("OK");
Result<String> error = Result.error("ERROR", "Something wrong");
```

## 配置示例

`application.yml`:

```yaml
spring:
  application:
    name: my-cli-app

logging:
  level:
    root: INFO
    com.example: DEBUG
```

## 升级到其他Starter

如果需要更多功能，可以升级到其他Starter：

- 需要Web功能? → `nebula-starter-web`
- 需要微服务能力? → `nebula-starter-service`
- 需要AI功能? → `nebula-starter-ai`

只需修改`artifactId`即可：

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-starter-web</artifactId>  <!-- 改为web -->
    <version>2.0.1-SNAPSHOT</version>
</dependency>
```

## 不包含的功能

以下功能需要切换到其他Starter：

- ❌ Web服务器 (Tomcat)
- ❌ REST API支持
- ❌ 数据库访问
- ❌ 缓存
- ❌ RPC调用
- ❌ 服务发现
- ❌ 消息队列
- ❌ AI功能

## 示例项目

参考示例: `nebula/examples/nebula-example-cli`

## 文档

- [Nebula框架使用指南](../../docs/Nebula框架使用指南.md)
- [Foundation模块文档](../../core/nebula-foundation/README.md)

---

**版本**: 2.0.1-SNAPSHOT  
**维护**: Nebula Framework Team

