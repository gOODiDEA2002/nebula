# Nebula Data Persistence 模块

## 概述

`nebula-data-persistence`是Nebula框架的关系型数据库持久化模块，基于MyBatis-Plus构建，提供强大的ORM功能、读写分离、分库分表、事务管理等企业级特性。

## 核心特性

- 🚀 **MyBatis-Plus集成**：提供增强的CRUD操作和代码生成
- 🔀 **读写分离**：支持主从数据库的智能路由
- 📊 **分库分表**：集成ShardingSphere，支持水平/垂直分片
- 🔄 **事务管理**：声明式和编程式事务支持
- 📋 **代码生成**：自动生成Entity、Mapper、Service代码
- 🔍 **条件构造器**：类型安全的动态SQL构建
- 📈 **性能监控**：SQL监控、慢查询分析、性能统计

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-data-persistence</artifactId>
    <version>2.0.0</version>
</dependency>

<!-- MySQL驱动 -->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
</dependency>
```

### 2. 基础配置

```yaml
nebula:
  data:
    persistence:
      enabled: true
      
    # 数据源配置
    sources:
      primary:
        type: mysql
        url: jdbc:mysql://localhost:3306/nebula_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
        username: root
        password: password

# MyBatis-Plus配置
mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      id-type: ASSIGN_ID
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
```

### 3. 实体类定义

```java
@Data
@TableName("users")
public class User extends Model<User> {
    
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    @TableField("username")
    private String username;
    
    @TableField("email")
    private String email;
    
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer deleted;
    
    @Version
    private Integer version;
}
```

## 核心组件

### 1. BaseMapper接口

```java
@Mapper
public interface UserMapper extends BaseMapper<User> {
    
    // 自定义查询方法
    @Select("SELECT * FROM users WHERE department = #{department}")
    List<User> findByDepartment(@Param("department") String department);
    
    // 复杂查询
    List<UserWithStats> selectUsersWithStats();
}
```

### 2. Service层封装

```java
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    
    public List<User> findActiveUsersByDepartment(String department) {
        return list(Wrappers.<User>lambdaQuery()
            .eq(User::getDepartment, department)
            .eq(User::getDeleted, 0)
            .orderByDesc(User::getCreateTime));
    }
    
    public Page<User> searchUsers(UserSearchRequest request, Page<User> page) {
        LambdaQueryWrapper<User> wrapper = Wrappers.<User>lambdaQuery();
        
        wrapper.like(StringUtils.isNotBlank(request.getKeyword()), 
                    User::getUsername, request.getKeyword())
               .eq(StringUtils.isNotBlank(request.getDepartment()), 
                   User::getDepartment, request.getDepartment());
                       
        return page(page, wrapper);
    }
}
```

### 3. 事务管理

```java
@Service
@Transactional
public class UserTransactionService {
    
    @Transactional(rollbackFor = Exception.class)
    public User createUser(UserCreateRequest request) {
        // 创建用户
        User user = new User();
        user.setUsername(request.getUsername());
        userService.save(user);
        
        // 创建用户档案
        UserProfile profile = new UserProfile();
        profile.setUserId(user.getId());
        userProfileService.save(profile);
        
        return user;
    }
    
    @Transactional(readOnly = true)
    public UserDetailVO getUserDetail(Long userId) {
        User user = userService.getById(userId);
        UserProfile profile = userProfileService.getByUserId(userId);
        return new UserDetailVO(user, profile);
    }
}
```

## 高级功能

### 1. 读写分离

详细配置和使用请参考：[读写分离使用指南](src/main/resources/META-INF/spring/read-write-separation-usage.md)

```yaml
nebula:
  data:
    read-write-separation:
      enabled: true
      clusters:
        default:
          master: master
          slaves: [slave1, slave2]
          load-balance-strategy: ROUND_ROBIN
```

```java
@Service
public class ReadWriteService {
    
    @ReadDataSource
    public List<User> findUsers() {
        return userService.list();
    }
    
    @WriteDataSource
    public User createUser(User user) {
        return userService.save(user) ? user : null;
    }
}
```

### 2. 分库分表

详细配置和使用请参考：[ShardingSphere分片使用指南](src/main/resources/META-INF/spring/sharding-sphere-usage.md)

```yaml
nebula:
  data:
    sharding:
      enabled: true
      schemas:
        default:
          data-sources: [ds0, ds1]
          tables:
            - logic-table: t_user
              actual-data-nodes: ds${0..1}.t_user_${0..1}
```

### 3. 分页查询

```java
@RestController
public class UserController {
    
    @GetMapping("/users")
    public Result<IPage<User>> getUsers(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        
        Page<User> page = new Page<>(current, size);
        IPage<User> result = userService.page(page);
        return Result.success(result);
    }
}
```

## 最佳实践

### 1. 实体类设计

```java
// ✅ 好的设计
@Data
@TableName("users")
public class User extends Model<User> {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    @TableField("username")
    @NotBlank(message = "用户名不能为空")
    private String username;
    
    @TableLogic
    private Integer deleted;
    
    @Version
    private Integer version;
}
```

### 2. 查询优化

```java
// ✅ 使用索引字段查询
public List<User> findUsersByDepartment(String department) {
    return userMapper.selectList(
        Wrappers.<User>lambdaQuery()
            .eq(User::getDepartment, department)  // 有索引
            .orderByDesc(User::getId)            // 主键排序
    );
}

// ❌ 避免全表扫描
public List<User> badQuery() {
    return userMapper.selectList(
        Wrappers.<User>lambdaQuery()
            .like(User::getDescription, "%keyword%")  // 前缀通配符
    );
}
```

### 3. 事务使用

```java
// ✅ 合适的事务边界
@Transactional
public void goodTransaction() {
    // 相关的数据操作放在同一事务中
    createUser();
    createUserProfile();
    recordLog();
}

// ❌ 避免长事务
@Transactional
public void badTransaction() {
    // 不要在事务中包含外部API调用、文件I/O等
}
```

## 配置文档

- [读写分离配置示例](src/main/resources/META-INF/spring/read-write-separation-example.yml)
- [ShardingSphere配置示例](src/main/resources/META-INF/spring/sharding-sphere-example.yml)

## 使用指南

- [读写分离使用指南](src/main/resources/META-INF/spring/read-write-separation-usage.md)
- [ShardingSphere使用指南](src/main/resources/META-INF/spring/sharding-sphere-usage.md)

## 性能优化

1. **索引优化**：为常用查询字段创建合适的索引
2. **分页优化**：使用游标分页代替深度分页
3. **批量操作**：使用批量插入/更新代替循环操作
4. **缓存集成**：对热点数据使用缓存
5. **读写分离**：将读操作分发到从库
6. **分库分表**：对大表进行水平分片

## 监控和诊断

1. **SQL监控**：监控慢查询和SQL性能
2. **连接池监控**：监控数据库连接池状态
3. **事务监控**：监控事务执行情况
4. **分片监控**：监控分片路由和性能

通过以上配置和使用方式，你可以充分利用Nebula Persistence模块的强大功能，构建高性能、可扩展的数据持久化层。