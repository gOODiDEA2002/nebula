# Nebula Starter Minimal Example

> 使用 `nebula-starter-minimal` 的最小化应用示例（非 Web 场景）

## 功能特性

- 基于 `nebula-starter-minimal`，仅包含 `nebula-foundation` + `nebula-autoconfigure`
- 不包含 Web 服务器、RPC、消息队列、服务发现等组件
- 适用于命令行工具、批处理任务、后台 Worker 等非 Web 场景
- 启动后自动退出（无 Web 容器保持运行）

## 项目结构

```
starter-minimal-example/
├── pom.xml
└── src/main/
    ├── java/io/nebula/examples/minimal/
    │   └── MinimalApplication.java    # 启动类
    └── resources/
        └── application.yml            # 应用配置
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

# 2. 启动应用（启动后自动退出，无 Web 端点）
mvn -q -f examples/starter-minimal-example spring-boot:run
```

## 配置说明

```yaml
spring:
  application:
    name: starter-minimal-example

logging:
  level:
    io.nebula: INFO
```

## 适用场景

- 命令行工具或 CLI 程序
- 批处理 / 数据迁移任务
- 后台 Worker / 定时任务 Runner
- 仅需 Nebula Foundation 工具类的场景

## 扩展使用

可以通过实现 `CommandLineRunner` 或 `ApplicationRunner` 接口添加启动逻辑：

```java
@SpringBootApplication
public class MinimalApplication implements CommandLineRunner {
    public static void main(String[] args) {
        SpringApplication.run(MinimalApplication.class, args);
    }

    @Override
    public void run(String... args) {
        System.out.println("Nebula Minimal Application started.");
        // 在此添加业务逻辑
    }
}
```

## 相关文档

- [Nebula Examples 总览](../README.md)
- [nebula-starter-minimal](../../starter/nebula-starter-minimal/pom.xml)
