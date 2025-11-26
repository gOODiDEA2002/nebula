# Nebula Foundation 发展路线图

> 模块未来优化和扩展规划

## 目录

- [版本历史](#版本历史)
- [当前版本](#当前版本)
- [近期计划](#近期计划)
- [中期规划](#中期规划)
- [长期展望](#长期展望)
- [技术债务](#技术债务)
- [社区反馈](#社区反馈)

## 版本历史

### v1.0.0 (2024-Q1)

初始版本,提供核心基础功能:

- ✅ 统一结果封装 (Result)
- ✅ 异常处理体系 (NebulaException)
- ✅ ID生成器 (UUID, 雪花算法)
- ✅ JWT工具
- ✅ 基础加密工具
- ✅ JSON工具
- ✅ 日期时间工具

### v2.0.0 (2024-Q4)

重大升级:

- ✅ 升级到 Java 21
- ✅ 升级到 Spring Boot 3.x
- ✅ 优化异常处理机制
- ✅ 增强ID生成器功能
- ✅ 完善JWT工具类
- ✅ 添加更多加密算法

### v2.0.1-SNAPSHOT (当前)

当前开发版本:

- ✅ Bug 修复
- ✅ 文档完善
- ✅ 性能优化

## 当前版本

### v2.0.1 特性

**已完成**:

- [x] 完整的单元测试覆盖
- [x] 详细的使用文档
- [x] 配置示例和最佳实践
- [x] 票务系统场景示例

**进行中**:

- [ ] 性能基准测试
- [ ] 压力测试报告

## 近期计划

### v2.1.0 (2025-Q1) - 增强功能

**目标**: 基于实际使用反馈,增强现有功能

#### 1. ID生成器增强

**优先级**: 🔴 高

**需求来源**: 票务系统需要更灵活的ID生成策略

**计划功能**:

```java
// 支持自定义ID生成策略
public interface IdGenerationStrategy {
    String generate(Map<String, Object> context);
}

// 业务ID生成器注册机制
@Component
public class IdGeneratorRegistry {
    
    /**
     * 注册自定义ID生成策略
     */
    public void registerStrategy(String name, IdGenerationStrategy strategy) {
        // 实现
    }
    
    /**
     * 使用注册的策略生成ID
     */
    public String generateId(String strategyName, Map<String, Object> context) {
        // 实现
    }
}

// 使用示例
IdGeneratorRegistry registry = new IdGeneratorRegistry();
registry.registerStrategy("custom-order", ctx -> {
    String prefix = (String) ctx.get("prefix");
    String region = (String) ctx.get("region");
    return prefix + region + IdGenerator.snowflakeId();
});

String orderId = registry.generateId("custom-order", Map.of(
    "prefix", "O",
    "region", "BJ"
));
```

**技术方案**:

- 使用策略模式实现
- 支持Spring Bean注册
- 提供默认策略库

**预计工作量**: 3人天

#### 2. 加密工具增强

**优先级**: 🔴 高

**需求来源**: 支付系统需要更多加密算法

**计划功能**:

```java
/**
 * RSA非对称加密
 */
public class RsaUtils {
    
    /**
     * 生成RSA密钥对
     */
    public static KeyPair generateKeyPair(int keySize);
    
    /**
     * 公钥加密
     */
    public static String encryptWithPublicKey(String plainText, PublicKey publicKey);
    
    /**
     * 私钥解密
     */
    public static String decryptWithPrivateKey(String cipherText, PrivateKey privateKey);
    
    /**
     * 私钥签名
     */
    public static String sign(String data, PrivateKey privateKey);
    
    /**
     * 公钥验签
     */
    public static boolean verify(String data, String signature, PublicKey publicKey);
}

/**
 * SM4国密算法
 */
public class Sm4Utils {
    
    /**
     * SM4加密
     */
    public static String encrypt(String plainText, String key);
    
    /**
     * SM4解密
     */
    public static String decrypt(String cipherText, String key);
}
```

**技术方案**:

- 集成 Bouncy Castle 库
- 实现国密算法 (SM2, SM3, SM4)
- 提供密钥管理工具

**预计工作量**: 5人天

#### 3. JSON工具增强

**优先级**: 🟡 中

**需求来源**: 复杂数据结构序列化需求

**计划功能**:

```java
/**
 * JSON Schema 验证
 */
public class JsonSchemaValidator {
    
    /**
     * 验证JSON是否符合Schema
     */
    public static boolean validate(String json, String schema);
    
    /**
     * 获取验证错误详情
     */
    public static List<ValidationError> getValidationErrors(String json, String schema);
}

/**
 * JSON Path 查询
 */
public class JsonPathUtils {
    
    /**
     * 使用JSONPath查询
     */
    public static <T> T query(String json, String path, Class<T> type);
    
    /**
     * 使用JSONPath更新
     */
    public static String update(String json, String path, Object value);
}
```

**技术方案**:

- 集成 JSON Schema Validator
- 集成 JSONPath
- 保持API简洁

**预计工作量**: 3人天

#### 4. 分布式追踪支持

**优先级**: 🟡 中

**需求来源**: 微服务链路追踪需求

**计划功能**:

```java
/**
 * 分布式追踪上下文
 */
public class TraceContext {
    
    /**
     * 生成Trace ID
     */
    public static String generateTraceId();
    
    /**
     * 生成Span ID
     */
    public static String generateSpanId();
    
    /**
     * 设置Trace上下文
     */
    public static void setTraceContext(String traceId, String spanId, String parentSpanId);
    
    /**
     * 获取当前Trace ID
     */
    public static String getCurrentTraceId();
    
    /**
     * 清除上下文
     */
    public static void clear();
}

/**
 * Result 自动携带 Trace 信息
 */
Result<User> result = Result.success(user)
    .withTraceId(TraceContext.getCurrentTraceId())
    .withSpanId(TraceContext.getCurrentSpanId());
```

**技术方案**:

- 使用 ThreadLocal 存储上下文
- 集成 OpenTelemetry
- 自动注入到 Result

**预计工作量**: 5人天

### v2.2.0 (2025-Q2) - 性能优化

**目标**: 提升性能和并发能力

#### 1. ID生成器性能优化

**当前性能**:

- UUID: ~500万次/秒
- 雪花ID: ~200万次/秒

**目标性能**:

- 雪花ID: ~400万次/秒 (提升100%)

**优化方案**:

1. 优化时钟获取方式
2. 减少同步锁粒度
3. 使用 VarHandle 替代 volatile

**预计工作量**: 3人天

#### 2. JSON序列化性能优化

**当前性能**:

- 序列化: ~100MB/秒
- 反序列化: ~80MB/秒

**目标性能**:

- 序列化: ~200MB/秒 (提升100%)
- 反序列化: ~160MB/秒 (提升100%)

**优化方案**:

1. 启用Jackson Fast Double Parser
2. 优化对象复用
3. 预编译序列化器

**预计工作量**: 3人天

#### 3. 加密性能优化

**目标**: 提升加密解密速度

**优化方案**:

1. 使用硬件加速 (AES-NI)
2. 对象池化
3. 批量处理支持

**预计工作量**: 4人天

## 中期规划

### v3.0.0 (2025-Q3) - 重大升级

**目标**: 引入新特性,保持向后兼容

#### 1. 响应式支持

```java
/**
 * 响应式结果封装
 */
public class ReactiveResult<T> {
    
    public static <T> Mono<Result<T>> success(Mono<T> data) {
        return data.map(Result::success);
    }
    
    public static <T> Mono<Result<T>> error(String code, String message) {
        return Mono.just(Result.error(code, message));
    }
}

// 使用示例
@GetMapping("/api/users/{id}")
public Mono<Result<User>> getUser(@PathVariable String id) {
    return userService.findById(id)
        .map(Result::success)
        .defaultIfEmpty(Result.notFound("用户不存在"));
}
```

#### 2. 异步ID生成

```java
/**
 * 异步ID生成器
 */
public class AsyncIdGenerator {
    
    /**
     * 异步生成ID
     */
    public static CompletableFuture<String> generateAsync() {
        return CompletableFuture.supplyAsync(() -> IdGenerator.snowflakeId());
    }
    
    /**
     * 批量异步生成ID
     */
    public static CompletableFuture<List<String>> generateBatch(int count) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> ids = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                ids.add(IdGenerator.snowflakeId());
            }
            return ids;
        });
    }
}
```

#### 3. GraalVM Native Image 支持

**目标**: 支持编译为 Native 可执行文件

**工作内容**:

- 添加 Native Hints
- 移除反射依赖
- 优化类初始化

**预计启动时间**: < 100ms (当前 ~2s)

**预计内存占用**: < 50MB (当前 ~200MB)

#### 4. Kotlin 扩展

```kotlin
// Kotlin 友好的 API
fun <T> Result<T>.onSuccess(block: (T) -> Unit): Result<T> {
    if (this.success) {
        block(this.data!!)
    }
    return this
}

fun <T> Result<T>.onError(block: (String, String) -> Unit): Result<T> {
    if (!this.success) {
        block(this.code, this.message)
    }
    return this
}

// 使用示例
userService.getById(userId)
    .onSuccess { user ->
        println("用户: ${user.username}")
    }
    .onError { code, message ->
        println("错误: $code - $message")
    }
```

## 长期展望

### v4.0.0 (2026+) - 云原生

**愿景**: 成为云原生应用的首选基础模块

#### 1. 多云支持

- AWS密钥管理集成
- Azure Key Vault集成
- 阿里云KMS集成
- 腾讯云KMS集成

#### 2. 服务网格集成

- Istio集成
- Linkerd集成
- 自动追踪注入

#### 3. AI增强

- 智能异常诊断
- 性能瓶颈分析
- 自动优化建议

## 技术债务

### 当前技术债务

#### 1. 测试覆盖率

**现状**: 85%

**目标**: 95%+

**行动计划**:

- [ ] 补充边界条件测试
- [ ] 增加异常场景测试
- [ ] 添加性能基准测试

**预计完成**: 2025-Q1

#### 2. 文档完善度

**现状**: 80%

**目标**: 100%

**行动计划**:

- [x] 完善模块README
- [x] 添加配置文档
- [x] 添加完整示例
- [x] 添加发展路线图
- [ ] 录制视频教程
- [ ] 编写最佳实践手册

**预计完成**: 2025-Q2

#### 3. 性能基准

**现状**: 无系统性基准测试

**目标**: 建立完整的性能基准体系

**行动计划**:

- [ ] 设计基准测试套件
- [ ] 建立性能监控dashboard
- [ ] 定期发布性能报告

**预计完成**: 2025-Q2

#### 4. 代码重复

**现状**: 部分工具类存在重复代码

**目标**: 消除重复代码

**行动计划**:

- [ ] 使用代码分析工具识别重复
- [ ] 重构重复代码
- [ ] 建立代码审查机制

**预计完成**: 2025-Q1

## 社区反馈

### 已收集的需求

| 需求 | 来源 | 优先级 | 状态 | 计划版本 |
|-----|------|--------|------|---------|
| RSA加密支持 | 票务系统 | 高 | 规划中 | v2.1.0 |
| 国密算法支持 | 支付系统 | 高 | 规划中 | v2.1.0 |
| 响应式支持 | 电商系统 | 中 | 规划中 | v3.0.0 |
| GraalVM Native Image | 社区 | 中 | 规划中 | v3.0.0 |
| Kotlin扩展 | 社区 | 低 | 规划中 | v3.0.0 |
| 性能优化 | 多个项目 | 高 | 规划中 | v2.2.0 |
| 更多ID生成策略 | 票务系统 | 高 | 规划中 | v2.1.0 |
| JSON Schema验证 | 多个项目 | 中 | 规划中 | v2.1.0 |

### 如何反馈需求

我们欢迎社区提出新的需求和建议:

1. **GitHub Issues**: 提交功能请求
2. **GitHub Discussions**: 参与讨论
3. **Pull Requests**: 直接贡献代码

## 发布计划

### 发布周期

- **主版本 (Major)**: 每年1次 (破坏性变更)
- **次版本 (Minor)**: 每季度1次 (新功能)
- **修订版本 (Patch)**: 按需发布 (Bug修复)

### 版本支持策略

- **当前大版本**: 长期支持 (LTS)
- **上一大版本**: 维护模式 (仅修复严重Bug)
- **更早版本**: 不再支持

### 即将发布

| 版本 | 计划发布时间 | 主要内容 |
|-----|------------|---------|
| v2.1.0 | 2025-01 | ID生成器增强、加密增强、分布式追踪 |
| v2.2.0 | 2025-04 | 性能优化、基准测试 |
| v3.0.0 | 2025-07 | 响应式支持、Native Image、Kotlin扩展 |

## 参与贡献

我们欢迎社区参与到路线图的制定和实现中来:

### 如何参与

1. **提出需求**: 在 GitHub Issues 中提交功能请求
2. **投票表决**: 对现有需求投票,帮助我们确定优先级
3. **贡献代码**: 提交 Pull Request 实现新功能
4. **编写文档**: 完善文档和示例
5. **反馈问题**: 报告Bug和性能问题

### 贡献指南

详见 [贡献指南](../../docs/CONTRIBUTING.md)

## 相关文档

- [模块 README](README.md) - 模块功能介绍
- [配置文档](CONFIG.md) - 详细配置说明
- [示例文档](EXAMPLE.md) - 完整使用示例
- [测试文档](TESTING.md) - 测试指南

---

**最后更新**: 2025-11-20  
**文档版本**: v1.0  
**路线图版本**: 2025-Q4

*本路线图会根据实际情况动态调整,最新版本请查看在线文档。*

