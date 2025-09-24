# Nebula读写分离使用指南

## 概述

Nebula框架提供了完善的读写分离功能，支持主从数据库的自动路由，提高数据库的并发性能和读写分离。

## 主要特性

- 🔀 **动态数据源路由**：运行时自动选择读写数据源
- 📝 **注解驱动**：通过@ReadDataSource和@WriteDataSource注解控制
- ⚖️ **负载均衡**：支持多种负载均衡策略
- 🔧 **灵活配置**：支持多集群配置
- 🛡️ **事务安全**：事务中自动使用主库保证数据一致性

## 快速开始

### 1. 配置数据源

```yaml
nebula:
  data:
    sources:
      master:
        url: jdbc:mysql://localhost:3306/master_db
        username: root
        password: password
      slave1:
        url: jdbc:mysql://localhost:3307/slave_db1
        username: root
        password: password
      slave2:
        url: jdbc:mysql://localhost:3308/slave_db2
        username: root
        password: password
    
    read-write-separation:
      enabled: true
      clusters:
        default:
          enabled: true
          master: master
          slaves: [slave1, slave2]
          load-balance-strategy: ROUND_ROBIN
```

### 2. 使用注解

#### 2.1 在Service方法上使用

```java
@Service
public class UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    // 读操作使用从库
    @ReadDataSource
    public User findById(Long id) {
        return userMapper.selectById(id);
    }
    
    // 写操作使用主库
    @WriteDataSource
    public void save(User user) {
        userMapper.insert(user);
    }
    
    // 复杂查询使用从库
    @ReadDataSource
    public List<User> findActiveUsers() {
        return userMapper.selectList(
            Wrappers.<User>lambdaQuery()
                .eq(User::getStatus, "ACTIVE")
        );
    }
    
    // 事务中的操作（自动使用主库）
    @Transactional
    @WriteDataSource
    public void batchUpdate(List<User> users) {
        for (User user : users) {
            userMapper.updateById(user);
        }
    }
}
```

#### 2.2 在类级别使用

```java
@Service
@ReadDataSource  // 类级别默认使用读数据源
public class UserQueryService {
    
    @Autowired
    private UserMapper userMapper;
    
    // 继承类级别的@ReadDataSource，使用从库
    public List<User> findAll() {
        return userMapper.selectList(null);
    }
    
    // 方法级别覆盖类级别，使用主库
    @WriteDataSource
    public void updateLastLoginTime(Long userId) {
        User user = new User();
        user.setId(userId);
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);
    }
}
```

### 3. 编程式使用

```java
@Service
public class UserManagementService {
    
    @Autowired
    private UserMapper userMapper;
    
    public User findUserWithFallback(Long id) {
        // 编程式指定使用读数据源
        return DataSourceContextHolder.executeRead(() -> {
            return userMapper.selectById(id);
        });
    }
    
    public void createUser(User user) {
        // 编程式指定使用写数据源
        DataSourceContextHolder.executeWrite(() -> {
            userMapper.insert(user);
        });
    }
    
    public void complexOperation() {
        // 手动控制数据源切换
        try {
            // 查询操作使用读数据源
            DataSourceContextHolder.setRead();
            List<User> users = userMapper.selectList(null);
            
            // 切换到写数据源进行更新
            DataSourceContextHolder.setWrite();
            for (User user : users) {
                user.setLastAccessTime(LocalDateTime.now());
                userMapper.updateById(user);
            }
        } finally {
            // 清理上下文
            DataSourceContextHolder.clearDataSourceType();
        }
    }
}
```

## 高级配置

### 1. 多集群配置

```yaml
nebula:
  data:
    read-write-separation:
      clusters:
        # 用户业务集群
        user:
          enabled: true
          master: user_master
          slaves: [user_slave1, user_slave2]
          load-balance-strategy: ROUND_ROBIN
        
        # 订单业务集群
        order:
          enabled: true
          master: order_master
          slaves: [order_slave1]
          load-balance-strategy: RANDOM
        
        # 分析业务集群（只读）
        analytics:
          enabled: true
          master: analytics_master
          slaves: [analytics_slave1, analytics_slave2, analytics_slave3]
          load-balance-strategy: WEIGHTED_ROUND_ROBIN
```

```java
@Service
public class OrderService {
    
    // 使用order集群的读数据源
    @ReadDataSource(cluster = "order")
    public Order findById(Long id) {
        return orderMapper.selectById(id);
    }
    
    // 使用order集群的写数据源
    @WriteDataSource(cluster = "order")
    public void save(Order order) {
        orderMapper.insert(order);
    }
}

@Service
public class AnalyticsService {
    
    // 使用analytics集群进行数据分析
    @ReadDataSource(cluster = "analytics")
    public List<ReportData> generateReport() {
        return analyticsMapper.selectReportData();
    }
}
```

### 2. 强制读写分离

```java
@Service
public class ReportService {
    
    // 即使在事务中也强制使用读数据源
    @Transactional
    @ReadDataSource(force = true)
    public ReportData generateRealTimeReport() {
        // 注意：这可能导致读取到旧数据
        return reportMapper.selectRealTimeData();
    }
}
```

### 3. 负载均衡策略

```yaml
nebula:
  data:
    read-write-separation:
      clusters:
        default:
          load-balance-strategy: ROUND_ROBIN    # 轮询（默认）
        high-traffic:
          load-balance-strategy: RANDOM         # 随机
        weighted:
          load-balance-strategy: WEIGHTED_ROUND_ROBIN  # 加权轮询
```

## 监控和管理

### 1. 健康检查

```java
@RestController
@RequestMapping("/admin/datasource")
public class DataSourceHealthController {
    
    @Autowired
    private ReadWriteDataSourceManager readWriteManager;
    
    @GetMapping("/health")
    public Map<String, Boolean> checkHealth() {
        return readWriteManager.healthCheck();
    }
    
    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        return readWriteManager.getClusterStats();
    }
}
```

### 2. 数据源状态监控

```java
@Component
public class DataSourceMonitor {
    
    @Autowired
    private ReadWriteDataSourceManager readWriteManager;
    
    @EventListener
    @Async
    public void onDataSourceSwitch(DataSourceSwitchEvent event) {
        log.info("Data source switched: {} -> {}", 
                event.getFrom(), event.getTo());
    }
    
    @Scheduled(fixedRate = 60000) // 每分钟检查一次
    public void monitorDataSourceHealth() {
        Map<String, Boolean> healthStatus = readWriteManager.healthCheck();
        healthStatus.forEach((name, healthy) -> {
            if (!healthy) {
                log.warn("Data source {} is unhealthy", name);
                // 发送告警通知
            }
        });
    }
}
```

## 最佳实践

### 1. 事务处理

```java
@Service
public class OrderService {
    
    // ✅ 正确：事务中的读写操作都会使用主库
    @Transactional
    public void processOrder(Order order) {
        // 这些操作都在主库上执行，保证一致性
        order.setStatus("PROCESSING");
        orderMapper.updateById(order);
        
        OrderHistory history = new OrderHistory();
        history.setOrderId(order.getId());
        history.setAction("PROCESS");
        historyMapper.insert(history);
    }
    
    // ❌ 错误：不要在事务中强制使用读数据源
    @Transactional
    @ReadDataSource(force = true)  // 危险！可能读到旧数据
    public void badExample(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        // 可能读到更新前的数据，导致业务逻辑错误
    }
}
```

### 2. 读写分离策略

```java
@Service
public class UserService {
    
    // ✅ 查询操作使用读数据源
    @ReadDataSource
    public List<User> findActiveUsers() {
        return userMapper.selectActiveUsers();
    }
    
    // ✅ 写操作使用写数据源
    @WriteDataSource
    public void updateUserStatus(Long userId, String status) {
        userMapper.updateStatus(userId, status);
    }
    
    // ✅ 复杂业务操作，明确指定数据源
    public void processUserLogin(Long userId) {
        // 更新最后登录时间（写操作）
        DataSourceContextHolder.executeWrite(() -> {
            userMapper.updateLastLoginTime(userId, LocalDateTime.now());
        });
        
        // 查询用户权限（读操作）
        List<Permission> permissions = DataSourceContextHolder.executeRead(() -> {
            return permissionMapper.selectByUserId(userId);
        });
        
        // 处理业务逻辑...
    }
}
```

### 3. 异常处理

```java
@Service
public class DataService {
    
    public void robustDataOperation() {
        try {
            DataSourceContextHolder.setRead();
            
            // 执行读操作
            List<Data> data = dataMapper.selectAll();
            
            // 处理数据...
            
        } catch (Exception e) {
            log.error("读操作失败，尝试使用主库", e);
            
            // 降级到主库
            DataSourceContextHolder.setWrite();
            List<Data> data = dataMapper.selectAll();
            
        } finally {
            // 确保清理上下文
            DataSourceContextHolder.clearDataSourceType();
        }
    }
}
```

## 故障排除

### 1. 常见问题

#### 问题1：读写分离没有生效
**可能原因**：
- 配置未启用
- AOP切面未生效
- 注解使用不正确

**解决方案**：
```yaml
# 确保配置正确
nebula:
  data:
    read-write-separation:
      enabled: true
      aspect-enabled: true
```

#### 问题2：事务中读取到旧数据
**原因**：事务隔离级别或主从延迟导致

**解决方案**：
```java
// 在事务中强制使用主库
@Transactional
@ReadDataSource(force = false)  // 不强制，保证一致性
public void safeTransactionRead() {
    // 这会使用主库
}
```

#### 问题3：从库连接失败
**解决方案**：自动降级到主库

```java
// 框架会自动处理从库不可用的情况，降级到主库
@ReadDataSource
public List<User> findUsers() {
    // 如果从库不可用，自动使用主库
    return userMapper.selectList(null);
}
```

### 2. 调试技巧

```java
// 启用调试日志
@Slf4j
@Service
public class DebugService {
    
    public void debugDataSourceRouting() {
        log.info("当前数据源上下文: {}", DataSourceContextHolder.getContextInfo());
        
        DataSourceContextHolder.setRead();
        log.info("设置为读数据源: {}", DataSourceContextHolder.getContextInfo());
        
        // 执行操作...
        
        DataSourceContextHolder.clearDataSourceType();
        log.info("清理后: {}", DataSourceContextHolder.getContextInfo());
    }
}
```

## 性能优化

1. **连接池配置**：根据读写比例调整主从库连接池大小
2. **负载均衡**：选择合适的负载均衡策略
3. **监控告警**：及时发现和处理数据源问题
4. **缓存策略**：配合缓存减少数据库压力

通过以上配置和使用方式，你可以充分利用Nebula框架的读写分离功能，提高应用的数据库性能和可用性。
