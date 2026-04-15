# Nebula Starter All Example

> 使用 `nebula-starter-all` 的全功能单体应用示例

## 功能特性

- 基于 `nebula-starter-all`，包含框架几乎所有模块
- 通过配置按需启用/禁用各模块
- 适用于单体架构或快速原型验证
- 默认禁用需要外部服务的模块，可零依赖启动

## 包含模块

| 模块 | 默认状态 | 说明 |
|------|----------|------|
| nebula-web | 启用 | Web 框架 |
| nebula-data-persistence | 禁用 | MyBatis-Plus 持久化 |
| nebula-data-cache | 禁用 | 多级缓存 |
| nebula-rpc-http | 禁用 | HTTP RPC |
| nebula-rpc-grpc | 禁用 | gRPC |
| nebula-discovery-nacos | 禁用 | Nacos 服务发现 |
| nebula-messaging-rabbitmq | 禁用 | RabbitMQ 消息队列 |
| nebula-lock-redis | 启用 | 分布式锁 |
| nebula-storage-minio | 禁用 | MinIO 对象存储 |
| nebula-ai-spring | 禁用 | Spring AI 集成 |

## 项目结构

```
starter-all-example/
├── pom.xml
└── src/main/
    ├── java/io/nebula/examples/all/
    │   ├── AllApplication.java            # 启动类
    │   └── controller/
    │       └── HelloController.java       # 示例控制器
    └── resources/
        └── application.yml                # 应用配置
```

## 前置条件

- JDK 21+
- Maven 3.8+
- 无外部依赖（默认配置）
- 按需启用模块时需对应的外部服务

## 快速开始

```bash
# 1. 安装框架到本地仓库（首次需要）
cd /path/to/nebula
mvn install -DskipTests

# 2. 启动应用（端口 8084）
mvn -q -f examples/starter-all-example spring-boot:run
```

## 接口测试

```bash
# Hello 接口
curl http://localhost:8084/hello
# 响应: {"code":200,"message":"success","data":"Hello, Nebula All",...}

# 健康检查
curl http://localhost:8084/health/ping
```

## 配置说明

```yaml
server:
  port: 8084

nebula:
  # 以下模块默认禁用，按需开启
  discovery:
    nacos:
      enabled: false
  rpc:
    http:
      enabled: false
    grpc:
      enabled: false
  messaging:
    rabbitmq:
      enabled: false
  data:
    persistence:
      enabled: false
    cache:
      enabled: false
  storage:
    minio:
      enabled: false
  lock:
    enabled: true
    enable-aspect: true
```

## 按需启用示例

启用数据库和缓存：

```bash
mvn -q -f examples/starter-all-example spring-boot:run \
  -Dspring-boot.run.arguments="\
    --nebula.data.persistence.enabled=true \
    --nebula.data.cache.enabled=true \
    --spring.datasource.url=jdbc:mysql://localhost:3306/demo \
    --spring.datasource.username=root \
    --spring.datasource.password=root"
```

## 相关文档

- [Nebula Examples 总览](../README.md)
- [nebula-starter-all](../../starter/nebula-starter-all/pom.xml)
- [全功能综合示例（fullstack-example）](../fullstack-example/README.md)
