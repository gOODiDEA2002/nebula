# Nebula Data Persistence 模块

##  模块简介

`nebula-data-persistence` 是 Nebula 框架的数据持久层模块，提供了统一的数据访问抽象和强大的数据库操作能力该模块基于 MyBatis-Plus 构建，集成了读写分离分库分表等企业级特性

##  功能特性

###  核心功能
- **基础数据访问**: 基于 MyBatis-Plus 的增强型 CRUD 操作
- **读写分离**: 支持主从数据库的读写分离，提高系统性能
- **分库分表**: 基于 ShardingSphere 的分片功能，支持水平扩展
- **事务管理**: 统一的事务管理接口，支持编程式和声明式事务
- **连接池管理**: 集成 HikariCP 高性能连接池

###  增强特性
- **自动配置**: Spring Boot 自动配置，零配置启动
- **类型安全**: 完整的泛型支持和类型安全
- **元数据处理**: 自动填充创建时间更新时间等字段
- **分页查询**: 内置分页插件，简化分页操作
- **性能监控**: 提供数据源健康检查和统计信息

##  快速开始

### 添加依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-data-persistence</artifactId>
    <version>2.0.0-SNAPSHOT</version>
</dependency>
```

### 基础配置

在 `application.yml` 中配置数据源：

```yaml
# 启用 Nebula 数据持久层
nebula:
  data:
    persistence:
      enabled: true
      primary: primary
      sources:
        primary:
          type: mysql
          url: jdbc:mysql://localhost:3306/nebula_example?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
          username: root
          password: password
          driver-class-name: com.mysql.cj.jdbc.Driver
          pool:
            min-size: 5
            max-size: 20
            connection-timeout: 30s
            idle-timeout: 10m
            max-lifetime: 30m

# MyBatis-Plus 配置
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      id-type: auto
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
```

##  基础数据访问功能

### 1. 实体类定义

使用 MyBatis-Plus 注解定义实体类：

```java
@Data
@TableName("t_product")
public class Product {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String name;
    private String description;
    private BigDecimal price;
    private String category;
    private Integer stockQuantity;
    private String status;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    private Boolean deleted;
}
```

### 2. Mapper 接口

继承 Nebula 增强的 BaseMapper：

```java
@Mapper
public interface ProductMapper extends BaseMapper<Product> {
    
    // 自定义查询方法
    @Select("SELECT category, COUNT(*) as count FROM t_product GROUP BY category")
    List<Map<String, Object>> getCategoryStatistics();
}
```

### 3. Service 层

继承 Nebula IService 接口：

```java
public interface ProductService extends IService<Product> {
    
    // 业务方法
    Page<Product> getProductsByCategory(String category, long current, long size);
    
    List<Product> getTopSellingProducts(int limit);
}

@Service
@Transactional
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> 
        implements ProductService {
    
    @Override
    public Page<Product> getProductsByCategory(String category, long current, long size) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getCategory, category)
               .eq(Product::getDeleted, false);
        return findPage(current, size, wrapper);
    }
    
    @Override
    @Cacheable(value = "products", key = "'top-selling:' + #limit")
    public List<Product> getTopSellingProducts(int limit) {
        return findTopN(new LambdaQueryWrapper<Product>()
            .eq(Product::getDeleted, false)
            .orderByDesc(Product::getStockQuantity), limit);
    }
}
```

### 4. Controller 层

```java
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {
    
    private final ProductService productService;
    
    @PostMapping
    public Result<Long> createProduct(@Valid @RequestBody CreateProductDto request) {
        Product product = new Product();
        BeanUtils.copyProperties(request, product);
        productService.save(product);
        return Result.success(product.getId());
    }
    
    @GetMapping("/{id}")
    public Result<Product> getProduct(@PathVariable Long id) {
        return productService.findById(id)
            .map(Result::success)
            .orElse(Result.error("PRODUCT_NOT_FOUND", "产品不存在"));
    }
    
    @GetMapping
    public Result<IPage<Product>> getProducts(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String category) {
        
        Page<Product> result = productService.getProductsByCategory(category, current, size);
        return Result.success(result);
    }
}
```

##  高级特性

### 元数据自动填充

框架会自动填充以下字段：
- `createTime`: 插入时自动设置
- `updateTime`: 插入和更新时自动设置  
- `createBy`: 插入时设置创建者（需要用户上下文）
- `updateBy`: 更新时设置修改者（需要用户上下文）
- `version`: 乐观锁版本号
- `deleted`: 逻辑删除标记

### 分页查询

```java
// 简单分页
Page<Product> page = productService.page(new Page<>(1, 10));

// 条件分页
LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
wrapper.like(Product::getName, "手机")
       .eq(Product::getStatus, "ACTIVE");
Page<Product> result = productService.page(new Page<>(1, 10), wrapper);

// 使用增强方法
Page<Product> products = productService.findPage(1, 10, wrapper);
```

### 批量操作

```java
// 批量插入
List<Product> products = Arrays.asList(product1, product2, product3);
productService.saveBatch(products);

// 批量更新
productService.updateBatchById(products);

// 批量删除（逻辑删除）
List<Long> ids = Arrays.asList(1L, 2L, 3L);
productService.removeByIds(ids);
```

### 事务管理

```java
@Service
public class OrderService {
    
    @Autowired
    private TransactionManager transactionManager;
    
    // 声明式事务
    @Transactional(rollbackFor = Exception.class)
    public void createOrder(Order order) {
        // 业务逻辑
    }
    
    // 编程式事务
    public void createOrderProgrammatic(Order order) {
        transactionManager.executeInTransaction(status -> {
            // 业务逻辑
            return order.getId();
        });
    }
    
    // 只读事务
    public List<Order> getOrderHistory(Long userId) {
        return transactionManager.executeInReadOnlyTransaction(status -> {
            return orderMapper.selectByUserId(userId);
        });
    }
}
```

##  性能监控

### 数据源健康检查

```java
@RestController
@RequestMapping("/admin/datasource")
public class DataSourceController {
    
    @Autowired
    private DataSourceManager dataSourceManager;
    
    @GetMapping("/health")
    public Result<Map<String, Boolean>> checkHealth() {
        Map<String, Boolean> health = new HashMap<>();
        for (String name : dataSourceManager.getDataSourceNames()) {
            health.put(name, dataSourceManager.testConnection(name));
        }
        return Result.success(health);
    }
}
```

## ️ 自定义配置

### 自定义元数据处理器

```java
@Component
public class CustomMetaObjectHandler implements MetaObjectHandler {
    
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "createBy", Long.class, getCurrentUserId());
    }
    
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        this.strictUpdateFill(metaObject, "updateBy", Long.class, getCurrentUserId());
    }
    
    private Long getCurrentUserId() {
        // 从安全上下文获取当前用户ID
        return AuthContext.getCurrentUserId();
    }
}
```

### 自定义分页插件

```java
@Configuration
public class MyBatisPlusConfig {
    
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        
        // 分页插件
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        paginationInterceptor.setMaxLimit(500L); // 设置最大分页限制
        interceptor.addInnerInterceptor(paginationInterceptor);
        
        // 乐观锁插件
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        
        return interceptor;
    }
}
```

##  故障排查

### 常见问题

1. **数据源连接失败**
   - 检查数据库连接配置
   - 验证数据库服务是否启动
   - 确认防火墙和网络配置

2. **Mapper 扫描失败**
   - 确认 @MapperScan 注解配置正确
   - 检查 Mapper 接口包路径
   - 验证是否继承了正确的 BaseMapper

3. **分页查询不生效**
   - 确认 MybatisPlusInterceptor 配置
   - 检查分页插件是否正确注册
   - 验证查询方法返回类型

### 开启调试日志

```yaml
logging:
  level:
    io.nebula.data.persistence: DEBUG
    com.baomidou.mybatisplus: DEBUG
    org.springframework.jdbc: DEBUG
```

##  读写分离功能

### 特性概述

Nebula 数据持久层集成了高性能的读写分离方案，通过 AOP 切面和动态数据源实现，支持：

- **主从数据库读写分离**：写操作路由到主库，读操作路由到从库
- **多从库负载均衡**：支持轮询随机等多种负载均衡策略
- **事务内读写一致性**：确保事务内读写操作的数据一致性
- **动态数据源切换**：支持运行时动态配置数据源路由规则

### 配置示例

```yaml
nebula:
  data:
    persistence:
      enabled: true
      sources:
        primary:  # 主库配置
          type: mysql
          url: jdbc:mysql://localhost:3306/nebula_master
          username: root
          password: password
        slave01:  # 从库1配置
          type: mysql
          url: jdbc:mysql://localhost:3306/nebula_slave1
          username: root
          password: password
        slave02:  # 从库2配置 (可选)
          type: mysql
          url: jdbc:mysql://localhost:3306/nebula_slave2
          username: root
          password: password
    
    read-write-separation:
      enabled: true                    # 启用读写分离
      dynamic-routing: true            # 启用动态路由
      aspect-enabled: true             # 启用 AOP 切面
      clusters:
        default:
          enabled: true
          master: primary              # 主库数据源名称
          slaves: [slave01, slave02]   # 从库数据源列表
          load-balance-strategy: ROUND_ROBIN  # 负载均衡策略：ROUND_ROBIN, RANDOM
          force-write-on-master: true  # 事务中强制使用主库
```

### 使用方式

#### 1. 注解方式 (遵循 DTO 规范)

```java
@Service
public class ReadWriteDemoServiceImpl implements ReadWriteDemoService {
    
    @WriteDataSource(cluster = "default", description = "创建产品-写操作")
    @Transactional(rollbackFor = Exception.class)
    public CreateReadWriteProductDto.Response createProduct(CreateReadWriteProductDto.Request request) {
        Product product = new Product();
        BeanUtils.copyProperties(request, product);
        productMapper.insert(product);
        
        CreateReadWriteProductDto.Response response = new CreateReadWriteProductDto.Response();
        response.setId(product.getId());
        return response;
    }
    
    @ReadDataSource(cluster = "default", description = "获取产品详情-读操作")
    public GetReadWriteProductDto.Response getProductById(GetReadWriteProductDto.Request request) {
        Product product = productMapper.selectById(request.getId());
        
        GetReadWriteProductDto.Response response = new GetReadWriteProductDto.Response();
        if (product != null && !product.getDeleted()) {
            ProductVo productVo = new ProductVo();
            BeanUtils.copyProperties(product, productVo);
            response.setProduct(productVo);
        }
        return response;
    }
    
    @WriteDataSource(cluster = "default", description = "更新产品-写操作") 
    @Transactional(rollbackFor = Exception.class)
    public UpdateReadWriteProductDto.Response updateProduct(UpdateReadWriteProductDto.Request request) {
        // 更新逻辑...
        return response;
    }
}
```

#### 2. 编程式切换

```java
@Service
public class ReportService {
    
    public List<Product> generateReport() {
        try {
            // 手动切换到读库
            DataSourceContextHolder.setDataSourceType(DataSourceType.READ);
            return productMapper.selectList(null);
        } finally {
            // 清理上下文
            DataSourceContextHolder.clearDataSourceType();
        }
    }
}
```

### 演示和测试

详细的读写分离功能演示请参考：[Nebula 读写分离功能测试指南](../../../nebula-example/docs/nebula-readwrite-splitting-test.md)

---

## ️ 分片功能 (ShardingSphere)

### 特性概述

Nebula 数据持久层深度集成 Apache ShardingSphere，提供企业级分库分表解决方案：

- **水平分片**：支持分库分表，轻松应对大数据量场景
- **多种分片策略**：精确分片范围分片复合分片等
- **分布式主键**：内置雪花算法UUID 等全局唯一主键生成
- **跨分片查询**：自动处理跨分片的聚合查询和排序
- **读写分离集成**：可与读写分离功能组合使用

### 配置示例

```yaml
nebula:
  data:
    persistence:
      enabled: true
      sources:
        ds0:  # 分片数据源0
          type: mysql
          url: jdbc:mysql://localhost:3306/nebula_shard_0
          username: root
          password: password
        ds1:  # 分片数据源1
          type: mysql
          url: jdbc:mysql://localhost:3306/nebula_shard_1
          username: root
          password: password
    
    sharding:
      enabled: true
      
      # Schema配置
      schemas:
        default:
          data-sources: [ds0, ds1]  # 分片数据源列表
          
          # 分片表配置
          tables:
            - logic-table: t_order   # 逻辑表名
              actual-data-nodes: ds${0..1}.t_order_${0..1}  # 实际数据节点
              
              # 分库策略：根据 user_id 分库（自动创建 INLINE 算法）
              database-sharding-config:
                sharding-column: user_id
                algorithm-name: database-user-mod
                algorithm-expression: ds${user_id % 2}
              
              # 分表策略：根据 id 分表（自动创建 INLINE 算法）
              table-sharding-config:
                sharding-column: id
                algorithm-name: table-order-mod
                algorithm-expression: t_order_${id % 2}
              
              # 主键生成策略（雪花算法）
              key-generate-config:
                column: id
                algorithm-name: snowflake
```

**重要说明**：
- 配置中的 `algorithm-expression` 会自动创建对应的 INLINE 算法
- 不需要手动配置 `algorithms` 节点，框架会根据表配置自动生成
- 支持的主键生成算法：`snowflake`（雪花算法）`uuid`（UUID）

### 实体类定义

```java
@Data
@TableName("t_order")  // 逻辑表名
public class Order {
    
    @TableId(type = IdType.ASSIGN_ID)  // 由ShardingSphere生成ID
    private Long orderId;
    
    private Long userId;        // 分库键
    private String productName;
    private BigDecimal amount;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    
    @TableLogic
    private Boolean deleted;
}
```

### Mapper 和 Service (遵循 DTO 规范)

```java
@Mapper
public interface OrderMapper extends BaseMapper<Order> {
    // 基础的 CRUD 操作由 BaseMapper 提供
}

@Service
public class ShardingDemoServiceImpl implements ShardingDemoService {
    
    private final OrderMapper orderMapper;
    
    // 创建订单（自动分片路由）
    @Transactional
    public CreateShardingOrderDto.Response createOrder(CreateShardingOrderDto.Request request) {
        Order order = new Order();
        BeanUtils.copyProperties(request, order);
        
        // 如果没有指定订单ID，由ShardingSphere的雪花算法自动生成
        if (request.getOrderId() != null) {
            order.setId(request.getOrderId());
        }
        
        orderMapper.insert(order);  // ShardingSphere会自动路由到正确的分片
        
        CreateShardingOrderDto.Response response = new CreateShardingOrderDto.Response();
        response.setOrderId(order.getId());
        return response;
    }
    
    // 根据订单ID查询（精确路由，需要分片键）
    public GetShardingOrderDto.Response getOrderById(GetShardingOrderDto.Request request) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getId, request.getOrderId())
               .eq(Order::getUserId, request.getUserId());  // 包含分片键，可以精确路由
        
        Order order = orderMapper.selectOne(wrapper);
        
        GetShardingOrderDto.Response response = new GetShardingOrderDto.Response();
        if (order != null) {
            OrderVo orderVo = new OrderVo();
            BeanUtils.copyProperties(order, orderVo);
            response.setOrder(orderVo);
        }
        return response;
    }
    
    // 查询用户订单（单库查询）
    public GetShardingOrdersDto.Response getOrdersByUserId(GetShardingOrdersDto.Request request) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, request.getUserId())  // 分片键，路由到单个库
               .eq(Order::getDeleted, false)
               .orderByDesc(Order::getCreateTime);
        
        Page<Order> page = new Page<>(request.getPage(), request.getSize());
        Page<Order> orderPage = orderMapper.selectPage(page, wrapper);
        
        GetShardingOrdersDto.Response response = new GetShardingOrdersDto.Response();
        response.setOrders(orderVos);
        response.setTotal(orderPage.getTotal());
        response.setPage(request.getPage());
        response.setSize(request.getSize());
        return response;
    }
}
```

### 演示和测试

完整的分库分表功能演示请参考：[Nebula 分库分表功能测试指南](../../../nebula-example/docs/nebula-sharding-test.md)

---

##  三种场景并存配置

### 场景说明

在实际业务中，常常需要在同一个应用中同时使用三种数据访问方式：

1. **普通数据访问** - 用于用户表字典表等小数据量表
2. **读写分离访问** - 用于产品表文章表等读多写少的表
3. **分片访问** - 用于订单表日志表等大数据量表

Nebula 数据持久层支持这三种场景在同一应用中并存，通过数据源优先级策略自动管理

### 数据源优先级

```
分片数据源 > 读写分离数据源 > 普通数据源
```

当配置了分片功能时，分片数据源会成为主数据源（`@Primary`），其他数据源通过不同方式访问

### 配置示例

详细配置请参考：`nebula-example/src/main/resources/application-combined.yml`

```yaml
nebula:
  data:
    persistence:
      enabled: true
      sources:
        # 1. 普通数据源
        primary:
          type: mysql
          url: jdbc:mysql://localhost:3306/nebula_example
          username: root
          password: password
        
        # 2. 读写分离数据源
        master:
          type: mysql
          url: jdbc:mysql://localhost:3306/nebula_master
          username: root
          password: password
        slave01:
          type: mysql
          url: jdbc:mysql://localhost:3306/nebula_slave1
          username: root
          password: password
        
        # 3. 分片数据源
        ds0:
          type: mysql
          url: jdbc:mysql://localhost:3306/nebula_shard_0
          username: root
          password: password
        ds1:
          type: mysql
          url: jdbc:mysql://localhost:3306/nebula_shard_1
          username: root
          password: password
    
    # 读写分离配置（用于产品表）
    read-write-separation:
      enabled: true
      dynamic-routing: false      # 不作为主数据源（分片优先）
      aspect-enabled: true         # 启用切面，通过注解控制
      clusters:
        product-cluster:
          enabled: true
          master: master
          slaves: [slave01]
          load-balance-strategy: ROUND_ROBIN
    
    # 分片配置（用于订单表）- 优先级最高
    sharding:
      enabled: true
      schemas:
        default:
          data-sources: [ds0, ds1]
          tables:
            - logic-table: t_order
              actual-data-nodes: ds${0..1}.t_order_${0..1}
              database-sharding-config:
                sharding-column: user_id
                algorithm-name: database-user-mod
                algorithm-expression: ds${user_id % 2}
              table-sharding-config:
                sharding-column: id
                algorithm-name: table-order-mod
                algorithm-expression: t_order_${id % 2}
              key-generate-config:
                column: id
                algorithm-name: snowflake
```

### 使用方式

#### 1. 用户表（普通数据访问）

```java
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> 
        implements UserService {
    
    // 无需任何注解，默认使用分片数据源（但 t_user 未配置分片规则，相当于普通访问）
    @Override
    @Transactional
    public void createUser(User user) {
        save(user);
    }
}
```

#### 2. 产品表（读写分离访问）

```java
@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> 
        implements ProductService {
    
    // 读操作：使用 @ReadDataSource 路由到从库
    @Override
    @ReadDataSource(cluster = "product-cluster")
    public Product getProduct(Long id) {
        return getById(id);
    }
    
    // 写操作：使用 @WriteDataSource 路由到主库
    @Override
    @WriteDataSource(cluster = "product-cluster")
    @Transactional
    public void saveProduct(Product product) {
        save(product);
    }
}
```

#### 3. 订单表（分片访问）

```java
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> 
        implements OrderService {
    
    // 无需注解，ShardingSphere 自动根据分片键路由
    @Override
    @Transactional
    public void createOrder(Order order) {
        save(order);  // 自动路由到 ds{user_id % 2}.t_order_{id % 2}
    }
}
```

### 路由规则总结

| 表名 | 数据访问方式 | 路由方式 | 数据源 |
|------|------------|----------|--------|
| t_user | 普通访问 | 默认 | 分片数据源（无分片规则） |
| t_product | 读写分离 | @ReadDataSource / @WriteDataSource | master/slave01 |
| t_order | 分片 | 自动路由 | ds0/ds1 |

---

##  更多功能

- [基础数据访问测试指南](../../../nebula-example/docs/nebula-data-access-test.md)
- [读写分离功能测试指南](../../../nebula-example/docs/nebula-readwrite-splitting-test.md)  
- [分库分表功能测试指南](../../../nebula-example/docs/nebula-sharding-test.md)  
- [完整示例项目](../../../nebula-example)

---

##  DTO 规范说明

###  项目 DTO 规范

Nebula 数据持久层严格遵循 DTO 规范，每个功能模块都有专用的 DTO 文件：

#### 基础数据访问 DTO
- `CreateProductDto` - 创建产品接口DTO
- `GetProductDto` - 获取产品详情接口DTO
- `UpdateProductDto` - 更新产品接口DTO
- `DeleteProductDto` - 删除产品接口DTO
- `GetProductsDto` - 查询产品列表接口DTO

#### 读写分离演示专用 DTO
- `CreateReadWriteProductDto` - 创建产品（读写分离演示）接口DTO
- `GetReadWriteProductDto` - 获取产品详情（读写分离演示）接口DTO
- `UpdateReadWriteProductDto` - 更新产品（读写分离演示）接口DTO

#### 分片演示专用 DTO
- `CreateShardingOrderDto` - 创建订单（分片演示）接口DTO
- `GetShardingOrderDto` - 获取订单详情（分片演示）接口DTO
- `GetShardingOrdersDto` - 获取订单列表（分片演示）接口DTO

###  DTO 结构规范

```java
/**
 * [功能描述]接口DTO
 */
public class [FunctionName]Dto {
    
    /**
     * [功能描述]请求
     */
    @Data
    public static class Request {
        /** 页码，默认1 */
        private Integer pageNum = 1;
        
        /** 每页大小，默认20 */
        private Integer pageSize = 20;
        
        /** 业务参数 */
        @NotNull(message = "业务参数不能为空")
        private String businessField;
    }
    
    /**
     * [功能描述]响应
     */
    @Data
    public static class Response {
        /** 业务数据 */
        private Page<EntityVo> list;
        
        /** 其他返回字段 */
        private Boolean success;
    }
}
```

###  Service 和 Controller 规范

#### Service 层规范
```java
@Service
public class XxxDemoServiceImpl implements XxxDemoService {
    
    // 严格使用对应的专用 DTO
    public CreateXxxDto.Response createXxx(CreateXxxDto.Request request) {
        // 业务逻辑
    }
    
    public GetXxxDto.Response getXxxById(GetXxxDto.Request request) {
        // 业务逻辑  
    }
}
```

#### Controller 层规范
```java
@RestController
@RequestMapping("/xxx")
public class XxxController {
    
    private final XxxDemoService xxxDemoService;
    
    @PostMapping("/items")
    public Result<CreateXxxDto.Response> createItem(@Valid @RequestBody CreateXxxDto.Request request) {
        CreateXxxDto.Response response = xxxDemoService.createXxx(request);
        return Result.success(response);
    }
}
```

###  规范优势

1. **类型安全**: 每个接口都有专用的 DTO，避免参数混用
2. **代码清晰**: 接口职责明确，易于维护和扩展
3. **验证完整**: 每个 DTO 都有完整的参数验证注解
4. **文档友好**: 便于生成 API 文档和接口测试
5. **功能隔离**: 不同功能模块的 DTO 互不干扰，避免意外修改影响其他功能

##  贡献指南

欢迎提交 Issue 和 Pull Request 来帮助改进这个模块

##  许可证

本项目基于 Apache 2.0 许可证开源