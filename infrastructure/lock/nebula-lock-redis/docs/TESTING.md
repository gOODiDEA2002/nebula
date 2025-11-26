# nebula-lock-redis 模块单元测试清单

## 模块说明

基于Redisson的分布式锁实现模块，提供可重入锁、公平锁、读写锁、看门狗机制等功能。

## 核心功能

1. 可重入锁（lock、unlock、tryLock）
2. 公平锁
3. 读写锁（支持多个读锁、写锁互斥）
4. 看门狗机制（自动续期）
5. 注解式锁（@Locked，支持SpEL表达式）
6. 锁回调机制（execute方法）

## 测试类清单

### 1. RedisLockManagerTest

**测试类路径**: `io.nebula.lock.redis.RedisLockManager`  
**测试目的**: 验证分布式锁管理器的核心功能

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testGetLock() | getLock(String) | 测试获取锁对象 | RedissonClient |
| testLockAndUnlock() | lock() / unlock() | 测试基本的加锁和解锁操作 | RLock |
| testTryLock() | tryLock(Duration) | 测试尝试获取锁（带超时） | RLock |
| testTryLockFailed() | tryLock(Duration) | 测试获取锁失败的情况 | RLock |
| testExecuteWithLock() | execute(String, Supplier) | 测试锁回调执行，验证自动加锁解锁 | RLock |
| testTryExecute() | tryExecute(String, LockConfig, Supplier) | 测试尝试执行（获取锁失败返回null） | RLock |

**测试数据准备**:
- Mock RedissonClient
- Mock RLock对象
- 准备测试锁键名

**验证要点**:
- 锁对象正确获取
- lock/unlock配对调用
- tryLock超时正确
- execute自动释放锁
- 锁键名正确拼接

**Mock示例**:
```java
@Mock
private RedissonClient redissonClient;

@Mock
private RLock lock;

@BeforeEach
void setUp() {
    when(redissonClient.getLock(anyString())).thenReturn(lock);
    when(lock.tryLock(anyLong(), any(TimeUnit.class))).thenReturn(true);
}
```

---

### 2. ReentrantLockTest

**测试类路径**: 可重入锁测试  
**测试目的**: 验证同一线程可以多次获取同一把锁

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testReentrant() | lock() 多次 | 测试可重入性，同一线程多次lock | RLock |
| testUnlockCount() | unlock() | 测试必须unlock相同次数才能完全释放 | RLock |

**测试数据准备**:
- Mock可重入锁
- 配置lock()和unlock()的行为

**验证要点**:
- 同一线程可以多次lock
- 必须unlock相同次数
- 锁计数正确

---

### 3. ReadWriteLockTest

**测试类路径**: `io.nebula.lock.redis.ReadWriteLock`相关  
**测试目的**: 验证读写锁的正确性

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testGetReadWriteLock() | getReadWriteLock(String) | 测试获取读写锁 | RedissonClient, RReadWriteLock |
| testReadLock() | readLock().lock() | 测试读锁获取 | RLock |
| testWriteLock() | writeLock().lock() | 测试写锁获取 | RLock |
| testMultipleReadLocks() | - | 测试多个读锁可以并存 | RLock |
| testWriteLockExclusive() | - | 测试写锁互斥（有写锁时不能有其他锁） | RLock |

**测试数据准备**:
- Mock RedissonClient
- Mock RReadWriteLock
- Mock RLock（读锁和写锁）

**验证要点**:
- 读锁可以并发
- 写锁互斥
- 读写锁互斥

**Mock示例**:
```java
@Mock
private RReadWriteLock readWriteLock;

@Mock
private RLock readLock;

@Mock
private RLock writeLock;

@BeforeEach
void setUp() {
    when(redissonClient.getReadWriteLock(anyString())).thenReturn(readWriteLock);
    when(readWriteLock.readLock()).thenReturn(readLock);
    when(readWriteLock.writeLock()).thenReturn(writeLock);
}
```

---

### 4. LockedAspectTest

**测试类路径**: `io.nebula.lock.redis.aspect.LockedAspect`  
**测试目的**: 验证@Locked注解的AOP切面功能

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testLockedAnnotation() | @Locked注解切面 | 测试注解自动加锁解锁 | RedisLockManager, ProceedingJoinPoint |
| testSpELKeyGeneration() | - | 测试SpEL表达式动态生成锁键 | SpelExpressionParser |
| testFailStrategyThrowException() | - | 测试获取锁失败抛出异常 | RLock |
| testFailStrategyReturnNull() | - | 测试获取锁失败返回null | RLock |
| testFailStrategyReturnFalse() | - | 测试获取锁失败返回false（boolean方法） | RLock |

**测试数据准备**:
- Mock ProceedingJoinPoint
- Mock MethodSignature
- 准备@Locked注解参数
- 准备SpEL表达式上下文

**验证要点**:
- 注解自动加锁
- SpEL表达式解析正确
- 失败策略正确执行
- finally块总是释放锁

**Mock示例**:
```java
@Mock
private ProceedingJoinPoint joinPoint;

@Mock
private MethodSignature signature;

@Mock
private Locked locked;

@BeforeEach
void setUp() {
    when(joinPoint.getSignature()).thenReturn(signature);
    when(signature.getMethod()).thenReturn(testMethod);
    when(locked.key()).thenReturn("'test:' + #id");
    when(locked.waitTime()).thenReturn(10);
    when(locked.leaseTime()).thenReturn(60);
}
```

---

## Mock策略

### 需要Mock的对象

| Mock对象 | 使用场景 | Mock行为 |
|---------|---------|---------|
| RedissonClient | 所有测试 | Mock getLock(), getReadWriteLock() |
| RLock | 锁操作测试 | Mock lock(), unlock(), tryLock() |
| RReadWriteLock | 读写锁测试 | Mock readLock(), writeLock() |
| ProceedingJoinPoint | 切面测试 | Mock proceed(), getArgs() |
| MethodSignature | 切面测试 | Mock getMethod(), getParameterNames() |

### 不需要真实Redis
**所有测试都应该Mock Redisson客户端，不需要启动真实的Redis服务**。

---

## 测试依赖

```xml
<dependencies>
    <!-- JUnit 5 -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- Mockito -->
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-core</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- AssertJ -->
    <dependency>
        <groupId>org.assertj</groupId>
        <artifactId>assertj-core</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- Spring Boot Test -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- Spring AOP (切面测试) -->
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-aop</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## 测试执行

运行测试：
```bash
mvn test -pl nebula/infrastructure/lock/nebula-lock-redis
```

查看测试报告：
```bash
mvn surefire-report:report
```

---

## 验收标准

- 所有测试方法通过
- 核心功能测试覆盖率 >= 90%
- Mock对象使用正确，无真实Redis依赖
- SpEL表达式解析测试通过
- 所有失败策略都有测试覆盖

