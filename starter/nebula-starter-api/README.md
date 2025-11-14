# Nebula Starter API

RPC 契约模块专用 Starter，提供 API 定义所需的标准依赖，确保所有契约模块使用一致的版本和配置。

## 适用场景

- **RPC 契约模块**：定义服务接口、DTO、VO 的独立模块
- **API 定义**：不包含实现，仅包含接口和数据模型
- **跨服务共享**：供服务提供方和消费方共同依赖

## 典型示例

- `nebula-example-user-api` - 用户服务 API 契约
- `nebula-example-order-api` - 订单服务 API 契约
- `nebula-integration-*-api` - 集成服务 API 契约

## 包含的依赖

### 1. Nebula RPC Core
```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-rpc-core</artifactId>
</dependency>
```
- **作用**：提供 RPC 核心注解（`@RpcClient`, `@RpcService` 等）
- **Scope**：`compile`（会传递到依赖方）

### 2. Spring Web
```xml
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-web</artifactId>
    <scope>provided</scope>
</dependency>
```
- **作用**：提供 Spring MVC 注解（`@RequestMapping`, `@PostMapping` 等）
- **Scope**：`provided`（编译时需要，运行时由应用提供）

### 3. Jakarta Validation
```xml
<dependency>
    <groupId>jakarta.validation</groupId>
    <artifactId>jakarta.validation-api</artifactId>
</dependency>
```
- **作用**：提供验证注解（`@Valid`, `@NotNull`, `@Size` 等）
- **Scope**：`compile`（会传递到依赖方）

### 4. Lombok
```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <scope>provided</scope>
</dependency>
```
- **作用**：减少样板代码（`@Data`, `@Builder`, `@AllArgsConstructor` 等）
- **Scope**：`provided`（编译时处理，运行时不需要）

## 使用方式

### 1. 创建 API 契约模块

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>my-service-api</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <name>My Service API</name>
    <description>My Service RPC API Contract</description>

    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <nebula.version>2.0.0-SNAPSHOT</nebula.version>
    </properties>

    <dependencies>
        <!-- 使用 Nebula Starter API -->
        <dependency>
            <groupId>io.nebula</groupId>
            <artifactId>nebula-starter-api</artifactId>
            <version>${nebula.version}</version>
        </dependency>
    </dependencies>
</project>
```

### 2. 定义 RPC 接口

```java
package com.example.api.rpc;

import com.example.api.dto.CreateUserDto;
import com.example.api.dto.GetUserDto;
import io.nebula.rpc.core.annotation.RpcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RpcClient(name = "my-service", path = "/api/v1/user")
public interface UserRpcClient {
    
    @PostMapping
    CreateUserDto.Response createUser(@RequestBody CreateUserDto.Request request);
    
    @GetMapping("/{id}")
    GetUserDto.Response getUserById(@PathVariable("id") Long id);
}
```

### 3. 定义 DTO

```java
package com.example.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

public class CreateUserDto {
    
    @Data
    public static class Request {
        @NotBlank(message = "用户名不能为空")
        private String username;
        
        @NotBlank(message = "密码不能为空")
        private String password;
        
        @NotBlank(message = "邮箱不能为空")
        private String email;
    }
    
    @Data
    public static class Response {
        @NotNull
        private Long userId;
        
        private String message;
    }
}
```

### 4. 定义实体/VO

```java
package com.example.api.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;
    private String username;
    private String email;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

## 模块结构示例

```
my-service-api/
├── pom.xml
└── src/
    └── main/
        └── java/
            └── com/
                └── example/
                    └── api/
                        ├── rpc/              # RPC 接口定义
                        │   └── UserRpcClient.java
                        ├── dto/              # 数据传输对象
                        │   ├── CreateUserDto.java
                        │   ├── GetUserDto.java
                        │   └── UpdateUserDto.java
                        ├── entity/           # 实体类
                        │   └── User.java
                        └── vo/               # 值对象
                            └── UserVo.java
```

## 最佳实践

### 1. 版本管理
- API 契约模块应独立版本控制
- 建议使用语义化版本（Semantic Versioning）
- 重大接口变更时升级主版本号

### 2. 接口设计
- 使用内部类定义 Request/Response（如 `CreateUserDto.Request`）
- 清晰的包结构（rpc、dto、entity、vo 分离）
- 充分使用验证注解，在接口层定义验证规则

### 3. 向后兼容性
- 不要删除现有字段，使用 `@Deprecated` 标记过时字段
- 添加新字段时提供默认值
- 不要修改现有方法签名，添加新方法替代

### 4. 文档
- 使用 JavaDoc 注释所有公开接口和字段
- 在 README.md 中说明接口的用途和使用方式

## 迁移指南

### 从手动依赖迁移

**迁移前**：
```xml
<dependencies>
    <dependency>
        <groupId>io.nebula</groupId>
        <artifactId>nebula-rpc-core</artifactId>
        <version>2.0.0-SNAPSHOT</version>
    </dependency>

    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-web</artifactId>
        <version>6.1.0</version>
        <scope>provided</scope>
    </dependency>

    <dependency>
        <groupId>jakarta.validation</groupId>
        <artifactId>jakarta.validation-api</artifactId>
        <version>3.0.2</version>
    </dependency>

    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <version>1.18.30</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

**迁移后**：
```xml
<properties>
    <nebula.version>2.0.0-SNAPSHOT</nebula.version>
</properties>

<dependencies>
    <dependency>
        <groupId>io.nebula</groupId>
        <artifactId>nebula-starter-api</artifactId>
        <version>${nebula.version}</version>
    </dependency>
</dependencies>
```

**收益**：
- ✅ 依赖从 4 个减少到 1 个
- ✅ 无需手动管理各依赖的版本号
- ✅ 自动获得 Nebula 框架推荐的依赖版本
- ✅ 后续升级只需修改 `nebula.version`

## 示例项目

- `nebula-example-user-api` - 用户服务 API 契约
- `nebula-example-order-api` - 订单服务 API 契约

## 注意事项

### 1. 不要包含实现代码
API 契约模块应该**只包含接口定义**，不要包含：
- ❌ Service 实现类
- ❌ Repository/Mapper
- ❌ Controller
- ❌ Configuration
- ✅ 只有接口、DTO、Entity、VO

### 2. 避免复杂依赖
API 模块应该保持轻量，不要引入：
- ❌ 数据库驱动（JDBC、MySQL Connector）
- ❌ 缓存实现（Redis、Caffeine）
- ❌ 消息队列（RabbitMQ、Kafka）
- ✅ 只依赖基础注解和数据模型

### 3. 使用 Parent POM（可选）
如果你有多个 API 契约模块，可以创建一个 Parent POM：

```xml
<parent>
    <groupId>com.example</groupId>
    <artifactId>api-parent</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</parent>
```

在 Parent POM 中统一管理 `nebula.version`。

## 相关文档

- [Nebula RPC 核心模块](../../infrastructure/rpc/nebula-rpc-core/README.md)
- [RPC 使用指南](../../docs/Nebula框架使用指南.md#rpc-模块)
- [其他 Starter 说明](../README.md)

