# nebula-data-persistence 模块单元测试文档

> **模块**: nebula-data-persistence  
> **版本**: 2.0.1-SNAPSHOT  
> **最后更新**: 2025-01-13

## 📋 测试概述

### 测试目标

数据持久层模块，提供统一的数据访问抽象，基于MyBatis-Plus构建，集成读写分离、分库分表等功能的全面测试。

### 核心功能

1. 基础CRUD操作（BaseMapper、IService）
2. 分页查询
3. 批量操作
4. 读写分离（@ReadDataSource、@WriteDataSource）
5. 分库分表（ShardingSphere集成）

### 测试覆盖率目标

- **行覆盖率**: ≥ 90%
- **分支覆盖率**: ≥ 88%
- **核心业务逻辑**: 100%

## 🧪 测试用例设计

### 1. ServiceImplTest

**测试类路径**: `com.baomidou.mybatisplus.extension.service.impl.ServiceImpl`的子类  
**测试目的**: 验证MyBatis-Plus的Service层基础CRUD功能

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testSave() | save(T) | 测试保存实体 | BaseMapper |
| testSaveBatch() | saveBatch(Collection) | 测试批量保存 | BaseMapper |
| testGetById() | getById(Serializable) | 测试根据ID查询 | BaseMapper |
| testUpdateById() | updateById(T) | 测试根据ID更新 | BaseMapper |
| testRemoveById() | removeById(Serializable) | 测试根据ID删除（逻辑删除） | BaseMapper |
| testPage() | page(Page, Wrapper) | 测试分页查询 | BaseMapper |
| testList() | list(Wrapper) | 测试列表查询 | BaseMapper |
| testCount() | count(Wrapper) | 测试统计数量 | BaseMapper |

**测试数据准备**:
- Mock BaseMapper（如UserMapper）
- 准备测试实体对象
- 准备测试查询条件

**验收标准**:
- ✅ CRUD操作正确
- ✅ 分页参数正确
- ✅ 查询条件正确
- ✅ 逻辑删除生效

**Mock示例**:
```java
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    
    @Mock
    private UserMapper userMapper;
    
    @InjectMocks
    private UserServiceImpl userService;
    
    @Test
    @DisplayName("Should save user successfully")
    void testSave() {
        // Given
        User user = User.builder()
            .username("test")
            .name("Test User")
            .build();
        
        when(userMapper.insert(any(User.class))).thenReturn(1);
        
        // When
        boolean result = userService.save(user);
        
        // Then
        assertThat(result).isTrue();
        verify(userMapper).insert(user);
    }
}
```

### 2. ReadWriteSeparationTest

**测试类路径**: 读写分离功能测试  
**测试目的**: 验证@ReadDataSource和@WriteDataSource注解的数据源路由功能

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testReadDataSource() | @ReadDataSource注解的方法 | 测试读操作路由到从库 | DataSourceContextHolder |
| testWriteDataSource() | @WriteDataSource注解的方法 | 测试写操作路由到主库 | DataSourceContextHolder |
| testTransactionForceWrite() | @Transactional方法 | 测试事务中强制使用主库 | - |

**测试数据准备**:
- 配置多数据源
- 创建带注解的Service方法

**验收标准**:
- ✅ @ReadDataSource切换到从库
- ✅ @WriteDataSource切换到主库
- ✅ 事务中使用主库
- ✅ DataSourceContext正确设置

**Mock示例**:
```java
@Test
@DisplayName("Should route to read datasource when using @ReadDataSource")
void testReadDataSource() {
    // Given
    // 创建带@ReadDataSource注解的方法
    
    // When
    User user = productService.getProductById(1L);
    
    // Then
    // 验证使用了从库数据源
    String dataSource = DataSourceContextHolder.getDataSourceType();
    assertThat(dataSource).isEqualTo(DataSourceType.READ.name());
}
```

### 3. MapperTest

**测试类路径**: BaseMapper实现类  
**测试目的**: 验证Mapper接口的基本SQL操作

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testInsert() | insert(T) | 测试插入操作 | SqlSession |
| testSelectById() | selectById(Serializable) | 测试根据ID查询 | SqlSession |
| testUpdateById() | updateById(T) | 测试根据ID更新 | SqlSession |
| testDeleteById() | deleteById(Serializable) | 测试根据ID删除 | SqlSession |
| testSelectList() | selectList(Wrapper) | 测试列表查询 | SqlSession |

**测试数据准备**:
- Mock SqlSession
- 准备测试SQL和结果

**验收标准**:
- ✅ SQL正确执行
- ✅ 参数正确绑定
- ✅ 结果正确映射

## 🔧 Mock 策略

### 需要Mock的对象

| Mock对象 | 使用场景 | Mock行为 |
|---------|-----------|---------|
| BaseMapper | Service层测试 | Mock CRUD方法 |
| SqlSession | Mapper层测试 | Mock selectOne(), selectList() |
| DataSource | 数据源测试 | Mock getConnection() |
| DataSourceContextHolder | 读写分离测试 | Mock setDataSourceType() |

### 不需要真实数据库

**所有测试都应该Mock数据库操作，不需要启动真实的数据库**。

## 📊 测试依赖

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
        <groupId>org.mybatis.spring.boot</groupId>
        <artifactId>mybatis-spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

## 🚀 测试执行

### 执行所有测试

```bash
# Maven
mvn test -pl infrastructure/data/nebula-data-persistence

# 执行特定测试类
mvn test -Dtest=UserServiceImplTest

# 生成覆盖率报告
mvn clean test jacoco:report
```

### 查看测试报告

```bash
# Surefire报告
mvn surefire-report:report

# 覆盖率报告
open target/site/jacoco/index.html
```

## 📝 测试验收标准

- ✅ 所有测试方法通过
- ✅ 核心功能测试覆盖率 >= 90%
- ✅ Mock对象使用正确，无真实数据库依赖
- ✅ 读写分离测试通过

## 🧩 集成测试

### Testcontainers集成

使用Testcontainers进行真实数据库测试：

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mysql</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

**配置测试基类**:
```java
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
public abstract class DataPersistenceIntegrationTest {
    
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("ticket_test")
            .withUsername("test")
            .withPassword("test");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }
}
```

### 票务场景集成测试示例

```java
/**
 * 演出场次服务集成测试
 */
@DisplayName("Showtime Service Integration Tests")
class ShowtimeServiceIntegrationTest extends DataPersistenceIntegrationTest {
    
    @Autowired
    private ShowtimeService showtimeService;
    
    @Test
    @DisplayName("Should create showtime successfully")
    void testCreateShowtime() {
        // Given
        Showtime showtime = new Showtime();
        showtime.setName("周杰伦演唱会");
        showtime.setVenue("鸟巢体育场");
        showtime.setShowTime(LocalDateTime.now().plusDays(30));
        showtime.setEndTime(LocalDateTime.now().plusDays(30).plusHours(3));
        showtime.setPrice(new BigDecimal("680.00"));
        showtime.setTotalSeats(8000);
        
        // When
        Long id = showtimeService.createShowtime(showtime);
        
        // Then
        assertThat(id).isNotNull();
        
        Showtime saved = showtimeService.getById(id);
        assertThat(saved).isNotNull();
        assertThat(saved.getName()).isEqualTo("周杰伦演唱会");
        assertThat(saved.getAvailableSeats()).isEqualTo(8000);
        assertThat(saved.getStatus()).isEqualTo("UPCOMING");
        assertThat(saved.getCreateTime()).isNotNull();
        assertThat(saved.getVersion()).isEqualTo(1);
        assertThat(saved.getDeleted()).isEqualTo(0);
    }
    
    @Test
    @DisplayName("Should update available seats with optimistic lock")
    void testUpdateAvailableSeatsWithOptimisticLock() {
        // Given
        Showtime showtime = createTestShowtime();
        Long showtimeId = showtimeService.createShowtime(showtime);
        
        // When
        boolean success = showtimeService.updateAvailableSeats(showtimeId, 10);
        
        // Then
        assertThat(success).isTrue();
        
        Showtime updated = showtimeService.getById(showtimeId);
        assertThat(updated.getAvailableSeats()).isEqualTo(showtime.getTotalSeats() - 10);
        assertThat(updated.getVersion()).isEqualTo(2); // 版本号递增
    }
    
    @Test
    @DisplayName("Should handle concurrent seat updates correctly")
    void testConcurrentSeatUpdates() throws InterruptedException {
        // Given
        Showtime showtime = createTestShowtime();
        showtime.setTotalSeats(100);
        Long showtimeId = showtimeService.createShowtime(showtime);
        
        int threadCount = 10;
        int seatsPerThread = 5;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        
        // When: 10个线程同时尝试扣减库存
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    boolean success = showtimeService.updateAvailableSeats(showtimeId, seatsPerThread);
                    if (success) {
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        // Then: 只有部分线程成功（乐观锁生效）
        Showtime updated = showtimeService.getById(showtimeId);
        int expectedAvailableSeats = 100 - (successCount.get() * seatsPerThread);
        assertThat(updated.getAvailableSeats()).isEqualTo(expectedAvailableSeats);
    }
    
    private Showtime createTestShowtime() {
        Showtime showtime = new Showtime();
        showtime.setName("测试演出");
        showtime.setVenue("测试场馆");
        showtime.setShowTime(LocalDateTime.now().plusDays(7));
        showtime.setEndTime(LocalDateTime.now().plusDays(7).plusHours(2));
        showtime.setPrice(new BigDecimal("200.00"));
        showtime.setTotalSeats(500);
        return showtime;
    }
}

/**
 * 订单服务集成测试
 */
@DisplayName("Order Service Integration Tests")
class OrderServiceIntegrationTest extends DataPersistenceIntegrationTest {
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private ShowtimeService showtimeService;
    
    @Autowired
    private IdGenerator idGenerator;
    
    @Test
    @DisplayName("Should create order successfully")
    void testCreateOrder() {
        // Given
        Long showtimeId = createTestShowtime();
        Long userId = 1001L;
        Integer quantity = 2;
        String seats = "A10,A11";
        
        // When
        String orderNo = orderService.createOrder(userId, showtimeId, quantity, seats);
        
        // Then
        assertThat(orderNo).isNotNull();
        
        Order order = orderService.getOrderByOrderNo(orderNo);
        assertThat(order).isNotNull();
        assertThat(order.getUserId()).isEqualTo(userId);
        assertThat(order.getShowtimeId()).isEqualTo(showtimeId);
        assertThat(order.getQuantity()).isEqualTo(quantity);
        assertThat(order.getSeats()).isEqualTo(seats);
        assertThat(order.getStatus()).isEqualTo("PENDING");
        assertThat(order.getExpireTime()).isAfter(LocalDateTime.now());
    }
    
    @Test
    @DisplayName("Should cancel expired orders")
    void testCancelExpiredOrders() {
        // Given: 创建一个已过期的订单
        Long showtimeId = createTestShowtime();
        String orderNo = orderService.createOrder(1001L, showtimeId, 1, "A10");
        
        Order order = orderService.getOrderByOrderNo(orderNo);
        order.setExpireTime(LocalDateTime.now().minusMinutes(1)); // 设置为已过期
        orderService.updateById(order);
        
        // When
        int cancelledCount = orderService.cancelExpiredOrders();
        
        // Then
        assertThat(cancelledCount).isGreaterThanOrEqualTo(1);
        
        Order cancelled = orderService.getOrderByOrderNo(orderNo);
        assertThat(cancelled.getStatus()).isEqualTo("CANCELLED");
    }
    
    @Test
    @DisplayName("Should query user orders with pagination")
    void testGetUserOrdersPage() {
        // Given: 创建多个订单
        Long showtimeId = createTestShowtime();
        Long userId = 1001L;
        for (int i = 0; i < 15; i++) {
            orderService.createOrder(userId, showtimeId, 1, "A" + i);
        }
        
        // When: 分页查询
        Page<Order> page1 = new Page<>(1, 10);
        page1 = orderService.lambdaQuery()
                .eq(Order::getUserId, userId)
                .orderByDesc(Order::getCreateTime)
                .page(page1);
        
        // Then
        assertThat(page1.getRecords()).hasSize(10);
        assertThat(page1.getTotal()).isGreaterThanOrEqualTo(15);
        assertThat(page1.getPages()).isGreaterThanOrEqualTo(2);
        
        // When: 查询第二页
        Page<Order> page2 = new Page<>(2, 10);
        page2 = orderService.lambdaQuery()
                .eq(Order::getUserId, userId)
                .orderByDesc(Order::getCreateTime)
                .page(page2);
        
        // Then
        assertThat(page2.getRecords()).hasSizeGreaterThanOrEqualTo(5);
    }
    
    private Long createTestShowtime() {
        Showtime showtime = new Showtime();
        showtime.setName("测试演出");
        showtime.setVenue("测试场馆");
        showtime.setShowTime(LocalDateTime.now().plusDays(7));
        showtime.setEndTime(LocalDateTime.now().plusDays(7).plusHours(2));
        showtime.setPrice(new BigDecimal("200.00"));
        showtime.setTotalSeats(500);
        return showtimeService.createShowtime(showtime);
    }
}

/**
 * 事务管理集成测试
 */
@DisplayName("Transaction Management Integration Tests")
class TransactionIntegrationTest extends DataPersistenceIntegrationTest {
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private ShowtimeService showtimeService;
    
    @Test
    @DisplayName("Should rollback transaction when exception occurs")
    void testTransactionRollback() {
        // Given
        Long showtimeId = createTestShowtime();
        Long userId = 1001L;
        
        // When: 在事务中抛出异常
        assertThatThrownBy(() -> {
            transactionalCreateOrder(showtimeId, userId);
        }).isInstanceOf(RuntimeException.class);
        
        // Then: 订单不应该被创建（事务回滚）
        List<Order> orders = orderService.lambdaQuery()
                .eq(Order::getUserId, userId)
                .list();
        assertThat(orders).isEmpty();
    }
    
    @Transactional(rollbackFor = Exception.class)
    private void transactionalCreateOrder(Long showtimeId, Long userId) {
        orderService.createOrder(userId, showtimeId, 1, "A10");
        // 故意抛出异常触发回滚
        throw new RuntimeException("Test rollback");
    }
    
    private Long createTestShowtime() {
        Showtime showtime = new Showtime();
        showtime.setName("测试演出");
        showtime.setVenue("测试场馆");
        showtime.setShowTime(LocalDateTime.now().plusDays(7));
        showtime.setEndTime(LocalDateTime.now().plusDays(7).plusHours(2));
        showtime.setPrice(new BigDecimal("200.00"));
        showtime.setTotalSeats(500);
        return showtimeService.createShowtime(showtime);
    }
}
```

## 📊 票务场景测试清单

### 核心业务场景测试

- ✅ 创建演出场次
- ✅ 扣减演出库存（乐观锁）
- ✅ 并发扣减库存测试
- ✅ 创建订单
- ✅ 订单支付
- ✅ 取消过期订单
- ✅ 批量生成电子票
- ✅ 分页查询订单
- ✅ 事务回滚测试

## 📚 相关文档

- [模块 README](./README.md)
- [使用示例 (EXAMPLE.md)](./EXAMPLE.md) - 包含完整票务场景代码示例
- [配置指南 (CONFIG.md)](./CONFIG.md) - 包含票务系统配置示例
- [发展路线图 (ROADMAP.md)](./ROADMAP.md)

---

**最后更新**: 2025-11-20  
**文档版本**: v2.0

