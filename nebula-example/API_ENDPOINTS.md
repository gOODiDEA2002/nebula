# Nebula Example - API端点汇总

## 应用信息
- **应用名称**: nebula-example  
- **版本**: 2.0.0-SNAPSHOT
- **框架**: Spring Boot 3.2.0 + Nebula Framework
- **数据库**: MyBatis-Plus 3.5.9 + MySQL

## 核心功能端点

### 🏥 健康检查
```
GET /api/health
返回: {"status":"UP","timestamp":"...","message":"Nebula Example Application is running"}
```

### ℹ️ 应用信息
```
GET /api/info  
返回: {"application":"nebula-example","version":"2.0.0-SNAPSHOT","framework":"Spring Boot 3.2.0","mybatis-plus":"3.5.9"}
```

### 👋 Hello World
```
GET /api/hello
返回: {"framework":"Nebula 2.0.0-SNAPSHOT","message":"Hello from Nebula Framework!","timestamp":"...","status":"success"}
```

## Nebula Web统一响应格式测试

### ✅ 成功响应
```
GET /api/test/success
返回: {"success":true,"code":"SUCCESS","message":"操作成功","data":{...},"timestamp":"..."}
```

### ❌ 业务错误响应  
```
GET /api/test/error
返回: {"success":false,"code":"BUSINESS_ERROR","message":"这是一个测试错误消息","timestamp":"..."}
```

### 🔢 自定义错误码响应
```
GET /api/test/error-with-code
返回: {"success":false,"code":"TEST_ERROR","message":"测试错误，错误码：TEST_ERROR","timestamp":"..."}
```

### 👤 模拟用户数据
```
GET /api/test/user/{id}
- 正常用户: {"success":true,"code":"SUCCESS","data":{"id":123,"username":"test_user_123",...}}
- 用户不存在(id=404): {"success":false,"code":"USER_NOT_FOUND","message":"用户不存在，ID: 404"}
```

## RPC服务端点

### 🔍 RPC健康检查
```
GET /rpc/user/health
返回: {"service":"UserRpcService","status":"UP","message":"用户RPC服务运行正常"}
```

### 👥 RPC用户服务
```
GET /rpc/user/all           # 获取所有用户
GET /rpc/user/{id}          # 根据ID获取用户  
GET /rpc/user/username/{username}  # 根据用户名获取用户
POST /rpc/user              # 创建用户
PUT /rpc/user/{id}          # 更新用户
DELETE /rpc/user/{id}       # 删除用户
POST /rpc/user/validate     # 验证用户凭据
```

## 用户REST API
```
GET /api/users              # 获取所有用户 (使用Nebula Web统一响应格式)
GET /api/users/{id}         # 根据ID获取用户
GET /api/users/username/{username}  # 根据用户名获取用户
POST /api/users             # 创建用户
PUT /api/users/{id}         # 更新用户
DELETE /api/users/{id}      # 删除用户
POST /api/users/validate    # 验证用户凭据
GET /api/users/page         # 分页查询用户
```

## 技术特性

### ✨ Nebula Web特性
- 🎯 **统一响应格式**: 所有API自动包装为标准Result格式
- ⚡ **错误处理**: 统一的异常处理和错误码管理
- 🛡️ **参数验证**: 集成的参数验证和错误反馈
- 📄 **分页支持**: 标准的分页查询响应格式

### 🔧 集成模块
- ✅ **nebula-data-persistence**: MyBatis-Plus数据持久层
- ✅ **nebula-web**: 统一Web响应和控制器基类
- ✅ **nebula-starter**: 自动配置和启动器

### 🗄️ 数据库配置
- **类型**: MySQL 8.0
- **地址**: 192.168.2.130:3306
- **数据库**: nebula_example
- **连接池**: Druid
- **ORM**: MyBatis-Plus 3.5.12

## 启动方式
```bash
cd nebula-example
mvn spring-boot:run
```

应用将在 http://localhost:8080 启动
