# nebula-lock-core 模块单元测试清单

## 模块说明

分布式锁核心抽象层，定义了Lock、LockManager接口以及@Locked注解。

## 核心功能

1. 锁接口定义（Lock, ReadWriteLock）
2. 锁管理器接口（LockManager）
3. 注解定义（@Locked）
4. 锁配置（LockConfig）

## 测试类清单

### 1. LockConfigTest

**测试类路径**: `io.nebula.lock.core.config.LockConfig`  
**测试目的**: 验证锁配置类的构建和默认值

| 测试方法 | 被测试方法 | 测试目的 |
|---------|-----------|---------|
| testDefaultConfig() | defaultConfig() | 验证默认配置值 |
| testBuilder() | builder() | 验证Builder构建 |

### 2. LockedAnnotationTest

**测试类路径**: `io.nebula.lock.core.annotation.Locked`  
**测试目的**: 验证注解属性

| 测试方法 | 被测试方法 | 测试目的 |
|---------|-----------|---------|
| testAnnotationAttributes() | - | 验证key, waitTime, leaseTime等属性 |

### 3. LockExceptionTest

**测试类路径**: `io.nebula.lock.core.exception` 包下的异常类  
**测试目的**: 验证异常体系

| 测试方法 | 被测试方法 | 测试目的 |
|---------|-----------|---------|
| testLockAcquisitionException() | - | 验证获取锁异常 |
| testLockReleaseException() | - | 验证释放锁异常 |

## 测试执行

```bash
mvn test -pl nebula/infrastructure/lock/nebula-lock-core
```

## 验收标准

- 配置类测试通过
- 异常类测试通过
- 注解定义正确

