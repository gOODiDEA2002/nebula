# Nebula Data Access 模块

## 概述

`nebula-data-access`是Nebula框架的数据访问抽象层，提供统一的数据访问接口，支持多种存储后端，包括关系型数据库、NoSQL数据库、缓存系统等。

## 核心特性

- 🔌 **统一接口**：提供一致的数据访问API，屏蔽底层存储差异
- 🛠️ **查询构建器**：链式API构建复杂查询条件
- 📦 **仓储模式**：实现Repository模式，封装数据访问逻辑
- 🔄 **事务管理**：统一的事务管理接口
- ⚠️ **异常处理**：完善的异常体系和错误处理
- 🏗️ **扩展性**：易于扩展新的存储后端

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-data-access</artifactId>
    <version>2.0.0</version>
</dependency>
```

### 2. 基础配置

```yaml
nebula:
  data:
    access:
      enabled: true
      default-transaction-timeout: 30s
      enable-query-cache: true
      cache-size: 1000
```

### 3. 实体类定义

```java
@Data
public class User {
    private Long id;
    private String username;
    private String email;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer version; // 乐观锁版本号
    private Boolean deleted = false; // 逻辑删除标记
}
```

## 核心组件

### 1. Repository接口

Repository接口提供基础的CRUD操作：

```java
public interface Repository<T, ID> {
    
    // 基础CRUD操作
    T save(T entity);
    T update(T entity);
    void delete(ID id);
    void deleteEntity(T entity);
    
    T findById(ID id);
    Optional<T> findByIdOptional(ID id);
    List<T> findAll();
    Page<T> findAll(Pageable pageable);
    
    // 条件查询
    List<T> findBy(Query query);
    Page<T> findBy(Query query, Pageable pageable);
    Optional<T> findOneBy(Query query);
    
    // 统计操作
    long count();
    long countBy(Query query);
    boolean exists(ID id);
    boolean existsBy(Query query);
    
    // 批量操作
    List<T> saveAll(Collection<T> entities);
    void deleteAll(Collection<ID> ids);
    void deleteAllEntities(Collection<T> entities);
}
```

### 2. QueryBuilder查询构建器

QueryBuilder提供链式API构建查询条件：

```java
@Service
public class UserService {
    
    @Autowired
    private Repository<User, Long> userRepository;
    
    public List<User> findActiveUsers(String name, Integer minAge) {
        QueryBuilder<User> query = QueryBuilder.create(User.class)
            .eq("deleted", false)
            .like("username", name + "%")
            .ge("age", minAge)
            .orderByDesc("createTime")
            .limit(10);
            
        return userRepository.findBy(query.build());
    }
    
    public Page<User> findUsersByPage(String keyword, Pageable pageable) {
        Query query = QueryBuilder.create(User.class)
            .groupStart()
                .like("username", "%" + keyword + "%")
                .or()
                .like("email", "%" + keyword + "%")
            .groupEnd()
            .eq("deleted", false)
            .orderByDesc("createTime")
            .build();
            
        return userRepository.findBy(query, pageable);
    }
}
```

### 3. 事务管理

提供声明式和编程式事务管理：

```java
@Service
public class UserTransactionService {
    
    @Autowired
    private TransactionManager transactionManager;
    
    @Autowired
    private Repository<User, Long> userRepository;
    
    // 声明式事务
    @Transactional
    public void updateUserWithHistory(User user) {
        userRepository.update(user);
        
        UserHistory history = new UserHistory();
        history.setUserId(user.getId());
        history.setAction("UPDATE");
        userHistoryRepository.save(history);
    }
    
    // 编程式事务
    public void manualTransactionExample() {
        transactionManager.executeInTransaction(() -> {
            User user = userRepository.findById(1L);
            user.setUsername("newName");
            userRepository.update(user);
            
            if (someCondition) {
                throw new BusinessException("Rollback transaction");
            }
            
            return user;
        });
    }
    
    // 异步事务
    public CompletableFuture<User> asyncUpdate(User user) {
        return transactionManager.executeInTransactionAsync(() -> {
            return userRepository.update(user);
        });
    }
}
```

### 4. 异常处理

完善的异常体系处理各种数据访问错误：

```java
@Service
public class UserServiceWithExceptionHandling {
    
    public User findUserSafely(Long id) {
        try {
            return userRepository.findById(id);
        } catch (EntityNotFoundException e) {
            log.warn("User not found: {}", id);
            return null;
        } catch (DataAccessException e) {
            log.error("Data access error when finding user: {}", id, e);
            throw new ServiceException("用户查询失败", e);
        }
    }
    
    public User createUserSafely(User user) {
        try {
            return userRepository.save(user);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("用户名已存在");
        } catch (DataAccessException e) {
            log.error("Failed to create user", e);
            throw new ServiceException("用户创建失败", e);
        }
    }
    
    public User updateUserWithOptimisticLock(User user) {
        try {
            return userRepository.update(user);
        } catch (OptimisticLockException e) {
            throw new BusinessException("数据已被其他用户修改，请刷新后重试");
        }
    }
}
```

## 实现Repository

### 1. 继承AbstractRepository

```java
@Repository
public class UserRepository extends AbstractRepository<User, Long> {
    
    @Autowired
    private UserMapper userMapper;
    
    @Override
    protected Class<User> getEntityClass() {
        return User.class;
    }
    
    @Override
    public User save(User user) {
        if (user.getId() == null) {
            user.setCreateTime(LocalDateTime.now());
            userMapper.insert(user);
        } else {
            user.setUpdateTime(LocalDateTime.now());
            userMapper.updateById(user);
        }
        return user;
    }
    
    @Override
    public User findById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw DataAccessExceptionFactory.entityNotFound("User", id);
        }
        return user;
    }
    
    // 自定义查询方法
    public List<User> findByDepartment(String department) {
        return userMapper.selectList(
            Wrappers.<User>lambdaQuery()
                .eq(User::getDepartment, department)
                .eq(User::getDeleted, false)
        );
    }
    
    public Optional<User> findByUsername(String username) {
        User user = userMapper.selectOne(
            Wrappers.<User>lambdaQuery()
                .eq(User::getUsername, username)
                .eq(User::getDeleted, false)
        );
        return Optional.ofNullable(user);
    }
}
```

### 2. 使用InMemoryRepository

```java
@Configuration
public class RepositoryConfig {
    
    // 用于测试或缓存的内存Repository
    @Bean
    public Repository<CacheData, String> cacheRepository() {
        return new InMemoryRepository<CacheData, String>() {
            @Override
            protected String getId(CacheData entity) {
                return entity.getKey();
            }
        };
    }
}
```

## 查询构建器详解

### 1. 基础查询条件

```java
public class QueryBuilderExamples {
    
    public void basicConditions() {
        QueryBuilder<User> query = QueryBuilder.create(User.class);
        
        // 等值查询
        query.eq("status", "ACTIVE");
        
        // 不等值查询
        query.ne("deleted", true);
        
        // 范围查询
        query.gt("age", 18)
             .lt("age", 65);
             
        // 区间查询
        query.between("createTime", startDate, endDate);
        
        // 模糊查询
        query.like("username", "%admin%");
        
        // 空值查询
        query.isNull("deletedTime")
             .isNotNull("email");
             
        // 包含查询
        query.in("status", Arrays.asList("ACTIVE", "PENDING"));
        query.notIn("role", Arrays.asList("GUEST", "TEMP"));
    }
    
    public void logicalOperators() {
        QueryBuilder<User> query = QueryBuilder.create(User.class);
        
        // AND操作（默认）
        query.eq("department", "IT")
             .and()
             .gt("salary", 50000);
             
        // OR操作
        query.eq("level", "SENIOR")
             .or()
             .gt("experience", 5);
             
        // 复杂分组
        query.groupStart()
                .eq("department", "IT")
                .or()
                .eq("department", "R&D")
             .groupEnd()
             .and()
             .gt("salary", 30000);
    }
    
    public void sortingAndPaging() {
        QueryBuilder<User> query = QueryBuilder.create(User.class);
        
        // 排序
        query.orderByAsc("username")
             .orderByDesc("createTime");
             
        // 分页
        query.page(1, 20);  // 第1页，每页20条
        
        // 限制结果数量
        query.limit(100);
        
        // 偏移量
        query.offset(50);
    }
}
```

### 2. 高级查询功能

```java
@Service
public class AdvancedQueryService {
    
    // 动态查询条件
    public List<User> findUsersWithDynamicConditions(UserSearchRequest request) {
        QueryBuilder<User> query = QueryBuilder.create(User.class);
        
        // 根据条件动态添加查询条件
        if (StringUtils.hasText(request.getUsername())) {
            query.like("username", "%" + request.getUsername() + "%");
        }
        
        if (request.getMinAge() != null) {
            query.ge("age", request.getMinAge());
        }
        
        if (request.getMaxAge() != null) {
            query.le("age", request.getMaxAge());
        }
        
        if (request.getDepartments() != null && !request.getDepartments().isEmpty()) {
            query.in("department", request.getDepartments());
        }
        
        if (request.getCreateTimeStart() != null || request.getCreateTimeEnd() != null) {
            if (request.getCreateTimeStart() != null && request.getCreateTimeEnd() != null) {
                query.between("createTime", request.getCreateTimeStart(), request.getCreateTimeEnd());
            } else if (request.getCreateTimeStart() != null) {
                query.ge("createTime", request.getCreateTimeStart());
            } else {
                query.le("createTime", request.getCreateTimeEnd());
            }
        }
        
        // 默认排序
        query.orderByDesc("createTime");
        
        return userRepository.findBy(query.build());
    }
    
    // 子查询
    public List<Order> findOrdersWithSubQuery() {
        // 查找有订单的用户
        Query userQuery = QueryBuilder.create(User.class)
            .eq("status", "ACTIVE")
            .build();
            
        List<User> activeUsers = userRepository.findBy(userQuery);
        List<Long> userIds = activeUsers.stream()
            .map(User::getId)
            .collect(Collectors.toList());
        
        // 查找这些用户的订单
        Query orderQuery = QueryBuilder.create(Order.class)
            .in("userId", userIds)
            .orderByDesc("createTime")
            .build();
            
        return orderRepository.findBy(orderQuery);
    }
}
```

## 事务管理详解

### 1. 声明式事务

```java
@Service
@Transactional // 类级别事务配置
public class UserTransactionService {
    
    // 继承类级别的事务配置
    public User createUser(User user) {
        return userRepository.save(user);
    }
    
    // 方法级别覆盖类级别配置
    @Transactional(readOnly = true)
    public User findUser(Long id) {
        return userRepository.findById(id);
    }
    
    // 指定传播行为
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void independentOperation(User user) {
        // 这个操作在新事务中执行
        userRepository.save(user);
    }
    
    // 指定回滚条件
    @Transactional(rollbackFor = Exception.class)
    public void operationWithCustomRollback(User user) {
        userRepository.save(user);
        
        if (someBusinessCondition()) {
            throw new BusinessException("Business rule violation");
        }
    }
}
```

### 2. 编程式事务

```java
@Service
public class ProgrammaticTransactionService {
    
    @Autowired
    private TransactionManager transactionManager;
    
    public User complexBusinessOperation(User user) {
        return transactionManager.executeInTransaction(() -> {
            // 第一步：保存用户
            User savedUser = userRepository.save(user);
            
            // 第二步：创建用户档案
            UserProfile profile = new UserProfile();
            profile.setUserId(savedUser.getId());
            profileRepository.save(profile);
            
            // 第三步：发送欢迎邮件（可能失败）
            try {
                emailService.sendWelcomeEmail(savedUser.getEmail());
            } catch (Exception e) {
                log.warn("Failed to send welcome email", e);
                // 不回滚事务，只记录日志
            }
            
            // 第四步：业务规则验证
            if (!businessRuleService.validateUser(savedUser)) {
                throw new BusinessException("User validation failed");
            }
            
            return savedUser;
        });
    }
    
    public void batchOperationWithCheckpoints(List<User> users) {
        int batchSize = 100;
        
        for (int i = 0; i < users.size(); i += batchSize) {
            List<User> batch = users.subList(i, Math.min(i + batchSize, users.size()));
            
            // 每批次一个事务
            transactionManager.executeInTransaction(() -> {
                for (User user : batch) {
                    try {
                        userRepository.save(user);
                    } catch (Exception e) {
                        log.error("Failed to save user: {}", user.getId(), e);
                        // 记录失败但不中断批次
                    }
                }
                return null;
            });
        }
    }
}
```

### 3. 异步事务

```java
@Service
public class AsyncTransactionService {
    
    @Autowired
    private TransactionManager transactionManager;
    
    public CompletableFuture<User> createUserAsync(User user) {
        return transactionManager.executeInTransactionAsync(() -> {
            User savedUser = userRepository.save(user);
            
            // 模拟耗时操作
            Thread.sleep(1000);
            
            return savedUser;
        });
    }
    
    public void batchCreateUsersAsync(List<User> users) {
        List<CompletableFuture<User>> futures = users.stream()
            .map(this::createUserAsync)
            .collect(Collectors.toList());
            
        // 等待所有异步操作完成
        CompletableFuture<Void> allOf = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0])
        );
        
        allOf.thenRun(() -> {
            log.info("All users created successfully");
        }).exceptionally(throwable -> {
            log.error("Some users failed to create", throwable);
            return null;
        });
    }
}
```

## 异常处理最佳实践

### 1. 异常分类和处理

```java
@Service
public class UserServiceWithProperExceptionHandling {
    
    public User createUser(UserCreateRequest request) {
        try {
            // 验证输入
            validateUserRequest(request);
            
            User user = new User();
            user.setUsername(request.getUsername());
            user.setEmail(request.getEmail());
            
            return userRepository.save(user);
            
        } catch (ValidationException e) {
            // 输入验证异常
            log.warn("Invalid user input: {}", e.getMessage());
            throw new BadRequestException("用户输入无效: " + e.getMessage());
            
        } catch (DuplicateKeyException e) {
            // 唯一键冲突
            log.info("Duplicate user creation attempt: {}", request.getUsername());
            throw new ConflictException("用户名或邮箱已存在");
            
        } catch (DataAccessException e) {
            // 数据访问异常
            log.error("Database error while creating user", e);
            throw new InternalServerException("用户创建失败，请稍后重试");
        }
    }
    
    public User updateUser(Long id, UserUpdateRequest request) {
        try {
            User existingUser = userRepository.findById(id);
            
            // 乐观锁检查
            if (!Objects.equals(existingUser.getVersion(), request.getVersion())) {
                throw new OptimisticLockException("Data has been modified by another user");
            }
            
            // 更新字段
            existingUser.setEmail(request.getEmail());
            existingUser.setVersion(request.getVersion() + 1);
            
            return userRepository.update(existingUser);
            
        } catch (EntityNotFoundException e) {
            throw new NotFoundException("用户不存在");
            
        } catch (OptimisticLockException e) {
            throw new ConflictException("数据已被其他用户修改，请刷新后重试");
            
        } catch (DataAccessException e) {
            log.error("Database error while updating user: {}", id, e);
            throw new InternalServerException("用户更新失败");
        }
    }
    
    private void validateUserRequest(UserCreateRequest request) {
        if (StringUtils.isEmpty(request.getUsername())) {
            throw new ValidationException("用户名不能为空");
        }
        
        if (!EmailUtils.isValidEmail(request.getEmail())) {
            throw new ValidationException("邮箱格式无效");
        }
        
        if (request.getUsername().length() < 3) {
            throw new ValidationException("用户名至少3个字符");
        }
    }
}
```

### 2. 全局异常处理

```java
@ControllerAdvice
public class DataAccessExceptionHandler {
    
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(EntityNotFoundException e) {
        ErrorResponse error = ErrorResponse.builder()
            .code("ENTITY_NOT_FOUND")
            .message(e.getMessage())
            .timestamp(LocalDateTime.now())
            .build();
            
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateKey(DuplicateKeyException e) {
        ErrorResponse error = ErrorResponse.builder()
            .code("DUPLICATE_KEY")
            .message("资源已存在")
            .details(e.getMessage())
            .timestamp(LocalDateTime.now())
            .build();
            
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
    
    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(OptimisticLockException e) {
        ErrorResponse error = ErrorResponse.builder()
            .code("OPTIMISTIC_LOCK_FAILURE")
            .message("数据已被修改，请刷新后重试")
            .timestamp(LocalDateTime.now())
            .build();
            
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
    
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccess(DataAccessException e) {
        log.error("Data access error", e);
        
        ErrorResponse error = ErrorResponse.builder()
            .code("DATA_ACCESS_ERROR")
            .message("数据访问失败")
            .timestamp(LocalDateTime.now())
            .build();
            
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
```

## 性能优化

### 1. 查询优化

```java
@Service
public class OptimizedQueryService {
    
    // 使用索引友好的查询
    public List<User> findUsersByIndexedFields(String department, String status) {
        return QueryBuilder.create(User.class)
            .eq("department", department)  // 假设department有索引
            .eq("status", status)          // 假设status有索引
            .orderByDesc("id")            // 使用主键排序
            .build();
    }
    
    // 分页查询优化
    public Page<User> findUsersOptimized(Pageable pageable) {
        // 先查询总数（可以缓存）
        long total = userRepository.count();
        
        if (total == 0) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }
        
        // 再查询当前页数据
        Query query = QueryBuilder.create(User.class)
            .eq("deleted", false)
            .orderByDesc("createTime")
            .page(pageable.getPageNumber(), pageable.getPageSize())
            .build();
            
        List<User> users = userRepository.findBy(query);
        return new PageImpl<>(users, pageable, total);
    }
    
    // 批量查询优化
    public Map<Long, User> findUsersByIdsAsMap(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        
        // 分批查询，避免IN子句过长
        List<User> allUsers = new ArrayList<>();
        int batchSize = 1000;
        
        for (int i = 0; i < userIds.size(); i += batchSize) {
            List<Long> batch = userIds.subList(i, Math.min(i + batchSize, userIds.size()));
            
            Query query = QueryBuilder.create(User.class)
                .in("id", batch)
                .build();
                
            allUsers.addAll(userRepository.findBy(query));
        }
        
        // 转换为Map便于查找
        return allUsers.stream()
            .collect(Collectors.toMap(User::getId, user -> user));
    }
}
```

### 2. 缓存集成

```java
@Service
public class CachedUserService {
    
    @Autowired
    private Repository<User, Long> userRepository;
    
    @Cacheable(value = "users", key = "#id")
    public User findUserById(Long id) {
        return userRepository.findById(id);
    }
    
    @CacheEvict(value = "users", key = "#user.id")
    public User updateUser(User user) {
        return userRepository.update(user);
    }
    
    @Caching(evict = {
        @CacheEvict(value = "users", key = "#user.id"),
        @CacheEvict(value = "userList", allEntries = true)
    })
    public void deleteUser(User user) {
        userRepository.deleteEntity(user);
    }
    
    // 缓存预热
    @PostConstruct
    public void warmUpCache() {
        List<User> activeUsers = QueryBuilder.create(User.class)
            .eq("status", "ACTIVE")
            .limit(1000)
            .build();
            
        activeUsers.forEach(user -> 
            cacheManager.getCache("users").put(user.getId(), user)
        );
    }
}
```

## 测试支持

### 1. 单元测试

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock
    private Repository<User, Long> userRepository;
    
    @InjectMocks
    private UserService userService;
    
    @Test
    void testFindUserById() {
        // Given
        Long userId = 1L;
        User expectedUser = new User();
        expectedUser.setId(userId);
        expectedUser.setUsername("testuser");
        
        when(userRepository.findById(userId)).thenReturn(expectedUser);
        
        // When
        User actualUser = userService.findUserById(userId);
        
        // Then
        assertThat(actualUser).isNotNull();
        assertThat(actualUser.getId()).isEqualTo(userId);
        assertThat(actualUser.getUsername()).isEqualTo("testuser");
        
        verify(userRepository).findById(userId);
    }
    
    @Test
    void testCreateUser() {
        // Given
        User userToCreate = new User();
        userToCreate.setUsername("newuser");
        userToCreate.setEmail("newuser@example.com");
        
        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setUsername("newuser");
        savedUser.setEmail("newuser@example.com");
        
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        
        // When
        User result = userService.createUser(userToCreate);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getUsername()).isEqualTo("newuser");
        
        verify(userRepository).save(userToCreate);
    }
}
```

### 2. 集成测试

```java
@SpringBootTest
@TestPropertySource(properties = {
    "nebula.data.access.enabled=true"
})
class UserRepositoryIntegrationTest {
    
    @Autowired
    private Repository<User, Long> userRepository;
    
    @Test
    @Transactional
    void testSaveAndFindUser() {
        // Given
        User user = new User();
        user.setUsername("integrationtest");
        user.setEmail("test@example.com");
        user.setCreateTime(LocalDateTime.now());
        
        // When
        User savedUser = userRepository.save(user);
        User foundUser = userRepository.findById(savedUser.getId());
        
        // Then
        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getUsername()).isEqualTo("integrationtest");
        assertThat(foundUser.getEmail()).isEqualTo("test@example.com");
    }
    
    @Test
    @Transactional
    void testQueryBuilder() {
        // Given - 创建测试数据
        List<User> testUsers = Arrays.asList(
            createUser("user1", "user1@test.com", 25),
            createUser("user2", "user2@test.com", 30),
            createUser("user3", "user3@test.com", 35)
        );
        
        userRepository.saveAll(testUsers);
        
        // When
        Query query = QueryBuilder.create(User.class)
            .ge("age", 28)
            .like("email", "%@test.com")
            .orderByDesc("age")
            .build();
            
        List<User> result = userRepository.findBy(query);
        
        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getUsername()).isEqualTo("user3");
        assertThat(result.get(1).getUsername()).isEqualTo("user2");
    }
    
    private User createUser(String username, String email, Integer age) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setAge(age);
        user.setCreateTime(LocalDateTime.now());
        return user;
    }
}
```

## 最佳实践

1. **Repository设计**：保持Repository接口简洁，复杂业务逻辑放在Service层
2. **查询优化**：合理使用索引，避免N+1查询问题
3. **事务边界**：事务边界应该在Service层，避免过长的事务
4. **异常处理**：使用特定的异常类型，提供有意义的错误信息
5. **性能监控**：监控慢查询，定期优化数据访问性能

通过以上文档和示例，你可以充分利用Nebula Data Access模块提供的强大功能，构建高效、可维护的数据访问层。
