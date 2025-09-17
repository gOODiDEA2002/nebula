# Nebula Data Persistence

Nebula框架的数据持久层模块，基于MyBatis-Plus提供高性能的数据访问实现。

## 功能特性

### 🚀 核心功能
- **扩展的BaseMapper**: 在MyBatis-Plus基础上增加更多便捷方法
- **增强的IService**: 提供更丰富的服务层接口
- **自动填充**: 自动填充创建时间、更新时间等字段
- **多数据源支持**: 支持动态配置和管理多个数据源
- **事务管理**: 提供统一的事务管理接口
- **连接池管理**: 支持HikariCP和Druid连接池

### 🛡️ 安全特性
- **防恶意SQL**: 内置SQL注入防护
- **防全表操作**: 阻止无条件的全表更新和删除
- **乐观锁**: 支持乐观锁并发控制
- **数据完整性**: 完整的约束和验证机制

### 📊 性能优化
- **分页查询**: 高效的分页实现
- **批量操作**: 支持批量插入、更新操作
- **连接池优化**: 智能的连接池配置
- **查询优化**: 多种查询方式支持

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-data-persistence</artifactId>
    <version>2.0.0-SNAPSHOT</version>
</dependency>
```

### 2. 配置数据源

```yaml
# application.yml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/nebula?useUnicode=true&characterEncoding=utf-8
    username: root
    password: password

# MyBatis-Plus配置
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
```

### 3. 创建实体类

```java
@Data
@TableName("users")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String username;
    private String email;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    @Version
    private Integer version;
    
    @TableLogic
    private Integer deleted;
}
```

### 4. 创建Mapper接口

```java
@Mapper
public interface UserMapper extends BaseMapper<User> {
    // 继承BaseMapper，获得丰富的CRUD方法
    
    // 自定义查询方法
    @Select("SELECT * FROM users WHERE username = #{username}")
    User findByUsername(@Param("username") String username);
}
```

### 5. 创建Service

```java
public interface UserService extends IService<User> {
    User findByUsername(String username);
}

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    
    @Override
    public User findByUsername(String username) {
        return baseMapper.findByUsername(username);
    }
}
```

## 高级功能

### 多数据源配置

```yaml
nebula:
  data:
    primary: master
    sources:
      master:
        driver-class-name: com.mysql.cj.jdbc.Driver
        url: jdbc:mysql://localhost:3306/master
        username: root
        password: password
        pool:
          min-size: 5
          max-size: 20
      slave:
        driver-class-name: com.mysql.cj.jdbc.Driver
        url: jdbc:mysql://localhost:3306/slave
        username: readonly
        password: password
```

### 事务管理

```java
@Service
public class BusinessService {
    
    @Autowired
    private TransactionManager transactionManager;
    
    public void businessMethod() {
        // 编程式事务
        transactionManager.executeInTransaction(status -> {
            // 业务逻辑
            return result;
        });
        
        // 异步事务
        CompletableFuture<String> future = transactionManager
            .executeInTransactionAsync(status -> {
                // 异步业务逻辑
                return "success";
            });
    }
}
```

### 批量操作

```java
@Service
public class BatchService extends ServiceImpl<UserMapper, User> {
    
    public void batchInsert(List<User> users) {
        // 批量插入
        saveBatch(users);
        
        // 批量插入（忽略重复）
        saveBatchIgnore(users);
        
        // 分批处理大数据量
        batchProcess(users, 1000, batch -> {
            saveBatch(batch);
            return batch.size();
        });
    }
}
```

## 配置选项

### 数据源配置

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `nebula.data.enabled` | `true` | 是否启用数据持久层 |
| `nebula.data.primary` | `primary` | 主数据源名称 |
| `nebula.data.sources.*` | - | 数据源配置 |

### 连接池配置

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `pool.min-size` | `5` | 最小连接数 |
| `pool.max-size` | `20` | 最大连接数 |
| `pool.connection-timeout` | `30s` | 连接超时时间 |
| `pool.idle-timeout` | `10m` | 空闲超时时间 |
| `pool.max-lifetime` | `30m` | 连接最大存活时间 |

## 最佳实践

### 1. 实体设计
- 使用`@TableLogic`实现逻辑删除
- 使用`@Version`实现乐观锁
- 合理使用`@TableField(fill = FieldFill.INSERT)`自动填充

### 2. 查询优化
- 使用分页查询避免大结果集
- 合理使用索引
- 避免N+1查询问题

### 3. 事务管理
- 事务方法要尽量简短
- 合理使用事务传播级别
- 避免在事务中执行耗时操作

### 4. 异常处理
- 使用`DataPersistenceException`包装数据访问异常
- 合理设置事务回滚条件
- 记录详细的错误日志

## 注意事项

1. **版本兼容性**: 需要Spring Boot 3.0+和Java 17+
2. **数据库支持**: 主要支持MySQL，其他数据库需要额外配置
3. **连接池**: 默认使用HikariCP，可配置为Druid
4. **字段填充**: 自动填充功能需要实体类配置相应注解

## 故障排除

### 常见问题

1. **启动失败**: 检查数据库连接配置
2. **事务不生效**: 确认方法上有`@Transactional`注解
3. **自动填充不工作**: 检查实体类字段注解配置
4. **连接池耗尽**: 调整连接池大小或检查连接泄漏

### 日志配置

```yaml
logging:
  level:
    io.nebula.data.persistence: DEBUG
    com.baomidou.mybatisplus: DEBUG
```
