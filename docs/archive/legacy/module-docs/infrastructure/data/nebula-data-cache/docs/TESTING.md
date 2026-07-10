# nebula-data-cache 模块单元测试清单

## 模块说明

缓存抽象层，提供统一的缓存操作接口，支持本地缓存（Caffeine）、Redis缓存和多级缓存架构。

## 核心功能

1. 缓存基本操作（get、put、evict、exists）
2. 批量操作（multiGet、multiPut）
3. 注解方式缓存（@Cacheable、@CachePut、@CacheEvict）
4. 多级缓存（L1本地缓存 + L2 Redis缓存）

## 测试类清单

### 1. CacheManagerTest

**测试类路径**: `io.nebula.data.cache.CacheManager`  
**测试目的**: 验证缓存管理器的基本操作

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testGet() | get(String, Class) | 测试获取缓存 | 无（使用内存实现） |
| testPut() | put(String, Object) | 测试设置缓存 | 无 |
| testPutWithTtl() | put(String, Object, Duration) | 测试带TTL的缓存 | 无 |
| testEvict() | evict(String) | 测试清除缓存 | 无 |
| testEvictPattern() | evictPattern(String) | 测试按模式清除缓存 | 无 |
| testExists() | exists(String) | 测试缓存是否存在 | 无 |
| testKeys() | keys(String) | 测试获取匹配的键列表 | 无 |
| testMultiGet() | multiGet(Collection, Class) | 测试批量获取 | 无 |
| testMultiPut() | multiPut(Map) | 测试批量设置 | 无 |

**测试数据准备**:
- 使用内存实现的CacheManager
- 准备测试键值对

**验证要点**:
- 缓存正确存储和读取
- TTL正确生效
- 清除操作生效
- 批量操作正确

**测试示例**:
```java
@Test
void testPutAndGet() {
    CacheManager cacheManager = new LocalCacheManager();
    
    String key = "test:key";
    String value = "test value";
    
    cacheManager.put(key, value);
    
    String cached = cacheManager.get(key, String.class);
    
    assertThat(cached).isEqualTo(value);
}
```

---

### 2. LocalCacheManagerTest

**测试类路径**: `io.nebula.data.cache.local.LocalCacheManager`  
**测试目的**: 验证Caffeine本地缓存的功能

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testCaffeineCache() | - | 测试Caffeine缓存基本功能 | 无 |
| testExpireAfterWrite() | - | 测试写入后过期 | 无 |
| testExpireAfterAccess() | - | 测试访问后过期 | 无 |
| testMaximumSize() | - | 测试最大容量限制 | 无 |
| testEvictionPolicy() | - | 测试淘汰策略 | 无 |

**测试数据准备**:
- 创建配置好的LocalCacheManager
- 准备测试数据

**验证要点**:
- 缓存过期时间生效
- 容量限制生效
- LRU策略正确

---

### 3. MultiLevelCacheManagerTest

**测试类路径**: `io.nebula.data.cache.multilevel.MultiLevelCacheManager`  
**测试目的**: 验证多级缓存的L1/L2缓存机制

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testL1CacheHit() | get() | 测试L1缓存命中 | 无 |
| testL2CacheHit() | get() | 测试L1未命中但L2命中 | 无 |
| testCacheMiss() | get() | 测试L1和L2都未命中 | 无 |
| testPutToMultiLevel() | put() | 测试同时写入L1和L2 | 无 |
| testEvictFromMultiLevel() | evict() | 测试同时清除L1和L2 | 无 |
| testSyncOnUpdate() | - | 测试更新时缓存同步 | 无 |

**测试数据准备**:
- Mock LocalCacheManager (L1)
- Mock RedisCacheManager (L2)
- 创建MultiLevelCacheManager

**验证要点**:
- L1优先查找
- L1未命中查L2
- 写入同时写L1和L2
- 清除同时清L1和L2

**Mock示例**:
```java
@Mock
private LocalCacheManager l1Cache;

@Mock
private RedisCacheManager l2Cache;

@InjectMocks
private MultiLevelCacheManager multiLevelCache;

@Test
void testL1CacheHit() {
    String key = "test:key";
    String value = "test value";
    
    // L1有缓存
    when(l1Cache.get(key, String.class)).thenReturn(value);
    
    String result = multiLevelCache.get(key, String.class);
    
    assertThat(result).isEqualTo(value);
    verify(l1Cache).get(key, String.class);
    verify(l2Cache, never()).get(anyString(), any());
}
```

---

### 4. CacheableAnnotationTest

**测试类路径**: `@Cacheable`注解测试  
**测试目的**: 验证Spring Cache注解的功能

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testCacheable() | @Cacheable方法 | 测试缓存生效 | CacheManager |
| testCacheHit() | - | 测试缓存命中，方法不执行 | - |
| testCacheMiss() | - | 测试缓存未命中，方法执行 | - |
| testCachePut() | @CachePut方法 | 测试更新缓存 | CacheManager |
| testCacheEvict() | @CacheEvict方法 | 测试清除缓存 | CacheManager |

**测试数据准备**:
- 创建带缓存注解的测试Service
- 配置Spring Cache

**验证要点**:
- @Cacheable生效
- 缓存命中时方法不执行
- @CachePut更新缓存
- @CacheEvict清除缓存

---

## Mock策略

### 需要Mock的对象

| Mock对象 | 使用场景 | Mock行为 |
|---------|-----------|---------|
| LocalCacheManager | 多级缓存测试 | Mock get(), put() |
| RedisCacheManager | 多级缓存测试 | Mock get(), put() |
| RedisTemplate | Redis缓存测试 | Mock opsForValue() |

### 本地缓存不需要Mock
**LocalCacheManager和CaffeineCache可以使用真实实现，无需外部依赖**。

---

## 测试依赖

```xml
<dependencies>
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-core</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.assertj</groupId>
        <artifactId>assertj-core</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>com.github.ben-manes.caffeine</groupId>
        <artifactId>caffeine</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-cache</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## 测试执行

```bash
mvn test -pl nebula/infrastructure/data/nebula-data-cache
```

---

## 验收标准

- 所有测试方法通过
- 核心功能测试覆盖率 >= 90%
- 多级缓存L1/L2测试通过
- 注解方式缓存测试通过

