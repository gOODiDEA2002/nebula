# Nebula Starter 优化建议 - 多场景Starter方案

**提出时间**: 2025-11-14  
**提出原因**: 在开发`nebula-doc-mcp-server`时发现，使用统一的`nebula-starter`会引入大量不需要的依赖，导致内存占用过高（1100MB+不必要组件）。

---

## 📋 问题背景

### 当前方案：单一Starter

**现状**:
- 只有一个`nebula-starter`包含所有模块
- 所有模块标记为`<optional>true</optional>`
- 用户需要通过`<exclusions>`手动排除不需要的模块

**问题**:
1. ❌ 用户需要了解每个模块的作用才能正确排除
2. ❌ 配置复杂，需要维护长长的`<exclusions>`列表
3. ❌ 容易遗漏某些依赖，导致不必要的组件启动
4. ❌ 对新手不友好

**示例** (MCP Server项目需要排除16个模块):
```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-starter</artifactId>
    <exclusions>
        <exclusion><!-- 1 --></exclusion>
        <exclusion><!-- 2 --></exclusion>
        <!-- ... 16个exclusions ... -->
    </exclusions>
</dependency>
```

---

## 💡 解决方案：多场景Starter

### 方案设计

提供**4个不同场景的Starter**，用户根据应用类型选择合适的starter：

| Starter | 场景 | 包含模块 | 内存占用估算 |
|---------|------|---------|-------------|
| `nebula-starter-minimal` | 工具库/CLI应用 | foundation | ~100MB |
| `nebula-starter-web` | Web单体应用 | foundation + web + cache | ~400MB |
| `nebula-starter-service` | 微服务应用 | foundation + web + rpc + discovery + cache + messaging | ~800MB |
| `nebula-starter-ai` | AI应用 | foundation + ai + cache | ~500MB |

### 详细设计

#### 1. `nebula-starter-minimal`

**适用场景**: 
- 工具库项目
- CLI命令行应用
- 批处理任务
- 数据处理脚本

**包含模块**:
```xml
<dependencies>
    <!-- 核心基础 -->
    <dependency>
        <groupId>io.nebula</groupId>
        <artifactId>nebula-foundation</artifactId>
    </dependency>
    
    <dependency>
        <groupId>io.nebula</groupId>
        <artifactId>nebula-autoconfigure</artifactId>
    </dependency>
</dependencies>
```

**功能**:
- ✅ 基础工具类（字符串、日期、加密等）
- ✅ 统一异常处理
- ✅ 结果封装（Result）
- ✅ 基础验证

**不包含**:
- ❌ Web服务器
- ❌ 数据库
- ❌ 缓存
- ❌ RPC
- ❌ 消息队列

---

#### 2. `nebula-starter-web`

**适用场景**:
- 单体Web应用
- Admin后台
- 简单REST API服务
- 不需要微服务能力的Web应用

**包含模块**:
```xml
<dependencies>
    <!-- 继承minimal -->
    <dependency>
        <groupId>io.nebula</groupId>
        <artifactId>nebula-starter-minimal</artifactId>
    </dependency>
    
    <!-- Web层 -->
    <dependency>
        <groupId>io.nebula</groupId>
        <artifactId>nebula-web</artifactId>
    </dependency>
    
    <!-- 数据访问 -->
    <dependency>
        <groupId>io.nebula</groupId>
        <artifactId>nebula-data-persistence</artifactId>
        <optional>true</optional>
    </dependency>
    
    <!-- 缓存 -->
    <dependency>
        <groupId>io.nebula</groupId>
        <artifactId>nebula-data-cache</artifactId>
    </dependency>
    
    <!-- 安全 -->
    <dependency>
        <groupId>io.nebula</groupId>
        <artifactId>nebula-security</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

**功能**:
- ✅ REST API支持
- ✅ JWT认证
- ✅ 限流
- ✅ 监控（Actuator）
- ✅ 多级缓存（Caffeine + Redis）
- ✅ 数据库访问（可选）
- ✅ 安全认证（可选）

**不包含**:
- ❌ RPC客户端/服务端
- ❌ 服务发现
- ❌ 消息队列
- ❌ 对象存储
- ❌ AI功能

---

#### 3. `nebula-starter-service`

**适用场景**:
- 微服务应用
- 分布式系统
- 需要RPC调用的服务
- 需要服务注册发现的应用

**包含模块**:
```xml
<dependencies>
    <!-- 继承web -->
    <dependency>
        <groupId>io.nebula</groupId>
        <artifactId>nebula-starter-web</artifactId>
    </dependency>
    
    <!-- RPC -->
    <dependency>
        <groupId>io.nebula</groupId>
        <artifactId>nebula-rpc-core</artifactId>
    </dependency>
    <dependency>
        <groupId>io.nebula</groupId>
        <artifactId>nebula-rpc-http</artifactId>
    </dependency>
    <dependency>
        <groupId>io.nebula</groupId>
        <artifactId>nebula-rpc-grpc</artifactId>
        <optional>true</optional>
    </dependency>
    
    <!-- 服务发现 -->
    <dependency>
        <groupId>io.nebula</groupId>
        <artifactId>nebula-discovery-core</artifactId>
    </dependency>
    <dependency>
        <groupId>io.nebula</groupId>
        <artifactId>nebula-discovery-nacos</artifactId>
        <optional>true</optional>
    </dependency>
    
    <!-- 消息队列 -->
    <dependency>
        <groupId>io.nebula</groupId>
        <artifactId>nebula-messaging-core</artifactId>
    </dependency>
    <dependency>
        <groupId>io.nebula</groupId>
        <artifactId>nebula-messaging-rabbitmq</artifactId>
        <optional>true</optional>
    </dependency>
    
    <!-- 分布式锁 -->
    <dependency>
        <groupId>io.nebula</groupId>
        <artifactId>nebula-lock-core</artifactId>
    </dependency>
    <dependency>
        <groupId>io.nebula</groupId>
        <artifactId>nebula-lock-redis</artifactId>
    </dependency>
    
    <!-- 任务调度 -->
    <dependency>
        <groupId>io.nebula</groupId>
        <artifactId>nebula-task</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

**功能**:
- ✅ 所有Web功能
- ✅ HTTP RPC客户端
- ✅ gRPC支持（可选）
- ✅ Nacos服务注册发现（可选）
- ✅ RabbitMQ消息队列（可选）
- ✅ Redis分布式锁
- ✅ XXL-JOB任务调度（可选）

**不包含**:
- ❌ AI功能
- ❌ 对象存储（按需引入）
- ❌ 搜索引擎（按需引入）

---

#### 4. `nebula-starter-ai`

**适用场景**:
- AI应用
- RAG系统
- LLM集成服务
- 向量检索应用

**包含模块**:
```xml
<dependencies>
    <!-- 核心基础 -->
    <dependency>
        <groupId>io.nebula</groupId>
        <artifactId>nebula-starter-minimal</artifactId>
    </dependency>
    
    <!-- AI核心 -->
    <dependency>
        <groupId>io.nebula</groupId>
        <artifactId>nebula-ai-core</artifactId>
    </dependency>
    
    <!-- Spring AI集成 -->
    <dependency>
        <groupId>io.nebula</groupId>
        <artifactId>nebula-ai-spring</artifactId>
    </dependency>
    
    <!-- LangChain4j集成 (可选) -->
    <dependency>
        <groupId>io.nebula</groupId>
        <artifactId>nebula-ai-langchain4j</artifactId>
        <optional>true</optional>
    </dependency>
    
    <!-- 缓存（AI应用常需要缓存） -->
    <dependency>
        <groupId>io.nebula</groupId>
        <artifactId>nebula-data-cache</artifactId>
    </dependency>
    
    <!-- Web支持 (可选，如果需要提供API) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

**功能**:
- ✅ 聊天服务（ChatService）
- ✅ Embedding服务
- ✅ 向量存储（VectorStoreService）
- ✅ RAG支持
- ✅ 多级缓存
- ✅ Spring AI / LangChain4j
- ✅ Web API（可选）

**不包含**:
- ❌ 数据库（AI应用常不需要传统数据库）
- ❌ RPC
- ❌ 消息队列
- ❌ 服务发现

---

## 📂 目录结构

```
nebula/
└── starter/
    ├── nebula-starter-minimal/      ← 新增
    │   └── pom.xml
    ├── nebula-starter-web/          ← 新增
    │   └── pom.xml
    ├── nebula-starter-service/      ← 新增
    │   └── pom.xml
    ├── nebula-starter-ai/           ← 新增
    │   └── pom.xml
    └── nebula-starter/              ← 保留（向后兼容）
        └── pom.xml
```

---

## 🎯 使用示例

### 示例1: MCP Server项目（当前项目）

**需求**: AI + 缓存 + Web

**优化前** (使用单一starter):
```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-starter</artifactId>
    <exclusions>
        <!-- 需要排除16个不需要的模块 -->
    </exclusions>
</dependency>
```

**优化后** (使用AI starter):
```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-starter-ai</artifactId>
</dependency>

<!-- 如果需要Web API -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

**效果**: 
- ✅ 不需要任何exclusion
- ✅ 内存占用: ~500MB (vs 1600MB)
- ✅ 启动时间更快

---

### 示例2: 微服务项目

**需求**: Web + RPC + 服务发现 + 消息队列

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-starter-service</artifactId>
</dependency>
```

**启用/禁用可选功能** (通过配置):
```yaml
nebula:
  rpc:
    grpc:
      enabled: true  # 启用gRPC
  
  discovery:
    nacos:
      enabled: true  # 启用Nacos
  
  messaging:
    rabbitmq:
      enabled: true  # 启用RabbitMQ
```

---

### 示例3: 简单Web应用

**需求**: Web + 数据库 + 缓存

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-starter-web</artifactId>
</dependency>
```

**启用数据库** (通过配置):
```yaml
nebula:
  data:
    persistence:
      enabled: true
      
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb
```

---

## 📊 方案对比

| 方案 | 依赖管理 | 配置复杂度 | 内存占用 | 新手友好度 | 推荐度 |
|------|---------|-----------|---------|-----------|--------|
| **当前方案** (单一starter) | 需要exclusions | 高 | 高 | 低 | ⭐⭐ |
| **多场景starter** | 无需exclusions | 低 | 优化 | 高 | ⭐⭐⭐⭐⭐ |
| 手动管理依赖 | 完全手动 | 中 | 最优 | 中 | ⭐⭐⭐ |

---

## 🚀 实施计划

### 阶段1: 创建新Starter模块 (2小时)

1. 创建4个新的starter子模块
2. 编写各自的pom.xml
3. 继承关系: minimal → web → service
4. ai独立继承minimal

### 阶段2: 文档和示例 (1小时)

1. 更新Nebula框架使用指南
2. 为每个starter创建README
3. 提供选择建议（决策树）
4. 更新示例项目

### 阶段3: 测试和发布 (1小时)

1. 创建测试项目验证
2. 性能测试（内存、启动时间）
3. 发布新版本

**总工时**: ~4小时

---

## 🎁 额外好处

1. **更好的文档组织**: 每个starter有独立的README，说明适用场景
2. **更快的编译**: 不需要的模块根本不会下载
3. **更清晰的依赖树**: `mvn dependency:tree`更简洁
4. **向后兼容**: 保留原有`nebula-starter`，老项目无需修改
5. **更容易扩展**: 未来可以添加更多场景starter

---

## 📝 决策树（帮助用户选择）

```
开始
├─ 需要AI功能？
│  └─ 是 → nebula-starter-ai
│
├─ 需要微服务能力（RPC/服务发现/消息队列）？
│  └─ 是 → nebula-starter-service
│
├─ 需要Web API/数据库？
│  └─ 是 → nebula-starter-web
│
└─ 只需要基础工具？
   └─ 是 → nebula-starter-minimal
```

---

## ✅ 验收标准

- [ ] 创建4个新starter模块
- [ ] 每个starter有清晰的README
- [ ] MCP Server项目改用`nebula-starter-ai`，无需exclusions
- [ ] 内存占用降低50%+
- [ ] 更新框架文档
- [ ] 通过集成测试

---

## 🤝 后续维护

### 新增模块时

当Nebula框架新增模块时，需要评估：
1. 这个模块属于哪个场景？
2. 是必需还是可选？
3. 更新对应的starter

### 版本管理

所有starter使用相同的版本号，统一升级。

---

## 📌 总结

**当前问题**: MCP Server项目因使用单一starter引入1100MB不必要组件

**解决方案**: 提供多场景starter，按需选择

**预期效果**: 
- ✅ 无需exclusions
- ✅ 内存降低50%+
- ✅ 配置更简单
- ✅ 新手更友好

**推荐**: ⭐⭐⭐⭐⭐ 强烈推荐实施

---

**提交时间**: 2025-11-14  
**提交人**: Nebula Framework Team  
**优先级**: P1 (高优先级)  
**预计工时**: 4小时  
**建议版本**: Nebula 2.1.0

