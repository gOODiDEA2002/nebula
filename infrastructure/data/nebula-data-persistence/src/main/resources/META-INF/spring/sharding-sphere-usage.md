# Nebula ShardingSphere分片使用指南

## 概述

Nebula框架集成了Apache ShardingSphere，提供强大的分库分表功能，支持水平分片、垂直分片、读写分离和分布式事务。

## 主要特性

- 🔀 **水平分片**：按业务规则将数据分散到多个数据库和表
- 📊 **垂直分片**：按表的维度分散到不同数据库
- ⚖️ **读写分离**：与分片结合的读写分离
- 🔧 **灵活配置**：支持多种分片策略和算法
- 🛡️ **分布式事务**：支持分布式环境下的事务一致性
- 📈 **透明路由**：应用层无感知的SQL路由

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>org.apache.shardingsphere</groupId>
    <artifactId>shardingsphere-jdbc-core-spring-boot-starter</artifactId>
    <version>5.4.0</version>
</dependency>
```

### 2. 基础配置

```yaml
nebula:
  data:
    sources:
      ds0:
        url: jdbc:mysql://localhost:3306/sharding_db0
        username: root
        password: password
      ds1:
        url: jdbc:mysql://localhost:3306/sharding_db1
        username: root
        password: password
    
    sharding:
      enabled: true
      schemas:
        default:
          data-sources: [ds0, ds1]
          tables:
            - logic-table: t_user
              actual-data-nodes: ds${0..1}.t_user_${0..1}
              database-sharding-config:
                sharding-column: user_id
                algorithm-expression: ds${user_id % 2}
              table-sharding-config:
                sharding-column: user_id
                algorithm-expression: t_user_${user_id % 2}
```

### 3. 实体类定义

```java
@Data
@TableName("t_user")
public class User {
    
    @TableId(type = IdType.ASSIGN_ID) // 使用ShardingSphere的ID生成
    private Long id;
    
    private Long userId;    // 分片键
    private String username;
    private String email;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer deleted;
}
```

### 4. 使用示例

```java
@Service
public class UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    // 插入操作会自动路由到正确的分片
    public void createUser(User user) {
        user.setUserId(generateUserId());
        userMapper.insert(user);
    }
    
    // 查询操作会根据分片键路由
    public User findByUserId(Long userId) {
        return userMapper.selectOne(
            Wrappers.<User>lambdaQuery()
                .eq(User::getUserId, userId)
        );
    }
    
    // 范围查询可能涉及多个分片
    public List<User> findUsersByRange(Long startUserId, Long endUserId) {
        return userMapper.selectList(
            Wrappers.<User>lambdaQuery()
                .between(User::getUserId, startUserId, endUserId)
        );
    }
}
```

## 分片策略详解

### 1. 标准分片策略（StandardShardingStrategy）

最常用的分片策略，支持`=`、`IN`、`BETWEEN AND`操作。

```yaml
# 用户表按用户ID分片
- logic-table: t_user
  actual-data-nodes: ds${0..1}.t_user_${0..3}
  database-sharding-config:
    sharding-column: user_id
    algorithm-expression: ds${user_id % 2}  # 分库算法
  table-sharding-config:
    sharding-column: user_id
    algorithm-expression: t_user_${user_id % 4}  # 分表算法
```

### 2. 复合分片策略（ComplexShardingStrategy）

支持多个分片键的复杂分片逻辑。

```java
// 自定义复合分片算法
public class OrderComplexShardingAlgorithm implements ComplexKeysShardingAlgorithm<String> {
    
    @Override
    public Collection<String> doSharding(Collection<String> availableTargetNames, 
                                       ComplexKeysShardingValue<String> shardingValue) {
        // 根据用户ID和订单时间进行分片
        Map<String, Collection<String>> columnNameAndShardingValuesMap = shardingValue.getColumnNameAndShardingValuesMap();
        
        Collection<String> userIds = columnNameAndShardingValuesMap.get("user_id");
        Collection<String> orderDates = columnNameAndShardingValuesMap.get("order_date");
        
        Set<String> result = new HashSet<>();
        
        for (String userId : userIds) {
            for (String orderDate : orderDates) {
                // 自定义分片逻辑
                String targetName = calculateTarget(availableTargetNames, userId, orderDate);
                result.add(targetName);
            }
        }
        
        return result;
    }
}
```

### 3. 时间范围分片策略

按时间维度进行分片，适用于日志、订单等按时间增长的数据。

```yaml
# 订单表按时间分片
- logic-table: t_order
  actual-data-nodes: ds${0..1}.t_order_${202401..202412}
  database-sharding-config:
    sharding-column: user_id
    algorithm-expression: ds${user_id % 2}
  table-sharding-config:
    sharding-column: create_time
    algorithm-name: order-date-range
```

```java
// 使用示例
@Service
public class OrderService {
    
    public void createOrder(Order order) {
        order.setCreateTime(LocalDateTime.now());
        orderMapper.insert(order);  // 自动路由到当前月份的表
    }
    
    public List<Order> findOrdersByMonth(Long userId, YearMonth month) {
        LocalDateTime start = month.atDay(1).atStartOfDay();
        LocalDateTime end = month.atEndOfMonth().atTime(23, 59, 59);
        
        return orderMapper.selectList(
            Wrappers.<Order>lambdaQuery()
                .eq(Order::getUserId, userId)
                .between(Order::getCreateTime, start, end)
        );
    }
}
```

## 绑定表配置

绑定表是指分片规则一致的主表和子表，例如订单表和订单明细表。

```yaml
# 订单表
- logic-table: t_order
  actual-data-nodes: ds${0..1}.t_order_${0..3}
  table-sharding-config:
    sharding-column: order_id
    algorithm-expression: t_order_${order_id % 4}

# 订单明细表（绑定表）
- logic-table: t_order_item
  actual-data-nodes: ds${0..1}.t_order_item_${0..3}
  table-sharding-config:
    sharding-column: order_id
    algorithm-expression: t_order_item_${order_id % 4}
```

```java
// 跨表查询会在同一个分片中执行，避免跨库关联
@Service
public class OrderService {
    
    public OrderWithItems findOrderWithItems(Long orderId) {
        // 这个查询会在同一个分片中执行
        Order order = orderMapper.selectById(orderId);
        List<OrderItem> items = orderItemMapper.selectList(
            Wrappers.<OrderItem>lambdaQuery()
                .eq(OrderItem::getOrderId, orderId)
        );
        
        return new OrderWithItems(order, items);
    }
}
```

## 广播表配置

广播表是指在所有分片中都存在的表，通常用于字典表、配置表等。

```yaml
# 字典表（广播表）
broadcast-tables: [t_dict, t_config, t_region]
```

```java
@Service
public class DictService {
    
    // 广播表的查询会在任意一个分片执行
    public List<Dict> findAllDicts() {
        return dictMapper.selectList(null);
    }
    
    // 广播表的写操作会在所有分片执行
    public void updateDict(Dict dict) {
        dictMapper.updateById(dict);  // 会在所有分片中更新
    }
}
```

## 分布式主键配置

### 1. 雪花算法（推荐）

```yaml
key-generate-config:
  column: id
  algorithm-name: snowflake
  algorithm-properties:
    worker-id: 1
    max-vibration-offset: 1
```

### 2. UUID算法

```yaml
key-generate-config:
  column: id
  algorithm-name: uuid
```

### 3. 自定义主键生成

```java
public class CustomKeyGenerator implements KeyGenerateAlgorithm {
    
    @Override
    public Comparable<?> generateKey() {
        // 自定义主键生成逻辑
        return System.currentTimeMillis() + RandomUtils.nextInt(1000, 9999);
    }
    
    @Override
    public String getType() {
        return "CUSTOM";
    }
}
```

## 读写分离与分片结合

```yaml
sharding:
  schemas:
    default:
      data-sources: [ds0, ds1]
      read-write-separation-enabled: true
      
      read-write-separation:
        data-sources:
          ds0:
            write-data-source: ds0_master
            read-data-sources: [ds0_slave1, ds0_slave2]
            load-balance-algorithm: ROUND_ROBIN
          ds1:
            write-data-source: ds1_master
            read-data-sources: [ds1_slave1, ds1_slave2]
            load-balance-algorithm: RANDOM
```

## 分布式事务

### 1. 本地事务（推荐用于简单场景）

```java
@Service
@Transactional
public class UserService {
    
    // 在同一个分片内的事务操作
    public void updateUserProfile(Long userId, UserProfile profile) {
        User user = userMapper.selectByUserId(userId);
        user.setProfile(profile);
        userMapper.updateById(user);
        
        // 记录操作日志（如果在同一分片）
        UserLog log = new UserLog();
        log.setUserId(userId);
        log.setAction("UPDATE_PROFILE");
        userLogMapper.insert(log);
    }
}
```

### 2. XA事务（强一致性）

```yaml
spring:
  shardingsphere:
    props:
      xa-transaction-manager-type: Atomikos
```

```java
@Service
public class OrderService {
    
    @Transactional
    @ShardingTransactionType(TransactionType.XA)
    public void createOrderWithInventory(Order order, List<OrderItem> items) {
        // 跨分片的强一致性事务
        orderMapper.insert(order);
        
        for (OrderItem item : items) {
            orderItemMapper.insert(item);
            inventoryMapper.updateStock(item.getProductId(), item.getQuantity());
        }
    }
}
```

### 3. BASE事务（最终一致性）

```java
@Service
public class OrderService {
    
    @Transactional
    @ShardingTransactionType(TransactionType.BASE)
    public void createOrderAsync(Order order) {
        // 适用于最终一致性场景
        orderMapper.insert(order);
        
        // 异步处理库存扣减
        inventoryService.reduceStockAsync(order.getItems());
    }
}
```

## 性能优化

### 1. 分片键选择原则

- **高基数**：分片键应该有足够的不同值
- **均匀分布**：避免数据倾斜
- **业务相关**：与主要查询条件一致

```java
// ✅ 好的分片键选择
public class User {
    private Long userId;     // 用户ID，分布均匀，查询频繁
}

// ❌ 不好的分片键选择
public class User {
    private Integer gender;  // 性别，只有2个值，会导致数据倾斜
}
```

### 2. SQL优化

```java
// ✅ 带分片键的查询（单分片路由）
List<Order> orders = orderMapper.selectList(
    Wrappers.<Order>lambdaQuery()
        .eq(Order::getUserId, userId)  // 包含分片键
        .eq(Order::getStatus, "PAID")
);

// ❌ 不带分片键的查询（全分片路由）
List<Order> orders = orderMapper.selectList(
    Wrappers.<Order>lambdaQuery()
        .eq(Order::getStatus, "PAID")  // 缺少分片键，性能差
);
```

### 3. 批量操作优化

```java
@Service
public class UserService {
    
    // 按分片键分组进行批量操作
    public void batchUpdateUsers(List<User> users) {
        Map<String, List<User>> usersByShard = users.stream()
            .collect(Collectors.groupingBy(user -> 
                calculateShard(user.getUserId())));
        
        for (Map.Entry<String, List<User>> entry : usersByShard.entrySet()) {
            // 同一分片的数据批量处理
            userMapper.batchUpdate(entry.getValue());
        }
    }
    
    private String calculateShard(Long userId) {
        return "ds" + (userId % 2);
    }
}
```

## 监控和管理

### 1. SQL监控

```yaml
spring:
  shardingsphere:
    props:
      sql-show: true        # 显示执行的SQL
      sql-simple: true      # 简化SQL显示
```

### 2. 分片统计

```java
@RestController
@RequestMapping("/admin/sharding")
public class ShardingMonitorController {
    
    @Autowired
    private ShardingSphereManager shardingSphereManager;
    
    @GetMapping("/stats")
    public Map<String, Object> getShardingStats() {
        return shardingSphereManager.getShardingStats();
    }
    
    @GetMapping("/health")
    public Map<String, Boolean> checkHealth() {
        return shardingSphereManager.healthCheck();
    }
}
```

### 3. 性能监控

```java
@Component
public class ShardingMetrics {
    
    private final MeterRegistry meterRegistry;
    
    public ShardingMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
    
    @EventListener
    public void onSqlExecution(SqlExecutionEvent event) {
        Timer.Sample sample = Timer.start(meterRegistry);
        sample.stop(Timer.builder("sharding.sql.execution")
            .tag("operation", event.getOperationType())
            .tag("shard", event.getShardName())
            .register(meterRegistry));
    }
}
```

## 最佳实践

### 1. 分片设计原则

- **垂直优先**：优先考虑垂直分片（按业务模块分库）
- **水平补充**：在垂直分片基础上进行水平分片
- **避免跨片**：尽量避免跨分片的关联查询和事务

### 2. 分片键设计

```java
// ✅ 推荐：复合分片键
public class Order {
    private Long userId;    // 租户ID，用于分库
    private Long orderId;   // 订单ID，用于分表
    private LocalDateTime createTime; // 时间维度，用于归档
}

// 分片配置
database-sharding-config:
  sharding-column: user_id
  algorithm-expression: ds${user_id % 4}

table-sharding-config:
  sharding-column: order_id  
  algorithm-expression: t_order_${order_id % 8}
```

### 3. 查询优化

```java
@Service
public class OrderQueryService {
    
    // ✅ 单分片查询
    public Order findOrderById(Long userId, Long orderId) {
        return orderMapper.selectOne(
            Wrappers.<Order>lambdaQuery()
                .eq(Order::getUserId, userId)    // 分库键
                .eq(Order::getOrderId, orderId)  // 分表键
        );
    }
    
    // ✅ 分页查询（带分片键）
    public IPage<Order> findUserOrders(Long userId, Page<Order> page) {
        return orderMapper.selectPage(page,
            Wrappers.<Order>lambdaQuery()
                .eq(Order::getUserId, userId)    // 必须包含分片键
                .orderByDesc(Order::getCreateTime)
        );
    }
    
    // ❌ 避免：跨分片聚合查询
    public long countAllOrders() {
        // 这会导致性能问题
        return orderMapper.selectCount(null);
    }
}
```

### 4. 事务处理

```java
@Service
public class OrderTransactionService {
    
    // ✅ 单分片事务
    @Transactional
    public void updateOrderInSingleShard(Long userId, Long orderId, String status) {
        Order order = orderMapper.selectByUserIdAndOrderId(userId, orderId);
        order.setStatus(status);
        orderMapper.updateById(order);
        
        // 在同一分片中记录日志
        OrderLog log = new OrderLog();
        log.setUserId(userId);
        log.setOrderId(orderId);
        log.setAction("STATUS_UPDATE");
        orderLogMapper.insert(log);
    }
    
    // ✅ 跨分片事务（使用消息队列）
    public void createOrderWithInventory(Order order) {
        // 1. 创建订单（本地事务）
        orderMapper.insert(order);
        
        // 2. 发送扣减库存消息（异步处理）
        messageService.sendInventoryReductionMessage(order);
    }
}
```

## 故障排除

### 1. 常见问题

#### 问题1：数据倾斜
**现象**：某些分片数据量过大，查询缓慢
**解决方案**：
```java
// 分析数据分布
@RestController
public class ShardingAnalysisController {
    
    @GetMapping("/analysis/distribution")
    public Map<String, Long> analyzeDataDistribution() {
        Map<String, Long> distribution = new HashMap<>();
        
        // 统计各分片数据量
        for (String shard : Arrays.asList("ds0", "ds1", "ds2", "ds3")) {
            Long count = orderMapper.countByShard(shard);
            distribution.put(shard, count);
        }
        
        return distribution;
    }
}
```

#### 问题2：跨分片查询性能差
**解决方案**：
```java
// 使用分片键进行查询优化
public class OptimizedQueryService {
    
    // 分批查询代替全表扫描
    public List<Order> findOrdersByStatus(String status) {
        List<Order> allOrders = new ArrayList<>();
        
        // 按用户ID范围分批查询
        for (int i = 0; i < 1000; i++) {
            List<Order> batchOrders = orderMapper.selectList(
                Wrappers.<Order>lambdaQuery()
                    .eq(Order::getStatus, status)
                    .between(Order::getUserId, i * 1000, (i + 1) * 1000 - 1)
                    .last("limit 100")
            );
            allOrders.addAll(batchOrders);
        }
        
        return allOrders;
    }
}
```

### 2. 调试技巧

```yaml
# 开启SQL日志
logging:
  level:
    org.apache.shardingsphere: DEBUG
    io.nebula.data.persistence: DEBUG

# 显示详细的分片路由信息
spring:
  shardingsphere:
    props:
      sql-show: true
      sql-comment-parse-enabled: true
```

## 迁移指南

### 1. 从单库到分片的迁移

```java
@Component
public class ShardingMigrationService {
    
    // 数据迁移工具
    public void migrateDataToShards() {
        List<User> allUsers = userMapper.selectAllFromOriginalTable();
        
        for (User user : allUsers) {
            // 计算目标分片
            String targetShard = calculateTargetShard(user.getUserId());
            
            // 插入到分片表
            userMapper.insertToShardedTable(user);
            
            // 验证数据完整性
            verifyMigration(user);
        }
    }
}
```

### 2. 渐进式迁移策略

```java
@Service
public class GradualMigrationService {
    
    @Value("${migration.shard-enabled:false}")
    private boolean shardEnabled;
    
    public User findUser(Long userId) {
        if (shardEnabled) {
            // 从分片表查询
            return shardedUserMapper.selectByUserId(userId);
        } else {
            // 从原始表查询
            return originalUserMapper.selectByUserId(userId);
        }
    }
    
    public void saveUser(User user) {
        if (shardEnabled) {
            shardedUserMapper.insert(user);
        } else {
            originalUserMapper.insert(user);
        }
        
        // 双写确保数据一致性
        if (migrationProperties.isDualWrite()) {
            try {
                if (shardEnabled) {
                    originalUserMapper.insert(user);
                } else {
                    shardedUserMapper.insert(user);
                }
            } catch (Exception e) {
                log.warn("Dual write failed", e);
            }
        }
    }
}
```

通过以上配置和使用方式，你可以充分利用Nebula框架集成的ShardingSphere功能，实现高性能的分库分表解决方案。
