# Nebula 快速开始

本文提供 Nebula `2.1.0-SNAPSHOT` 的最短可运行路径。完整示例位于仓库的
[`examples/`](../../examples/README.md)。

## 环境要求

- JDK 21
- Maven 3.6.3 或更高版本
- 需要使用的外部服务，例如 MySQL、Redis、Nacos 或 RabbitMQ

## 添加 Starter

普通 REST 服务使用 `nebula-starter-web`：

```xml
<properties>
    <java.version>21</java.version>
    <nebula.version>2.1.0-SNAPSHOT</nebula.version>
</properties>

<dependencies>
    <dependency>
        <groupId>com.nebula-projects</groupId>
        <artifactId>nebula-starter-web</artifactId>
        <version>${nebula.version}</version>
    </dependency>
</dependencies>
```

微服务、网关、AI、MCP 和任务应用的入口依赖见
[Starter 选择指南](../Nebula%20Starter%20选择指南.md)。

## 创建应用

```java
package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
```

```java
package com.example.demo;

import io.nebula.core.common.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/hello")
    public Result<String> hello() {
        return Result.success("Hello, Nebula");
    }
}
```

## 最小配置

```yaml
spring:
  application:
    name: demo-service

nebula:
  security:
    jwt:
      secret: ${JWT_SECRET}
```

`JWT_SECRET` 必须显式提供，长度不少于 32 个字符。生产配置不得在仓库中保存真实密钥。

Starter 会通过 `META-INF/nebula-defaults.properties` 注入最低优先级的模块开关。
应用的 `application.yml`、环境变量和命令行参数始终可以覆盖这些默认值。

## 运行

```bash
mvn spring-boot:run
curl http://localhost:8080/hello
```

首次在本仓库运行示例前，先安装框架快照：

```bash
mvn clean install -DskipTests
mvn -q -f examples/starter-web-example spring-boot:run
```

可验证的默认端点：

```text
GET /hello
GET /health/ping
GET /health/liveness
GET /health/readiness
GET /v3/api-docs
GET /swagger-ui.html
```

性能端点只有在 `nebula.web.performance.enabled=true` 时启用。

## 启用外部组件

外部基础设施默认由 Starter 或显式配置开启。直接引入模块时，需要设置对应开关：

```yaml
nebula:
  discovery:
    nacos:
      enabled: true
      server-addr: localhost:8848
  rpc:
    grpc:
      enabled: true
      client:
        auth-token: ${GRPC_AUTH_TOKEN:}
        request-timeout: 5000
  search:
    elasticsearch:
      enabled: true
      uris:
        - http://localhost:9200
```

所有配置前缀和安全限制见[配置说明](../Nebula框架配置说明.md)。

## 常用验证命令

```bash
# 整仓干净编译
mvn clean compile

# 整仓测试
mvn test

# 单模块及其上游依赖
mvn test -pl application/nebula-web -am

# 单个测试类
mvn test -pl application/nebula-web -Dtest=SpringdocCompatibilityTest
```

涉及升级或依赖收敛时必须使用 `clean`，避免旧 `target/classes` 掩盖真实结果。
