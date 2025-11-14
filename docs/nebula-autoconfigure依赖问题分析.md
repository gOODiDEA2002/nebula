# nebula-autoconfigure 依赖问题分析

**发现时间**: 2025-11-14  
**问题**: 使用`nebula-starter-ai`时，仍然加载了大量不需要的组件（H2、JPA、MyBatis、Nacos等）

---

## 🔍 问题根源

### 依赖链路追踪

```
nebula-doc-mcp-server
└── nebula-starter-ai
    └── nebula-starter-minimal
        └── nebula-autoconfigure  ← 问题所在！
            ├── nebula-discovery-nacos      ← 强制依赖！
            ├── nebula-data-persistence     ← 强制依赖！
            ├── nebula-rpc-http             ← 强制依赖！
            ├── nebula-rpc-grpc             ← 强制依赖！
            ├── nebula-messaging-rabbitmq   ← 强制依赖！
            ├── nebula-search-elasticsearch ← 强制依赖！
            ├── nebula-storage-minio        ← 强制依赖！
            ├── nebula-web                  ← 强制依赖！
            └── ... (所有模块)
```

### 问题代码

`nebula/autoconfigure/nebula-autoconfigure/pom.xml`:

```xml
<!-- ❌ 没有 <optional>true</optional> -->
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-discovery-nacos</artifactId>
    <version>${project.version}</version>
</dependency>

<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-data-persistence</artifactId>
    <version>${project.version}</version>
</dependency>

<!-- ... 等等，所有模块都是强制依赖 -->
```

**结果**: 
- 即使只需要AI功能，也会引入所有模块
- `nebula-data-persistence` → MyBatis + H2 + JPA (~600MB)
- `nebula-discovery-nacos` → Nacos Client (~100MB)
- `nebula-rpc-*` → RPC组件 (~200MB)
- 等等...

---

## 💡 设计问题分析

### AutoConfiguration的正确设计

Spring Boot的自动配置模块应该遵循以下原则：

1. **依赖应该是optional的**
   ```xml
   <dependency>
       <groupId>io.nebula</groupId>
       <artifactId>nebula-data-persistence</artifactId>
       <optional>true</optional>  ← 关键！
   </dependency>
   ```

2. **使用@ConditionalOnClass条件激活**
   ```java
   @ConditionalOnClass(name = "com.baomidou.mybatisplus.core.mapper.BaseMapper")
   public class DataPersistenceAutoConfiguration {
       // 只有类路径中存在MyBatis时才激活
   }
   ```

3. **使用@ConditionalOnProperty开关控制**
   ```java
   @ConditionalOnProperty(
       prefix = "nebula.data.persistence", 
       name = "enabled", 
       havingValue = "true", 
       matchIfMissing = false  ← 默认禁用
   )
   ```

### 参考：Spring Boot官方实践

Spring Boot的`spring-boot-autoconfigure`模块：
- 所有第三方依赖都标记为`<optional>true</optional>`
- 不会强制引入任何组件
- 完全基于类路径检测来激活配置

---

## ✅ 解决方案

### 方案A: 修改nebula-autoconfigure (推荐)

**修改**: `nebula/autoconfigure/nebula-autoconfigure/pom.xml`

将所有非核心依赖标记为`<optional>true</optional>`:

```xml
<!-- ✅ 正确做法 -->
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-discovery-nacos</artifactId>
    <version>${project.version}</version>
    <optional>true</optional>
</dependency>

<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-data-persistence</artifactId>
    <version>${project.version}</version>
    <optional>true</optional>
</dependency>

<!-- 所有模块都添加 <optional>true</optional> -->
```

**哪些依赖应该optional?**

| 依赖 | 是否optional | 原因 |
|------|-------------|------|
| `spring-boot-starter` | ❌ No | 核心依赖 |
| `spring-boot-autoconfigure` | ❌ No | 核心依赖 |
| `nebula-discovery-*` | ✅ Yes | 可选功能 |
| `nebula-rpc-*` | ✅ Yes | 可选功能 |
| `nebula-data-persistence` | ✅ Yes | 可选功能 |
| `nebula-data-cache` | ✅ Yes | 可选功能 |
| `nebula-messaging-*` | ✅ Yes | 可选功能 |
| `nebula-storage-*` | ✅ Yes | 可选功能 |
| `nebula-search-*` | ✅ Yes | 可选功能 |
| `nebula-ai-*` | ✅ Yes | 可选功能 |
| `nebula-web` | ✅ Yes | 可选功能 |
| `nebula-lock-*` | ✅ Yes | 可选功能 |

**预期效果**:
- `nebula-starter-ai` → 只包含 AI + Cache 相关依赖
- `nebula-starter-web` → 只包含 Web + Cache + Persistence 相关依赖
- 内存占用降低 ~1100MB

---

### 方案B: 临时规避 (不推荐)

在`nebula-starter-ai`中排除`nebula-autoconfigure`:

```xml
<!-- ❌ 不推荐的临时方案 -->
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-starter-minimal</artifactId>
    <exclusions>
        <exclusion>
            <groupId>io.nebula</groupId>
            <artifactId>nebula-autoconfigure</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

**问题**: 失去了自动配置能力

---

## 📊 影响范围

### 修改`nebula-autoconfigure`后的影响

| 项目类型 | 影响 | 说明 |
|---------|------|------|
| 使用`nebula-starter` | ✅ 无影响 | starter中显式声明了依赖 |
| 使用`nebula-starter-ai` | ✅ **受益** | 不再引入多余依赖 |
| 使用`nebula-starter-web` | ✅ **受益** | 不再引入多余依赖 |
| 使用`nebula-starter-service` | ✅ **受益** | 按需引入依赖 |
| 直接使用`nebula-autoconfigure` | ⚠️ 需要显式声明 | 需要在应用中显式声明模块依赖 |

### 向后兼容性

- ✅ 使用starter的项目：**完全兼容**
- ⚠️ 直接依赖`nebula-autoconfigure`的项目：需要显式添加模块依赖

**迁移建议**: 推荐所有项目使用starter，不要直接依赖`nebula-autoconfigure`。

---

## 🎯 实施步骤

1. ✅ 分析问题根源
2. ⏳ 修改`nebula-autoconfigure/pom.xml`，添加`<optional>true</optional>`
3. ⏳ 重新编译并安装`nebula-autoconfigure`
4. ⏳ 测试`nebula-starter-ai`项目
5. ⏳ 验证内存占用降低

---

## 🔧 后续改进建议

1. **文档补充**: 在README中说明依赖原则
2. **CI检查**: 添加Maven Enforcer插件，检测非optional依赖
3. **最佳实践**: 更新框架开发指南，强调optional的重要性

---

**结论**: `nebula-autoconfigure`的设计需要优化，所有功能模块依赖都应该标记为`<optional>true</optional>`。

**优先级**: P0 (最高)  
**预计工时**: 30分钟  
**预期收益**: 内存优化 ~1100MB

