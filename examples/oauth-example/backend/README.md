# OAuth Client Backend

Vocoor OAuth 2.0 客户端示例 - 后端服务

## 项目简介

这是一个基于 Nebula 框架的 OAuth 客户端示例项目，演示如何接入 Vocoor OAuth 2.0 服务实现第三方登录。

## 技术栈

- Java 21
- Spring Boot 4.1
- Nebula Framework 2.1.0
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

### 3. 配置环境变量

```bash
export OAUTH_DB_URL='jdbc:mysql://localhost:13306/oauth_client_demo'
export OAUTH_DB_USERNAME=root
export OAUTH_DB_PASSWORD=your-password
export OAUTH_SERVER_URL=http://localhost:8080
export OAUTH_CLIENT_ID=your-client-id
export OAUTH_CLIENT_SECRET=your-client-secret
export OAUTH_REDIRECT_URI=http://localhost:8081/api/oauth/callback
export OAUTH_FRONTEND_URL=http://localhost:4010
export OAUTH_JWT_SECRET=replace-with-at-least-32-characters
```

### 4. 构建运行

```bash
# 安装依赖
cd /path/to/nebula-projects/nebula
mvn install -DskipTests

# 运行应用
mvn -f examples/oauth-example/backend spring-boot:run
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
| 健康检查 | GET | /health/ping | 服务健康检查 |

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

- Vocoor OAuth 接入说明由对应服务仓库维护
- [Nebula 文档索引](../../../docs/INDEX.md)
