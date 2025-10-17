# Nebula RPC 优化5: @RpcCall 注解简化

## 需求背景

当前 RPC 客户端接口的每个方法都需要手动添加 `@RpcCall` 注解并指定路径和 HTTP 方法：

```java
@RpcClient
public interface AuthRpcClient {
    @RpcCall(value = "/rpc/auth", method = "POST")
    AuthDto.Response auth(@RequestBody AuthDto.Request request);
}
```

这种写法存在以下问题：
1. **重复性高**：每个方法都需要相同的配置
2. **易出错**：路径和方法名需要手动对应
3. **维护困难**：方法名变更需要同步修改路径

## 期望效果

通过约定优于配置的方式，简化为：

```java
@RpcClient
public interface AuthRpcClient {
    AuthDto.Response auth(AuthDto.Request request);
}
```

## 约定规则

### 约定1: 方法自动识别为 RPC 调用
- 所有 `@RpcClient` 接口中的方法自动视为 RPC 调用
- 无需手动添加 `@RpcCall` 注解

### 约定2: 路径自动推导
- 规则：`/rpc/{methodName}`
- 例如：
  - `auth()` -> `/rpc/auth`
  - `getUserById()` -> `/rpc/getUserById`
  - `createOrder()` -> `/rpc/createOrder`

### 约定3: 默认 POST 方法
- 所有 RPC 调用默认使用 POST 方法
- POST 方法适合大多数 RPC 场景（传输请求体）

### 约定4: 参数自动识别为请求体
- 无需 `@RequestBody` 注解
- 第一个参数自动作为请求体

## 设计方案

### 方案A: 在运行时自动应用（推荐）

**实现位置：** `RpcClientFactoryBean` 和相关调用处理逻辑

**核心思路：**
1. 在创建 RPC 客户端代理时
2. 检查方法是否有 `@RpcCall` 注解
3. 如果没有，自动应用约定规则：
   - path = `/rpc/{methodName}`
   - method = `POST`
   - 第一个参数作为请求体

**优势：**
- 不需要修改注解定义
- 对现有代码完全兼容
- 实现简单，逻辑集中

**劣势：**
- 运行时开销（很小，一次性）

### 方案B: 编译时字节码增强

**实现位置：** 使用 APT (Annotation Processing Tool)

**核心思路：**
- 编译时自动为方法添加 `@RpcCall` 注解

**优势：**
- 零运行时开销

**劣势：**
- 实现复杂
- 需要额外的编译工具

### 推荐方案：方案A（运行时自动应用）

## 技术实现

### 1. 修改 RpcClientFactoryBean

当前逻辑（简化）：
```java
// 在 RpcInvocationHandler.invoke() 中
RpcCall rpcCall = method.getAnnotation(RpcCall.class);
String path = rpcCall.value();
String httpMethod = rpcCall.method();
```

优化后逻辑：
```java
// 在 RpcInvocationHandler.invoke() 中
RpcCall rpcCall = method.getAnnotation(RpcCall.class);

String path;
String httpMethod;
if (rpcCall != null) {
    // 显式指定，优先使用
    path = rpcCall.value();
    httpMethod = rpcCall.method();
} else {
    // 自动推导（约定）
    path = "/rpc/" + method.getName();
    httpMethod = "POST";
}
```

### 2. 参数处理优化

当前逻辑：
- 需要 `@RequestBody` 注解标识哪个参数是请求体

优化后逻辑：
- 如果没有 `@RequestBody` 注解，第一个参数自动作为请求体
- 保留 `@PathVariable` 和 `@RequestParam` 的支持（如果需要）

### 3. 兼容性保证

**显式配置优先：**
```java
// 场景1：使用约定（推荐）
AuthDto.Response auth(AuthDto.Request request);
// 自动推导：POST /rpc/auth

// 场景2：显式指定（向后兼容）
@RpcCall(value = "/custom/path", method = "GET")
AuthDto.Response customMethod(AuthDto.Request request);
// 使用显式配置：GET /custom/path

// 场景3：部分指定
@RpcCall(value = "/custom/path")  // method 使用默认值 POST
AuthDto.Response anotherMethod(AuthDto.Request request);
// 使用：POST /custom/path
```

## 实施步骤

### 步骤1: 分析现有代码

查看以下文件，了解 RPC 调用的处理逻辑：
- `RpcClientFactoryBean` - 客户端代理创建
- `RpcInvocationHandler` - RPC 调用拦截
- HTTP/gRPC 客户端的实际调用实现

### 步骤2: 修改核心逻辑

**文件：** `RpcClientFactoryBean.java`

添加方法：
```java
/**
 * 获取 RPC 调用路径
 * 如果方法有 @RpcCall 注解且指定了路径，使用显式路径
 * 否则使用约定路径：/rpc/{methodName}
 */
private String getRpcPath(Method method) {
    RpcCall rpcCall = method.getAnnotation(RpcCall.class);
    if (rpcCall != null && StringUtils.hasText(rpcCall.value())) {
        return rpcCall.value();
    }
    // 约定路径
    return "/rpc/" + method.getName();
}

/**
 * 获取 HTTP 方法
 * 如果方法有 @RpcCall 注解且指定了 HTTP 方法，使用显式方法
 * 否则使用约定方法：POST
 */
private String getHttpMethod(Method method) {
    RpcCall rpcCall = method.getAnnotation(RpcCall.class);
    if (rpcCall != null && StringUtils.hasText(rpcCall.method())) {
        return rpcCall.method();
    }
    // 约定方法
    return "POST";
}
```

### 步骤3: 更新示例代码

简化 `AuthRpcClient` 和 `UserRpcClient` 接口：

**之前：**
```java
@RpcClient
public interface AuthRpcClient {
    @RpcCall(value = "/rpc/auth", method = "POST")
    AuthDto.Response auth(@RequestBody AuthDto.Request request);
}
```

**之后：**
```java
@RpcClient
public interface AuthRpcClient {
    AuthDto.Response auth(AuthDto.Request request);
}
```

### 步骤4: 测试验证

1. 启动服务
2. 验证 RPC 调用路径自动推导
3. 验证显式配置仍然有效
4. 验证参数自动识别为请求体

## 验证方法

### 日志验证
```
INFO - RPC调用: method=auth, path=/rpc/auth (auto-derived), httpMethod=POST (default)
INFO - RPC调用成功: auth
```

### 功能测试
```bash
# 测试自动推导
curl -X POST http://localhost:9090/rpc/auth \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"123456"}'
```

## 优势

### 1. 代码更简洁
- 减少注解配置 ~80%
- 接口定义更清晰

### 2. 降低出错概率
- 路径自动生成，不会拼写错误
- 方法名变更自动同步

### 3. 符合约定
- 遵循 RESTful 约定
- 符合 Spring Boot 风格

### 4. 灵活性
- 保留显式配置选项
- 支持特殊场景定制

## 潜在问题和解决方案

### 问题1: 方法名重载
```java
AuthDto.Response auth(AuthDto.Request request);
AuthDto.Response auth(String username, String password);
```

**解决方案：**
- Java 接口不允许同名方法（不同参数）作为 RPC 方法
- 或者要求显式指定不同的路径

### 问题2: 路径冲突
```java
// AuthRpcClient
void auth();

// UserRpcClient  
void auth();
```

**解决方案：**
- RPC 服务名包含接口全限定名，不会冲突
- 完整路径：`{serviceName}/rpc/{methodName}`

### 问题3: RESTful 风格的方法
```java
// GET 请求
User getUserById(Long id);

// DELETE 请求
void deleteUser(Long id);
```

**解决方案：**
- 如果需要 GET/DELETE，手动添加 `@RpcCall` 注解
- 或者通过方法名前缀约定（get* -> GET, delete* -> DELETE）

## 向后兼容

✅ **完全兼容**：
- 所有显式指定的 `@RpcCall` 注解仍然有效
- 只在没有注解时应用约定
- 现有代码无需修改

✅ **渐进式迁移**：
- 新代码可以使用简化写法
- 旧代码保持不变
- 逐步迁移

## 实施优先级

**建议优先级：高**

理由：
1. 实现简单，改动小
2. 收益明显，大幅简化代码
3. 不影响现有功能
4. 符合框架整体优化方向

## 总结

这个优化将进一步简化 Nebula RPC 的使用方式，使开发者只需关注业务逻辑，而不是繁琐的配置。通过合理的约定，在保持灵活性的同时，提供了更好的开发体验。

---

**设计日期：** 2025-01-16  
**设计版本：** Nebula 2.0.0  
**相关优化：** 优化1-4

