# Nebula Framework - 文档索引

##  核心文档

- [README.md](../README.md) - 框架概述和快速开始
- [Nebula框架使用指南.md](Nebula框架使用指南.md) - 完整使用指南
- [Spring Boot自动配置文件详解.md](../Spring%20Boot自动配置文件详解.md) - 自动配置机制说明
- [CLAUDE.md](../CLAUDE.md) - AI辅助开发记录

##  专题文档

### RPC 远程过程调用
- [RPC 优化文档汇总](rpc/RPC_ALL_OPTIMIZATIONS_COMPLETED.md) -  所有RPC优化的总览
- [EnableRpcClients 增强](rpc/RPC_ENABLE_CLIENTS_VALUE_ENHANCEMENT.md) -  最新：零配置RPC客户端
- [RPC 优化设计](rpc/RPC_OPTIMIZATION_DESIGN.md) - 优化方案设计
- [RPC 优化总结](rpc/RPC_OPTIMIZATION_SUMMARY.md) - 优化成果总结
- [RPC 第5项优化](rpc/RPC_OPTIMIZATION_5_DESIGN.md) - @RpcCall简化设计
- [RPC 优化任务清单](rpc/RPC_OPTIMIZATION_TASKS.md) - 优化任务跟踪

### 自动配置
- [AutoConfiguration 详解](../autoconfigure/nebula-autoconfigure/README.md)
- [配置属性迁移](../autoconfigure/nebula-autoconfigure/PROPERTIES_MIGRATION_SUMMARY.md)
- [Nacos配置修复](../autoconfigure/nebula-autoconfigure/Nacos配置属性绑定问题修复说明.md)
- [启动顺序分析](../autoconfigure/nebula-autoconfigure/启动顺序分析报告.md)

## ️ 模块文档

### 基础设施层（Infrastructure）

#### RPC 模块
- [RPC Core](../infrastructure/rpc/nebula-rpc-core/)
- [RPC HTTP](../infrastructure/rpc/nebula-rpc-http/)
- [RPC gRPC](../infrastructure/rpc/nebula-rpc-grpc/)
- [RPC Roadmap](../infrastructure/rpc/ROADMAP.md)

#### 服务发现
- [Discovery Core](../infrastructure/discovery/nebula-discovery-core/)
- [Discovery Nacos](../infrastructure/discovery/nebula-discovery-nacos/)

#### 数据访问
- [Data Persistence](../infrastructure/data/nebula-data-persistence/)
- [Data Cache](../infrastructure/data/nebula-data-cache/)
- [Data MongoDB](../infrastructure/data/nebula-data-mongodb/)

#### 消息队列
- [Messaging Core](../infrastructure/messaging/nebula-messaging-core/)
- [Messaging RabbitMQ](../infrastructure/messaging/nebula-messaging-rabbitmq/)

#### 存储服务
- [Storage Core](../infrastructure/storage/nebula-storage-core/)
- [Storage MinIO](../infrastructure/storage/nebula-storage-minio/)
- [Storage Aliyun OSS](../infrastructure/storage/nebula-storage-aliyun-oss/)

#### 搜索服务
- [Search Core](../infrastructure/search/nebula-search-core/)
- [Search Elasticsearch](../infrastructure/search/nebula-search-elasticsearch/)

#### AI集成
- [AI Core](../infrastructure/ai/nebula-ai-core/)
- [AI Spring](../infrastructure/ai/nebula-ai-spring/)

### 应用层（Application）
- [Web Module](../application/nebula-web/)
- [Task Module](../application/nebula-task/)

### 核心层（Core）
- [Foundation](../core/nebula-foundation/)

### 集成层（Integration）
- [Payment Integration](../integration/nebula-integration-payment/)

##  关键特性文档

### 零配置 RPC（最新）
Nebula 2.0 的重大创新，实现了极致简化的 RPC 使用体验：

```java
// API 配置（3行）
@AutoConfiguration
@EnableRpcClients("nebula-example-user-service")
public class UserApiAutoConfiguration {
}

// RPC 客户端（零配置）
@RpcClient
public interface UserRpcClient {
    UserDto getUserById(Long id);
}

// 服务实现（零配置）
@RpcService
@RequiredArgsConstructor
public class OrderServiceImpl {
    private final UserRpcClient userRpcClient;  // 无需 @Qualifier
}
```

详见：[EnableRpcClients 增强文档](rpc/RPC_ENABLE_CLIENTS_VALUE_ENHANCEMENT.md)

##  文档分类

### 按主题分类
- **RPC**: `docs/rpc/`
- **自动配置**: `autoconfigure/nebula-autoconfigure/`
- **使用指南**: `docs/`

### 按优先级分类
-  **核心必读**: README使用指南RPC优化汇总
-  **深入学习**: 各模块专题文档
-  **参考资料**: 自动配置详解优化设计文档

##  外部资源

- [示例项目](../example/) - 完整的示例应用
- [Starter 模板](../starter/) - 快速开始模板

##  文档贡献指南

欢迎贡献文档！请遵循以下原则：
1. 文档应该简洁明了，重点突出
2. 提供完整的代码示例
3. 包含必要的图表说明
4. 注明版本和更新日期

---

**框架版本**: 2.0.0  
**文档更新**: 2025-10  
**许可**: Apache License 2.0

