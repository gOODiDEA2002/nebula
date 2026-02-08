# Nebula Example - Web Application

> Nebula Framework Web 应用示例项目

## 功能特性

- Thymeleaf 模板引擎集成
- 静态资源自动处理
- RESTful API 支持
- Spring Boot Actuator 监控
- 统一异常处理

## 技术栈

- **nebula-starter-web** - Nebula Web Starter
- **Spring Boot Web** - Web 框架
- **Thymeleaf** - 模板引擎
- **Spring Boot Actuator** - 监控

## 快速开始

### 前置条件

- JDK 21+
- Maven 3.8+
- Nebula Framework 2.0.1-SNAPSHOT（需先安装到本地仓库）

### 启动步骤

```bash
# 1. 确保 Nebula Framework 已安装到本地仓库
cd /path/to/nebula
mvn install -DskipTests

# 2. 编译本示例
cd nebula-example/nebula-example-web
mvn clean package -DskipTests

# 3. 启动应用
java -jar target/nebula-example-web-1.0.0-SNAPSHOT.jar
```

### 验证

```bash
# 访问首页
curl http://localhost:8080/

# Hello API
curl http://localhost:8080/api/hello

# 系统信息
curl http://localhost:8080/api/info

# 健康检查
curl http://localhost:8080/actuator/health
```

## 页面列表

| 路径 | 描述 |
|------|------|
| / | 首页 |
| /about | 关于页面 |

## API 列表

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | /api/hello | Hello 接口 |
| GET | /api/info | 系统信息 |
| GET | /actuator/health | 健康检查 |
| GET | /actuator/info | 应用信息 |

## 项目结构

```
nebula-example-web/
├── src/
│   ├── main/
│   │   ├── java/io/nebula/example/web/
│   │   │   ├── WebExampleApplication.java    # 启动类
│   │   │   ├── controller/
│   │   │   │   └── HomeController.java       # 首页控制器
│   │   │   └── config/
│   │   │       └── WebConfig.java            # Web 配置
│   │   └── resources/
│   │       ├── application.yml               # 配置文件
│   │       ├── templates/                    # Thymeleaf 模板
│   │       │   ├── index.html
│   │       │   └── about.html
│   │       └── static/                       # 静态资源
│   │           ├── css/style.css
│   │           └── js/main.js
│   └── test/
│       └── java/io/nebula/example/web/
├── pom.xml
└── README.md
```

## 配置说明

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| server.port | 8080 | 服务端口 |
| spring.thymeleaf.cache | false | 模板缓存（开发时禁用） |

## 配置复杂度

| 指标 | 值 |
|------|-----|
| 配置文件行数 | ~55 行 |
| 必需配置项 | 2（port, name） |
| 无需外部服务 | 是 |

## 相关文档

- [Nebula Framework 文档](../../docs/README.md)
- [nebula-starter-web 文档](../../starter/nebula-starter-web/README.md)

---

**版本**: 1.0.0-SNAPSHOT
