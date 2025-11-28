# Nebula Gateway Core - 配置参考

> 本文档提供 Nebula Gateway Core 的完整配置说明和示例。

## 配置概览

本模块支持通过 YAML 配置文件进行配置，所有配置项都在 `nebula.gateway` 命名空间下。

**配置优先级**（从高到低）：
1. 命令行参数（`--nebula.gateway.xxx=value`）
2. 环境变量（`NEBULA_GATEWAY_XXX=value`）
3. 应用配置文件（`application.yml`）
4. Spring Profile配置（`application-{profile}.yml`）
5. 框架默认配置

## 快速配置

### 最小配置

最简单的配置，使用默认值：

```yaml
nebula:
  gateway:
    enabled: true
    jwt:
      secret: your-jwt-secret-key-at-least-32-characters
```

### 推荐配置

适合大多数场景的配置：

```yaml
nebula:
  gateway:
    enabled: true
    jwt:
      enabled: true
      secret: ${JWT_SECRET:your-jwt-secret-key-at-least-32-characters}
      whitelist:
        - /api/v1/public/**
        - /api/v1/users/login
        - /api/v1/users/register
    logging:
      enabled: true
      slow-request-threshold: 3000
    rate-limit:
      enabled: true
      strategy: ip
```

### 完整配置

包含所有配置项的完整示例：

```yaml
nebula:
  gateway:
    # 基础配置
    enabled: true
    
    # JWT认证配置
    jwt:
      enabled: true
      secret: ${JWT_SECRET:your-jwt-secret-key-at-least-32-characters}
      header: Authorization
      prefix: "Bearer "
      user-id-header: X-User-Id
      username-header: X-Username
      whitelist:
        - /api/v1/public/**
        - /api/v1/users/login
        - /api/v1/users/register
        - /api/v1/health
        - /actuator/**
      claim-headers:
        - phone:X-User-Phone
        - role:X-User-Role
    
    # 日志配置
    logging:
      enabled: true
      request-id-header: X-Request-Id
      log-request-body: false
      log-response-body: false
      slow-request-threshold: 3000
    
    # 限流配置
    rate-limit:
      enabled: true
      strategy: ip  # ip, user, path
```

---

## 配置项详解

### 1. 基础配置

#### 1.1 启用配置

| 配置项 | 类型 | 默认值 | 必填 | 说明 |
|--------|------|--------|------|------|
| `nebula.gateway.enabled` | `Boolean` | `true` | 否 | 是否启用网关模块 |

**说明**：

控制网关模块是否启用。设置为 `false` 时，所有网关过滤器将不生效。

**示例**：

```yaml
nebula:
  gateway:
    enabled: true
```

**环境变量**：

```bash
export NEBULA_GATEWAY_ENABLED=true
```

---

### 2. JWT认证配置

#### 2.1 基础JWT配置

| 配置项 | 类型 | 默认值 | 必填 | 说明 |
|--------|------|--------|------|------|
| `nebula.gateway.jwt.enabled` | `Boolean` | `true` | 否 | 是否启用JWT认证 |
| `nebula.gateway.jwt.secret` | `String` | - | 是 | JWT密钥（至少32字符） |
| `nebula.gateway.jwt.header` | `String` | `Authorization` | 否 | JWT请求头名称 |
| `nebula.gateway.jwt.prefix` | `String` | `Bearer ` | 否 | Token前缀 |

**secret 详解**：

- **作用**：用于验证JWT Token签名的密钥
- **取值范围**：字符串，长度至少32字符
- **注意事项**：
  - 生产环境必须使用环境变量配置
  - 不要在代码中硬编码密钥
  - 建议使用随机生成的强密钥
- **示例**：
  ```yaml
  nebula:
    gateway:
      jwt:
        secret: ${JWT_SECRET}  # 从环境变量读取
  ```

#### 2.2 白名单配置

| 配置项 | 类型 | 默认值 | 必填 | 说明 |
|--------|------|--------|------|------|
| `nebula.gateway.jwt.whitelist` | `List<String>` | `[]` | 否 | 免认证路径列表 |

**whitelist 详解**：

- **作用**：配置不需要JWT认证的路径
- **格式**：支持Ant风格路径模式
- **示例**：
  ```yaml
  nebula:
    gateway:
      jwt:
        whitelist:
          - /api/v1/public/**     # 所有public路径
          - /api/v1/users/login   # 登录接口
          - /api/v1/users/register # 注册接口
          - /actuator/**          # 健康检查
  ```

#### 2.3 用户信息传递配置

| 配置项 | 类型 | 默认值 | 必填 | 说明 |
|--------|------|--------|------|------|
| `nebula.gateway.jwt.user-id-header` | `String` | `X-User-Id` | 否 | 用户ID请求头 |
| `nebula.gateway.jwt.username-header` | `String` | `X-Username` | 否 | 用户名请求头 |
| `nebula.gateway.jwt.claim-headers` | `List<String>` | `[]` | 否 | Claims映射到请求头 |

**claim-headers 详解**：

- **作用**：将JWT中的Claims自动映射为请求头，传递给下游服务
- **格式**：`claimName:headerName`
- **示例**：
  ```yaml
  nebula:
    gateway:
      jwt:
        claim-headers:
          - phone:X-User-Phone    # 将phone claim映射为X-User-Phone头
          - role:X-User-Role      # 将role claim映射为X-User-Role头
          - email:X-User-Email    # 将email claim映射为X-User-Email头
  ```

---

### 3. 日志配置

#### 3.1 日志基础配置

| 配置项 | 类型 | 默认值 | 必填 | 说明 |
|--------|------|--------|------|------|
| `nebula.gateway.logging.enabled` | `Boolean` | `true` | 否 | 是否启用请求日志 |
| `nebula.gateway.logging.request-id-header` | `String` | `X-Request-Id` | 否 | RequestId请求头 |
| `nebula.gateway.logging.slow-request-threshold` | `Long` | `3000` | 否 | 慢请求阈值(毫秒) |

**slow-request-threshold 详解**：

- **作用**：定义慢请求的阈值，超过此时间的请求会在日志中标记为[SLOW]
- **取值范围**：大于0的整数，单位毫秒
- **推荐值**：3000（3秒）
- **示例**：
  ```yaml
  nebula:
    gateway:
      logging:
        slow-request-threshold: 5000  # 5秒
  ```

#### 3.2 日志内容配置

| 配置项 | 类型 | 默认值 | 必填 | 说明 |
|--------|------|--------|------|------|
| `nebula.gateway.logging.log-request-body` | `Boolean` | `false` | 否 | 是否记录请求体 |
| `nebula.gateway.logging.log-response-body` | `Boolean` | `false` | 否 | 是否记录响应体 |

**注意事项**：

- 开启请求体/响应体日志会影响性能
- 生产环境建议关闭
- 仅在调试时临时开启

---

### 4. 限流配置

#### 4.1 限流基础配置

| 配置项 | 类型 | 默认值 | 必填 | 说明 |
|--------|------|--------|------|------|
| `nebula.gateway.rate-limit.enabled` | `Boolean` | `true` | 否 | 是否启用限流 |
| `nebula.gateway.rate-limit.strategy` | `String` | `ip` | 否 | 限流策略 |

**strategy 详解**：

- **作用**：定义限流的维度
- **可选值**：
  - `ip`：基于客户端IP限流（默认）
  - `user`：基于用户ID限流（需要JWT认证）
  - `path`：基于请求路径限流
- **示例**：
  ```yaml
  nebula:
    gateway:
      rate-limit:
        enabled: true
        strategy: user  # 基于用户限流
  ```

---

## 配置示例

### 示例1：开发环境配置

**application-dev.yml**：

```yaml
spring:
  profiles:
    active: dev

nebula:
  gateway:
    enabled: true
    jwt:
      enabled: true
      secret: dev-jwt-secret-key-at-least-32-characters-long
      whitelist:
        - /api/**  # 开发环境放开所有接口
    logging:
      enabled: true
      slow-request-threshold: 1000
      log-request-body: true   # 开发环境开启请求体日志
      log-response-body: true  # 开发环境开启响应体日志

logging:
  level:
    io.nebula.gateway: DEBUG
```

### 示例2：生产环境配置

**application-prod.yml**：

```yaml
spring:
  profiles:
    active: prod

nebula:
  gateway:
    enabled: true
    jwt:
      enabled: true
      secret: ${JWT_SECRET}  # 从环境变量读取
      whitelist:
        - /api/v1/users/login
        - /api/v1/users/register
        - /api/v1/public/**
        - /actuator/health
      claim-headers:
        - phone:X-User-Phone
        - role:X-User-Role
    logging:
      enabled: true
      slow-request-threshold: 3000
      log-request-body: false
      log-response-body: false
    rate-limit:
      enabled: true
      strategy: user

logging:
  level:
    io.nebula.gateway: WARN
```

### 示例3：Docker环境配置

**docker-compose.yml**：

```yaml
version: '3.8'
services:
  gateway:
    image: your-gateway:latest
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - NEBULA_GATEWAY_ENABLED=true
      - NEBULA_GATEWAY_JWT_SECRET=your-production-secret-key-at-least-32-chars
      - NEBULA_GATEWAY_JWT_WHITELIST=/api/v1/users/login,/api/v1/users/register
    ports:
      - "8080:8080"
```

---

## 配置验证

### 启动时验证

模块启动时会自动验证配置的合法性。

**验证规则**：

1. JWT密钥不能为空
2. JWT密钥长度至少32字符
3. 慢请求阈值必须大于0

**验证失败示例**：

```
Caused by: IllegalArgumentException: 
  Property 'nebula.gateway.jwt.secret' must be at least 32 characters
```

---

## 相关文档

- [README.md](./README.md) - 模块介绍
- [EXAMPLE.md](./EXAMPLE.md) - 使用示例
- [TESTING.md](./TESTING.md) - 测试指南
- [ROADMAP.md](./ROADMAP.md) - 未来规划

---

> 如有配置相关问题，请查阅文档或提Issue。

