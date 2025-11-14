# Nebula Framework 测试文档索引

> **版本**: 2.0.1-SNAPSHOT  
> **最后更新**: 2025-01-13

## 📋 文档导航

本索引提供 Nebula 框架所有模块的测试文档快速访问入口。

## 🗂️ 按层级分类

### 核心层 (Core Layer)

| 模块 | 测试文档 | 状态 | 覆盖率 |
|------|----------|------|--------|
| nebula-foundation | [TESTING.md](../../core/nebula-foundation/TESTING.md) | ✅ 已迁移 | 85% |
| nebula-security | [TESTING.md](../../core/nebula-security/TESTING.md) | ✅ 已迁移 | 82% |

### 数据访问层 (Data Access Layer)

| 模块 | 测试文档 | 状态 | 覆盖率 |
|------|----------|------|--------|
| nebula-data-persistence | [TESTING.md](../../infrastructure/data/nebula-data-persistence/TESTING.md) | ✅ 已迁移 | 88% |
| nebula-data-mongodb | [TESTING.md](../../infrastructure/data/nebula-data-mongodb/TESTING.md) | ✅ 已迁移 | 80% |
| nebula-data-cache | [TESTING.md](../../infrastructure/data/nebula-data-cache/TESTING.md) | ✅ 已迁移 | 86% |

### 消息传递层 (Messaging Layer)

| 模块 | 测试文档 | 状态 | 覆盖率 |
|------|----------|------|--------|
| nebula-messaging-core | [TESTING.md](../../infrastructure/messaging/nebula-messaging-core/TESTING.md) | ✅ 已迁移 | 90% |
| nebula-messaging-rabbitmq | [TESTING.md](../../infrastructure/messaging/nebula-messaging-rabbitmq/TESTING.md) | ✅ 已迁移 | 84% |

### RPC 层 (RPC Layer)

| 模块 | 测试文档 | 状态 | 覆盖率 |
|------|----------|------|--------|
| nebula-rpc-core | [TESTING.md](../../infrastructure/rpc/nebula-rpc-core/TESTING.md) | 🚧 待补充 | N/A |
| nebula-rpc-http | [TESTING.md](../../infrastructure/rpc/nebula-rpc-http/TESTING.md) | ✅ 已迁移 | 83% |
| nebula-rpc-grpc | [TESTING.md](../../infrastructure/rpc/nebula-rpc-grpc/TESTING.md) | ✅ 已迁移 | 81% |

### 服务发现层 (Service Discovery Layer)

| 模块 | 测试文档 | 状态 | 覆盖率 |
|------|----------|------|--------|
| nebula-discovery-core | [TESTING.md](../../infrastructure/discovery/nebula-discovery-core/TESTING.md) | 🚧 待补充 | N/A |
| nebula-discovery-nacos | [TESTING.md](../../infrastructure/discovery/nebula-discovery-nacos/TESTING.md) | ✅ 已迁移 | 79% |

### 存储层 (Storage Layer)

| 模块 | 测试文档 | 状态 | 覆盖率 |
|------|----------|------|--------|
| nebula-storage-minio | [TESTING.md](../../infrastructure/storage/nebula-storage-minio/TESTING.md) | ✅ 已迁移 | 85% |
| nebula-storage-aliyun-oss | [TESTING.md](../../infrastructure/storage/nebula-storage-aliyun-oss/TESTING.md) | 🚧 待补充 | N/A |

### 搜索层 (Search Layer)

| 模块 | 测试文档 | 状态 | 覆盖率 |
|------|----------|------|--------|
| nebula-search-elasticsearch | [TESTING.md](../../infrastructure/search/nebula-search-elasticsearch/TESTING.md) | ✅ 已迁移 | 82% |

### AI 层 (AI Layer)

| 模块 | 测试文档 | 状态 | 覆盖率 |
|------|----------|------|--------|
| nebula-ai-spring | [TESTING.md](../../infrastructure/ai/nebula-ai-spring/TESTING.md) | ✅ 已迁移 | 78% |

### 分布式锁层 (Lock Layer)

| 模块 | 测试文档 | 状态 | 覆盖率 |
|------|----------|------|--------|
| nebula-lock-core | [TESTING.md](../../infrastructure/lock/nebula-lock-core/TESTING.md) | 🚧 待补充 | N/A |
| nebula-lock-redis | [TESTING.md](../../infrastructure/lock/nebula-lock-redis/TESTING.md) | ✅ 已迁移 | 87% |

### 应用层 (Application Layer)

| 模块 | 测试文档 | 状态 | 覆盖率 |
|------|----------|------|--------|
| nebula-web | [TESTING.md](../../application/nebula-web/TESTING.md) | ✅ 已迁移 | 88% |
| nebula-task | [TESTING.md](../../application/nebula-task/TESTING.md) | ✅ 已迁移 | 86% |

## 📊 整体测试统计

### 覆盖率概览

| 指标 | 目标 | 当前 | 状态 |
|------|------|------|------|
| 平均行覆盖率 | ≥80% | 84% | ✅ 达标 |
| 平均分支覆盖率 | ≥70% | 73% | ✅ 达标 |
| P0用例通过率 | 100% | 100% | ✅ 达标 |
| P1用例通过率 | ≥95% | 97% | ✅ 达标 |

### 测试用例分布

| 层级 | 模块数 | 用例数 | 通过率 |
|------|--------|--------|--------|
| 核心层 | 2 | 156 | 100% |
| 数据访问层 | 3 | 289 | 99.7% |
| 消息传递层 | 2 | 134 | 100% |
| RPC 层 | 3 | 178 | 100% |
| 服务发现层 | 2 | 98 | 100% |
| 存储层 | 2 | 142 | 100% |
| 搜索层 | 1 | 87 | 100% |
| AI 层 | 1 | 64 | 100% |
| 分布式锁层 | 2 | 112 | 100% |
| 应用层 | 2 | 201 | 100% |
| **总计** | **20** | **1,461** | **99.9%** |

## 🎯 测试策略

### 测试原则

1. **独立性**: 每个测试用例独立运行，不依赖其他用例
2. **可重复性**: 测试结果可重复，不受执行顺序影响
3. **快速反馈**: 单元测试执行时间 < 10秒/模块
4. **清晰性**: 测试意图明确，使用 Given-When-Then 模式

### 测试层次

```
┌─────────────────────────────────────┐
│     端到端测试 (E2E)                 │  5%
├─────────────────────────────────────┤
│     集成测试 (Integration)           │  25%
├─────────────────────────────────────┤
│     单元测试 (Unit)                  │  70%
└─────────────────────────────────────┘
```

### 测试优先级

- **P0 (关键)**: 核心功能、数据安全、系统稳定性
- **P1 (重要)**: 主要功能、常用场景
- **P2 (一般)**: 辅助功能、边界场景
- **P3 (可选)**: 增强功能、极端场景

## 🛠️ 测试工具

### 核心工具

| 工具 | 版本 | 用途 |
|------|------|------|
| JUnit 5 | 5.10.1 | 测试框架 |
| Mockito | 5.7.0 | Mock 框架 |
| AssertJ | 3.24.2 | 断言库 |
| Spring Boot Test | 3.2.12 | Spring 测试支持 |
| TestContainers | 1.19.3 | 容器化测试 |
| JaCoCo | 0.8.11 | 覆盖率工具 |

### 辅助工具

| 工具 | 用途 |
|------|------|
| WireMock | HTTP API Mock |
| Awaitility | 异步测试 |
| JSONAssert | JSON 断言 |
| Testcontainers | 集成测试容器 |

## 📚 测试最佳实践

### 命名规范

```java
// 推荐的测试方法命名
testMethodName_Should[ExpectedBehavior]_When[StateUnderTest]()

// 示例
testCreateUser_ShouldReturnSuccess_WhenValidInput()
testDeleteOrder_ShouldThrowException_WhenOrderNotFound()
```

### 测试结构

```java
@Test
@DisplayName("清晰的测试描述")
void testMethodName() {
    // Given - 准备测试数据
    String input = "test data";
    
    // When - 执行被测方法
    Result result = service.method(input);
    
    // Then - 验证结果
    assertThat(result).isNotNull();
    assertThat(result.isSuccess()).isTrue();
}
```

### Mock 使用

```java
// 推荐: 使用 @Mock 和 @InjectMocks
@ExtendWith(MockitoExtension.class)
class ServiceTest {
    @Mock
    private Dependency dependency;
    
    @InjectMocks
    private ServiceImpl service;
    
    @Test
    void testMethod() {
        // 配置 Mock 行为
        when(dependency.method()).thenReturn(value);
        
        // 执行测试
        // 验证结果
    }
}
```

## 🚀 执行测试

### 本地执行

```bash
# 执行所有测试
mvn clean test

# 执行特定模块测试
mvn test -pl infrastructure/data/nebula-data-cache

# 执行特定测试类
mvn test -Dtest=CacheServiceTest

# 生成覆盖率报告
mvn clean test jacoco:report
```

### CI/CD 执行

```yaml
# GitHub Actions 示例
name: Test
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Set up JDK 21
        uses: actions/setup-java@v2
        with:
          java-version: '21'
      - name: Run tests
        run: mvn clean test
      - name: Upload coverage
        uses: codecov/codecov-action@v2
```

## 📝 贡献测试

### 添加新测试

1. 参考 [测试文档模板](../templates/TESTING_TEMPLATE.md)
2. 在模块目录下创建或更新 `TESTING.md`
3. 编写测试用例代码
4. 确保测试通过且覆盖率达标
5. 更新本索引文档

### 测试 Review Checklist

- [ ] 测试用例覆盖所有核心功能
- [ ] 使用 Given-When-Then 模式
- [ ] 测试方法命名清晰
- [ ] 添加 `@DisplayName` 注解
- [ ] 覆盖率达到目标
- [ ] 所有测试通过
- [ ] 测试文档更新

## 🔗 相关资源

- [测试文档模板](../templates/TESTING_TEMPLATE.md)
- [模块 README 模板](../templates/MODULE_README_TEMPLATE.md)
- [Nebula 框架使用指南](../Nebula框架使用指南.md)
- [贡献指南](../../CONTRIBUTING.md)

## 📧 联系方式

如有测试相关问题，请通过以下方式联系:
- GitHub Issues: [提交 Issue](https://github.com/your-org/nebula/issues)
- 邮件: test@nebula-framework.io

---

**测试驱动质量** - Nebula Framework 测试团队

