# OAuth Client Backend

Vocoor OAuth 2.0 客户端示例 - 后端服务

## 项目简介

这是一个基于 Nebula 框架的 OAuth 客户端示例项目，演示如何接入 Vocoor OAuth 2.0 服务实现第三方登录。

## 技术栈

- Java 21
- Spring Boot 3.x
- Nebula Framework 2.0.1
- MyBatis-Plus
- MySQL 8.x

## 快速开始

### 1. 环境准备

- JDK 21+
- Maven 3.6+
- MySQL 8.x

### 2. 数据库初始化

```bash
# 创建数据库
mysql -u root -p -e "CREATE DATABASE oauth_client_demo DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 执行初始化脚本
mysql -u root -p oauth_client_demo < sql/init.sql
```

### 3. 配置修改

编辑 `src/main/resources/application.yml`：

```yaml
# 数据库配置
spring:
  datasource:
    url: jdbc:mysql://your-mysql-host:3306/oauth_client_demo
    username: your-username
    password: your-password

# Vocoor OAuth 配置
vocoor:
  oauth:
    server-url: http://localhost:8080  # Vocoor OAuth 服务器地址
    client-id: your_client_id          # 客户端ID
    client-secret: your_client_secret  # 客户端密钥
    redirect-uri: http://localhost:8081/api/oauth/callback
    frontend-url: http://localhost:5173
```

### 4. 构建运行

```bash
# 安装依赖
cd /path/to/nebula-projects/nebula
mvn install -DskipTests

# 运行应用
cd /path/to/nebula-projects/example/oauth-client/backend
mvn spring-boot:run
```

应用启动后访问：http://localhost:8081

## API 接口

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 获取授权URL | GET | /api/oauth/authorize | 返回 Vocoor 授权 URL |
| 发起登录 | GET | /api/oauth/login | 直接重定向到 Vocoor 登录页 |
| 授权回调 | GET | /api/oauth/callback | 处理 Vocoor 授权回调 |
| 获取当前用户 | GET | /api/oauth/user/current | 获取当前登录用户信息 |
| 退出登录 | POST | /api/oauth/logout | 退出登录 |
| 健康检查 | GET | /api/oauth/health | 服务健康检查 |

## 目录结构

```
src/main/java/io/nebula/examples/oauth/
├── OAuthClientApplication.java    # 应用入口
├── config/                        # 配置类
│   ├── OAuthClientConfig.java    # OAuth 客户端配置
│   └── WebConfig.java            # Web 配置
├── controller/                    # REST 控制器
│   └── OAuthController.java      # OAuth 接口
├── entity/                        # 实体类
│   ├── dos/                      # 数据对象
│   ├── dto/                      # 传输对象
│   └── vo/                       # 视图对象
├── mapper/                        # MyBatis Mapper
├── service/                       # 业务逻辑
│   └── impl/
└── util/                          # 工具类
    └── JwtUtil.java              # JWT 工具
```

## 授权流程

1. 前端调用 `GET /api/oauth/authorize` 获取授权 URL
2. 前端重定向到授权 URL（Vocoor 登录页）
3. 用户在 Vocoor 完成登录授权
4. Vocoor 回调到 `GET /api/oauth/callback`
5. 后端处理授权码，获取用户信息，创建/绑定本地用户
6. 后端重定向到前端，携带本地 JWT Token
7. 前端存储 Token，完成登录

## 注意事项

1. **client_secret 保密**：绝不在前端暴露
2. **redirect_uri 一致**：必须与 Vocoor 平台配置完全一致
3. **HTTPS**：生产环境必须使用 HTTPS
4. **state 验证**：防止 CSRF 攻击

## 相关链接

- [Vocoor OAuth 接入指南](../../vocoor-service/docs/vocoor_oauth_integration_guide.md)
- [Nebula 框架文档](../../nebula/docs/Nebula框架使用指南.md)


