# Nebula Starter Web Example

> 使用 `nebula-starter-web` 的最简 Web 应用示例

## 功能特性

- 基于 `nebula-starter-web`，开箱即用
- 统一响应格式（`Result<T>`）
- 内置健康检查、性能监控端点
- JWT 安全认证（已配置开发密钥）

## 项目结构

```
starter-web-example/
├── pom.xml
└── src/main/
    ├── java/io/nebula/examples/web/
    │   ├── WebApplication.java         # 启动类
    │   └── controller/
    │       └── HelloController.java    # 示例控制器
    └── resources/
        └── application.yml             # 应用配置
```

## 前置条件

- JDK 21+
- Maven 3.8+
- 无外部依赖

## 快速开始

```bash
# 1. 安装框架到本地仓库（首次需要）
cd /path/to/nebula
mvn install -DskipTests

# 2. 启动应用（端口 8080）
mvn -q -f examples/starter-web-example spring-boot:run
```

## 接口测试

```bash
# Hello 接口
curl http://localhost:8080/hello
# 响应: {"code":200,"message":"success","data":"Hello, Nebula Web","timestamp":...}

# 健康检查
curl http://localhost:8080/health/ping
```

## 配置说明

```yaml
server:
  port: 8080

nebula:
  web:
    performance:
      enabled: true       # 启用性能监控
  security:
    jwt:
      secret: ${JWT_SECRET:nebula-starter-web-example-dev-secret-key-at-least-32-bytes}
      expiration: 86400   # Token 过期时间（秒）
```

## 核心代码

```java
@RestController
public class HelloController {
    @GetMapping("/hello")
    public Result<String> hello() {
        return Result.success("Hello, Nebula Web");
    }
}
```

## 相关文档

- [Nebula Examples 总览](../README.md)
- [nebula-starter-web](../../starter/nebula-starter-web/pom.xml)
- [Nebula 框架使用指南](../../docs/Nebula框架使用指南.md)
