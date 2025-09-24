# Nebula Data MongoDB 模块

## 概述

`nebula-data-mongodb`是Nebula框架的MongoDB数据访问模块，提供对MongoDB数据库的统一访问接口，支持文档存储、地理位置查询、聚合查询等MongoDB特有功能。

## 核心特性

- 🍃 **MongoDB集成**：原生支持MongoDB文档数据库
- 🔍 **高级查询**：支持复杂的文档查询和聚合操作
- 📍 **地理查询**：内置地理位置查询支持
- 📄 **文档映射**：自动Java对象与MongoDB文档的映射
- 🚀 **高性能**：基于Spring Data MongoDB的高性能实现
- 🔧 **灵活配置**：支持多种连接配置和优化选项

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-data-mongodb</artifactId>
    <version>2.0.0</version>
</dependency>

<!-- Spring Data MongoDB -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-mongodb</artifactId>
</dependency>
```

### 2. 基础配置

```yaml
nebula:
  data:
    mongodb:
      enabled: true
      database: nebula_db
      
spring:
  data:
    mongodb:
      host: localhost
      port: 27017
      database: nebula_db
      username: nebula_user
      password: nebula_pass
      
      # 连接池配置
      connection-pool:
        max-size: 100
        min-size: 10
        max-wait-time: 2000ms
        max-connection-idle-time: 30000ms
        max-connection-life-time: 600000ms
```

### 3. 文档实体定义

```java
@Document(collection = "users")
@Data
public class User {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String username;
    
    private String email;
    private Integer age;
    
    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private Point location;
    
    private List<String> tags;
    private Map<String, Object> attributes;
    
    @CreatedDate
    private LocalDateTime createTime;
    
    @LastModifiedDate  
    private LocalDateTime updateTime;
    
    private Boolean deleted = false;
}

@Document(collection = "orders")
@Data
public class Order {
    
    @Id
    private String id;
    
    @DBRef
    private User user;  // 文档引用
    
    private String orderNumber;
    private BigDecimal amount;
    private OrderStatus status;
    
    private List<OrderItem> items;  // 嵌入文档
    
    @GeoSpatialIndexed
    private Point deliveryLocation;
    
    @CreatedDate
    private LocalDateTime createTime;
}

@Data
public class OrderItem {
    private String productId;
    private String productName;
    private Integer quantity;
    private BigDecimal price;
}
```

## 核心组件

### 1. MongoRepository接口

MongoRepository扩展了标准Repository，提供MongoDB特有功能：

```java
public interface MongoRepository<T, ID> extends Repository<T, ID> {
    
    // 地理位置查询
    List<T> findByLocationNear(Point point, Distance distance);
    GeoResults<T> findByLocationNear(Point point, Distance distance, Sort sort);
    
    // 地理空间查询
    List<T> findByLocationWithin(Circle circle);
    List<T> findByLocationWithin(Box box);
    List<T> findByLocationWithin(Polygon polygon);
    
    // 正则表达式查询
    List<T> findByFieldRegex(String field, String regex);
    
    // 数组操作
    List<T> findByArrayFieldContains(String field, Object value);
    List<T> findByArrayFieldSize(String field, int size);
    
    // 嵌入文档查询
    List<T> findByEmbeddedField(String embeddedPath, Object value);
    
    // 聚合查询
    AggregationResults<T> aggregate(Aggregation aggregation);
    <O> AggregationResults<O> aggregate(Aggregation aggregation, Class<O> outputType);
    
    // 批量操作
    BulkWriteResult bulkWrite(List<WriteModel<T>> writes);
    
    // 索引操作
    void ensureIndex(IndexDefinition indexDefinition);
    void dropIndex(String indexName);
    List<IndexInfo> getIndexInfo();
}
```

### 2. MongoTemplate高级操作

MongoTemplate提供更底层的MongoDB操作：

```java
@Service
public class UserMongoService {
    
    @Autowired
    private MongoTemplate mongoTemplate;
    
    // 复杂查询
    public List<User> findUsersWithComplexCriteria(String namePattern, Integer minAge, List<String> tags) {
        Query query = new Query();
        
        Criteria criteria = new Criteria();
        
        if (StringUtils.hasText(namePattern)) {
            criteria.and("username").regex(namePattern, "i");
        }
        
        if (minAge != null) {
            criteria.and("age").gte(minAge);
        }
        
        if (tags != null && !tags.isEmpty()) {
            criteria.and("tags").in(tags);
        }
        
        query.addCriteria(criteria);
        query.with(Sort.by(Sort.Direction.DESC, "createTime"));
        
        return mongoTemplate.find(query, User.class);
    }
    
    // 聚合查询
    public List<UserAgeStats> getUserAgeStatistics() {
        Aggregation aggregation = Aggregation.newAggregation(
            Aggregation.match(Criteria.where("deleted").is(false)),
            Aggregation.group("ageRange")
                .count().as("count")
                .avg("age").as("avgAge"),
            Aggregation.sort(Sort.Direction.ASC, "ageRange")
        );
        
        AggregationResults<UserAgeStats> results = mongoTemplate.aggregate(
            aggregation, User.class, UserAgeStats.class);
            
        return results.getMappedResults();
    }
    
    // 地理位置查询
    public List<User> findUsersNearLocation(double longitude, double latitude, double maxDistanceKm) {
        Point point = new Point(longitude, latitude);
        Distance distance = new Distance(maxDistanceKm, Metrics.KILOMETERS);
        
        Query query = new Query(Criteria.where("location").near(point).maxDistance(distance.getNormalizedValue()));
        
        return mongoTemplate.find(query, User.class);
    }
    
    // 批量更新
    public BulkWriteResult batchUpdateUsers(List<User> users) {
        List<WriteModel<Document>> writes = new ArrayList<>();
        
        for (User user : users) {
            Query query = Query.query(Criteria.where("id").is(user.getId()));
            Update update = new Update()
                .set("email", user.getEmail())
                .set("updateTime", LocalDateTime.now());
                
            UpdateOneModel<Document> updateModel = new UpdateOneModel<>(
                query.getQueryObject(),
                update.getUpdateObject()
            );
            
            writes.add(updateModel);
        }
        
        return mongoTemplate.getCollection("users").bulkWrite(writes);
    }
    
    // 原子操作
    public User incrementUserScore(String userId, int increment) {
        Query query = Query.query(Criteria.where("id").is(userId));
        Update update = new Update().inc("score", increment);
        
        FindAndModifyOptions options = new FindAndModifyOptions()
            .returnNew(true)
            .upsert(false);
            
        return mongoTemplate.findAndModify(query, update, options, User.class);
    }
}
```

## 高级查询功能

### 1. 文档查询示例

```java
@Service
public class ProductQueryService {
    
    @Autowired
    private MongoRepository<Product, String> productRepository;
    
    // 基础查询
    public List<Product> findProductsByCategory(String category) {
        Query query = Query.query(Criteria.where("category").is(category));
        return mongoTemplate.find(query, Product.class);
    }
    
    // 范围查询
    public List<Product> findProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        Query query = Query.query(
            Criteria.where("price").gte(minPrice).lte(maxPrice)
        );
        return mongoTemplate.find(query, Product.class);
    }
    
    // 数组查询
    public List<Product> findProductsByTags(List<String> tags) {
        Query query = Query.query(Criteria.where("tags").in(tags));
        return mongoTemplate.find(query, Product.class);
    }
    
    // 嵌入文档查询
    public List<Product> findProductsBySpecification(String specKey, String specValue) {
        Query query = Query.query(
            Criteria.where("specifications." + specKey).is(specValue)
        );
        return mongoTemplate.find(query, Product.class);
    }
    
    // 正则表达式查询
    public List<Product> searchProductsByName(String keyword) {
        Query query = Query.query(
            Criteria.where("name").regex(keyword, "i")  // 忽略大小写
        );
        return mongoTemplate.find(query, Product.class);
    }
    
    // 复合查询
    public List<Product> findProductsWithComplexCriteria(ProductSearchRequest request) {
        Query query = new Query();
        Criteria criteria = new Criteria();
        
        if (StringUtils.hasText(request.getKeyword())) {
            criteria.orOperator(
                Criteria.where("name").regex(request.getKeyword(), "i"),
                Criteria.where("description").regex(request.getKeyword(), "i")
            );
        }
        
        if (request.getMinPrice() != null) {
            criteria.and("price").gte(request.getMinPrice());
        }
        
        if (request.getMaxPrice() != null) {
            criteria.and("price").lte(request.getMaxPrice());
        }
        
        if (request.getCategories() != null && !request.getCategories().isEmpty()) {
            criteria.and("category").in(request.getCategories());
        }
        
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            criteria.and("tags").all(request.getTags());
        }
        
        query.addCriteria(criteria);
        
        // 排序
        if (request.getSortBy() != null) {
            Sort.Direction direction = "desc".equals(request.getSortOrder()) ? 
                Sort.Direction.DESC : Sort.Direction.ASC;
            query.with(Sort.by(direction, request.getSortBy()));
        }
        
        // 分页
        if (request.getPage() != null && request.getSize() != null) {
            query.skip(request.getPage() * request.getSize()).limit(request.getSize());
        }
        
        return mongoTemplate.find(query, Product.class);
    }
}
```

### 2. 聚合查询示例

```java
@Service
public class OrderAnalyticsService {
    
    @Autowired
    private MongoTemplate mongoTemplate;
    
    // 销售统计
    public List<SalesStats> getSalesStatsByMonth() {
        Aggregation aggregation = Aggregation.newAggregation(
            // 匹配条件
            Aggregation.match(Criteria.where("status").is("COMPLETED")),
            
            // 添加月份字段
            Aggregation.addFields()
                .addField("month")
                .withValue(DateOperators.dateOf("createTime").month())
                .build(),
            
            // 按月份分组统计
            Aggregation.group("month")
                .sum("amount").as("totalAmount")
                .count().as("orderCount")
                .avg("amount").as("avgAmount"),
            
            // 排序
            Aggregation.sort(Sort.Direction.ASC, "_id"),
            
            // 投影结果字段
            Aggregation.project("totalAmount", "orderCount", "avgAmount")
                .and("_id").as("month")
        );
        
        AggregationResults<SalesStats> results = mongoTemplate.aggregate(
            aggregation, Order.class, SalesStats.class);
            
        return results.getMappedResults();
    }
    
    // 用户购买行为分析
    public List<UserBehaviorStats> analyzeUserBehavior() {
        Aggregation aggregation = Aggregation.newAggregation(
            // 展开订单项目
            Aggregation.unwind("items"),
            
            // 按用户和产品分组
            Aggregation.group("user", "items.productId")
                .sum("items.quantity").as("totalQuantity")
                .sum("items.price").as("totalSpent")
                .count().as("purchaseCount"),
            
            // 按用户重新分组
            Aggregation.group("_id.user")
                .sum("totalSpent").as("totalSpent")
                .sum("totalQuantity").as("totalQuantity")
                .sum("purchaseCount").as("purchaseCount")
                .push(new Document()
                    .append("productId", "$_id.productId")
                    .append("quantity", "$totalQuantity")
                    .append("spent", "$totalSpent")
                ).as("products"),
            
            // 排序
            Aggregation.sort(Sort.Direction.DESC, "totalSpent")
        );
        
        AggregationResults<UserBehaviorStats> results = mongoTemplate.aggregate(
            aggregation, Order.class, UserBehaviorStats.class);
            
        return results.getMappedResults();
    }
    
    // 地理位置配送分析
    public List<DeliveryStats> analyzeDeliveryByRegion() {
        Aggregation aggregation = Aggregation.newAggregation(
            // 地理位置聚合
            Aggregation.geoNear(
                NearQuery.near(new Point(0, 0))
                    .spherical(true),
                "distanceFromCenter"
            ),
            
            // 按距离区间分组
            Aggregation.group(
                ConditionalOperators.when(
                    ComparisonOperators.valueOf("distanceFromCenter").lessThanValue(1000)
                ).then("近距离")
                .when(
                    ComparisonOperators.valueOf("distanceFromCenter").lessThanValue(5000)
                ).then("中距离")
                .otherwise("远距离")
            )
            .count().as("orderCount")
            .avg("distanceFromCenter").as("avgDistance")
            .sum("amount").as("totalAmount"),
            
            // 排序
            Aggregation.sort(Sort.Direction.ASC, "_id")
        );
        
        AggregationResults<DeliveryStats> results = mongoTemplate.aggregate(
            aggregation, Order.class, DeliveryStats.class);
            
        return results.getMappedResults();
    }
}
```

### 3. 地理位置查询

```java
@Service
public class LocationService {
    
    @Autowired
    private MongoRepository<Store, String> storeRepository;
    
    @Autowired
    private MongoTemplate mongoTemplate;
    
    // 查找附近的商店
    public List<Store> findNearbyStores(double longitude, double latitude, double radiusKm) {
        Point point = new Point(longitude, latitude);
        Distance distance = new Distance(radiusKm, Metrics.KILOMETERS);
        
        return storeRepository.findByLocationNear(point, distance);
    }
    
    // 查找指定区域内的商店
    public List<Store> findStoresInArea(double centerLng, double centerLat, double radiusKm) {
        Point center = new Point(centerLng, centerLat);
        Distance radius = new Distance(radiusKm, Metrics.KILOMETERS);
        Circle circle = new Circle(center, radius);
        
        return storeRepository.findByLocationWithin(circle);
    }
    
    // 查找矩形区域内的商店
    public List<Store> findStoresInBounds(double swLng, double swLat, double neLng, double neLat) {
        Point sw = new Point(swLng, swLat);  // 西南角
        Point ne = new Point(neLng, neLat);  // 东北角
        Box box = new Box(sw, ne);
        
        return storeRepository.findByLocationWithin(box);
    }
    
    // 查找多边形区域内的商店
    public List<Store> findStoresInPolygon(List<Point> polygonPoints) {
        Polygon polygon = new Polygon(polygonPoints);
        return storeRepository.findByLocationWithin(polygon);
    }
    
    // 按距离排序的地理查询
    public GeoResults<Store> findStoresByDistanceFromPoint(double longitude, double latitude) {
        Point point = new Point(longitude, latitude);
        Distance maxDistance = new Distance(50, Metrics.KILOMETERS);
        
        NearQuery nearQuery = NearQuery.near(point)
            .maxDistance(maxDistance)
            .spherical(true);
            
        return mongoTemplate.geoNear(nearQuery, Store.class);
    }
    
    // 地理位置聚合查询
    public List<AreaStats> getStoreStatsByArea() {
        Aggregation aggregation = Aggregation.newAggregation(
            // 地理位置分组（按网格）
            Aggregation.group(
                ConditionalOperators.when(
                    ComparisonOperators.valueOf("location.coordinates.0").greaterThanValue(120)
                ).then("东部")
                .otherwise("西部"),
                ConditionalOperators.when(
                    ComparisonOperators.valueOf("location.coordinates.1").greaterThanValue(30)
                ).then("北部")
                .otherwise("南部")
            )
            .count().as("storeCount")
            .avg("rating").as("avgRating"),
            
            // 投影结果
            Aggregation.project("storeCount", "avgRating")
                .and("_id").as("area")
        );
        
        AggregationResults<AreaStats> results = mongoTemplate.aggregate(
            aggregation, Store.class, AreaStats.class);
            
        return results.getMappedResults();
    }
}
```

## 索引管理

### 1. 索引配置

```java
@Configuration
public class MongoIndexConfig {
    
    @Autowired
    private MongoTemplate mongoTemplate;
    
    @PostConstruct
    public void initIndexes() {
        createUserIndexes();
        createProductIndexes();
        createOrderIndexes();
    }
    
    private void createUserIndexes() {
        // 唯一索引
        mongoTemplate.indexOps(User.class)
            .ensureIndex(new Index().on("username", Sort.Direction.ASC).unique());
            
        // 复合索引
        mongoTemplate.indexOps(User.class)
            .ensureIndex(new Index()
                .on("age", Sort.Direction.ASC)
                .on("createTime", Sort.Direction.DESC));
                
        // 地理空间索引
        mongoTemplate.indexOps(User.class)
            .ensureIndex(new GeospatialIndex("location").typed(GeoSpatialIndexType.GEO_2DSPHERE));
            
        // 文本索引
        mongoTemplate.indexOps(User.class)
            .ensureIndex(new TextIndexDefinition.TextIndexDefinitionBuilder()
                .onField("username")
                .onField("email")
                .build());
    }
    
    private void createProductIndexes() {
        // 多字段索引
        mongoTemplate.indexOps(Product.class)
            .ensureIndex(new Index()
                .on("category", Sort.Direction.ASC)
                .on("price", Sort.Direction.DESC)
                .on("createTime", Sort.Direction.DESC));
                
        // 数组字段索引
        mongoTemplate.indexOps(Product.class)
            .ensureIndex(new Index().on("tags", Sort.Direction.ASC));
            
        // 稀疏索引
        mongoTemplate.indexOps(Product.class)
            .ensureIndex(new Index()
                .on("discountPrice", Sort.Direction.ASC)
                .sparse());
                
        // TTL索引（自动过期）
        mongoTemplate.indexOps(Product.class)
            .ensureIndex(new Index()
                .on("expireTime", Sort.Direction.ASC)
                .expire(Duration.ofDays(30)));
    }
    
    private void createOrderIndexes() {
        // 复合索引优化查询
        mongoTemplate.indexOps(Order.class)
            .ensureIndex(new Index()
                .on("user.$id", Sort.Direction.ASC)
                .on("status", Sort.Direction.ASC)
                .on("createTime", Sort.Direction.DESC));
                
        // 哈希索引（用于分片）
        mongoTemplate.indexOps(Order.class)
            .ensureIndex(new Index().on("orderNumber", Sort.Direction.ASC).hashed());
    }
}
```

### 2. 动态索引管理

```java
@Service
public class IndexManagementService {
    
    @Autowired
    private MongoTemplate mongoTemplate;
    
    // 分析查询性能并建议索引
    public List<IndexSuggestion> analyzeQueryPerformance(String collection) {
        List<IndexSuggestion> suggestions = new ArrayList<>();
        
        // 获取当前索引信息
        List<IndexInfo> currentIndexes = mongoTemplate.indexOps(collection).getIndexInfo();
        
        // 分析慢查询日志（需要启用MongoDB profiler）
        Query slowQueryAnalysis = new Query();
        // ... 分析逻辑
        
        return suggestions;
    }
    
    // 创建推荐的索引
    public void createRecommendedIndexes(String collection, List<IndexDefinition> indexes) {
        IndexOperations indexOps = mongoTemplate.indexOps(collection);
        
        for (IndexDefinition index : indexes) {
            try {
                indexOps.ensureIndex(index);
                log.info("Created index for collection {}: {}", collection, index);
            } catch (Exception e) {
                log.error("Failed to create index for collection {}: {}", collection, index, e);
            }
        }
    }
    
    // 索引使用统计
    public IndexUsageStats getIndexUsageStats(String collection) {
        // 使用MongoDB的$indexStats聚合操作
        Aggregation aggregation = Aggregation.newAggregation(
            Aggregation.indexStats(),
            Aggregation.project()
                .and("name").as("indexName")
                .and("accesses.ops").as("accessCount")
                .and("accesses.since").as("lastAccess")
        );
        
        AggregationResults<IndexUsageStats> results = mongoTemplate.aggregate(
            aggregation, collection, IndexUsageStats.class);
            
        return results.getUniqueMappedResult();
    }
    
    // 清理未使用的索引
    public void cleanupUnusedIndexes(String collection, Duration unusedThreshold) {
        List<IndexInfo> indexes = mongoTemplate.indexOps(collection).getIndexInfo();
        
        for (IndexInfo index : indexes) {
            if (isIndexUnused(collection, index.getName(), unusedThreshold)) {
                log.info("Dropping unused index: {} on collection: {}", index.getName(), collection);
                mongoTemplate.indexOps(collection).dropIndex(index.getName());
            }
        }
    }
    
    private boolean isIndexUnused(String collection, String indexName, Duration threshold) {
        // 检查索引在指定时间段内是否被使用
        // 实现需要结合MongoDB的索引统计信息
        return false;
    }
}
```

## 事务支持

### 1. 多文档事务

```java
@Service
@Transactional
public class MongoTransactionService {
    
    @Autowired
    private MongoTemplate mongoTemplate;
    
    // 转账操作（需要事务保证一致性）
    @Transactional
    public void transferMoney(String fromAccountId, String toAccountId, BigDecimal amount) {
        // 查询源账户
        Account fromAccount = mongoTemplate.findById(fromAccountId, Account.class);
        if (fromAccount == null || fromAccount.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("余额不足");
        }
        
        // 查询目标账户
        Account toAccount = mongoTemplate.findById(toAccountId, Account.class);
        if (toAccount == null) {
            throw new AccountNotFoundException("目标账户不存在");
        }
        
        // 更新源账户
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        mongoTemplate.save(fromAccount);
        
        // 更新目标账户
        toAccount.setBalance(toAccount.getBalance().add(amount));
        mongoTemplate.save(toAccount);
        
        // 记录转账日志
        TransferLog log = new TransferLog();
        log.setFromAccountId(fromAccountId);
        log.setToAccountId(toAccountId);
        log.setAmount(amount);
        log.setCreateTime(LocalDateTime.now());
        mongoTemplate.insert(log);
    }
    
    // 编程式事务
    public void programmaticTransaction() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(mongoTransactionManager);
        
        transactionTemplate.execute(status -> {
            try {
                // 执行事务操作
                User user = new User();
                user.setUsername("testuser");
                mongoTemplate.insert(user);
                
                UserProfile profile = new UserProfile();
                profile.setUserId(user.getId());
                mongoTemplate.insert(profile);
                
                return user;
            } catch (Exception e) {
                status.setRollbackOnly();
                throw e;
            }
        });
    }
}
```

### 2. Session管理

```java
@Service
public class MongoSessionService {
    
    @Autowired
    private MongoTemplate mongoTemplate;
    
    // 手动session管理
    public void manualSessionExample() {
        try (ClientSession session = mongoTemplate.getMongoClient().startSession()) {
            session.startTransaction();
            
            try {
                // 在session中执行操作
                mongoTemplate.insert(new User(), session);
                mongoTemplate.insert(new UserProfile(), session);
                
                // 提交事务
                session.commitTransaction();
            } catch (Exception e) {
                // 回滚事务
                session.abortTransaction();
                throw e;
            }
        }
    }
    
    // 使用MongoTransactionManager
    @Autowired
    private MongoTransactionManager transactionManager;
    
    public void transactionManagerExample() {
        TransactionDefinition definition = new DefaultTransactionDefinition();
        TransactionStatus status = transactionManager.getTransaction(definition);
        
        try {
            // 执行数据库操作
            mongoTemplate.insert(new User());
            mongoTemplate.insert(new UserProfile());
            
            // 提交事务
            transactionManager.commit(status);
        } catch (Exception e) {
            // 回滚事务
            transactionManager.rollback(status);
            throw e;
        }
    }
}
```

## 性能优化

### 1. 查询优化

```java
@Service
public class MongoPerformanceService {
    
    @Autowired
    private MongoTemplate mongoTemplate;
    
    // 使用投影减少网络传输
    public List<UserSummary> getUserSummaries() {
        Query query = new Query();
        query.fields()
            .include("username")
            .include("email")
            .include("createTime")
            .exclude("_id");  // 排除不需要的字段
            
        return mongoTemplate.find(query, UserSummary.class, "users");
    }
    
    // 批量操作优化
    public void batchInsertUsers(List<User> users) {
        // 使用批量插入而不是逐个插入
        mongoTemplate.insertAll(users);
    }
    
    public BulkWriteResult batchUpdateUsers(List<User> users) {
        BulkOperations bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, User.class);
        
        for (User user : users) {
            Query query = Query.query(Criteria.where("id").is(user.getId()));
            Update update = new Update()
                .set("email", user.getEmail())
                .set("updateTime", LocalDateTime.now());
            bulkOps.updateOne(query, update);
        }
        
        return bulkOps.execute();
    }
    
    // 使用聚合管道优化复杂查询
    public List<UserOrderStats> getUserOrderStatsOptimized() {
        Aggregation aggregation = Aggregation.newAggregation(
            // 在数据库层面进行关联
            Aggregation.lookup("orders", "_id", "userId", "orders"),
            
            // 计算统计信息
            Aggregation.addFields()
                .addField("orderCount").withValue(ArrayOperators.arrayOf("orders").length())
                .addField("totalAmount").withValue(
                    ArrayOperators.reduce(
                        ArrayOperators.arrayOf("orders.amount"), 
                        0,
                        ArithmeticOperators.valueOf("$$value").add("$$this")
                    )
                ).build(),
            
            // 投影需要的字段
            Aggregation.project("username", "email", "orderCount", "totalAmount")
        );
        
        return mongoTemplate.aggregate(aggregation, User.class, UserOrderStats.class)
            .getMappedResults();
    }
    
    // 分页优化
    public Page<User> findUsersOptimizedPaging(Pageable pageable) {
        Query query = new Query();
        
        // 先计算总数（可以缓存）
        long total = mongoTemplate.count(query, User.class);
        
        // 然后查询当前页数据
        query.with(pageable);
        List<User> users = mongoTemplate.find(query, User.class);
        
        return new PageImpl<>(users, pageable, total);
    }
    
    // 使用hint指定索引
    public List<User> findUsersWithIndexHint(String department) {
        Query query = Query.query(Criteria.where("department").is(department));
        query.withHint("department_1_createTime_-1");  // 指定使用的索引
        
        return mongoTemplate.find(query, User.class);
    }
}
```

### 2. 连接池优化

```yaml
spring:
  data:
    mongodb:
      # 连接池配置
      connection-pool:
        max-size: 100              # 最大连接数
        min-size: 10               # 最小连接数
        max-wait-time: 2000ms      # 最大等待时间
        max-connection-idle-time: 30000ms  # 连接最大空闲时间
        max-connection-life-time: 600000ms # 连接最大生存时间
        
      # 服务器配置
      server-selection-timeout: 30000ms
      socket-timeout: 0
      connect-timeout: 10000ms
      
      # 读取配置
      read-preference: secondaryPreferred  # 优先从从节点读取
      read-concern: majority              # 读取已确认的数据
      write-concern: majority             # 写入确认
```

## 监控和诊断

### 1. 性能监控

```java
@Component
public class MongoMetricsCollector {
    
    @Autowired
    private MongoTemplate mongoTemplate;
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    @EventListener
    public void onMongoQueryExecution(MongoQueryEvent event) {
        Timer.Sample sample = Timer.start(meterRegistry);
        sample.stop(Timer.builder("mongo.query.duration")
            .tag("collection", event.getCollectionName())
            .tag("operation", event.getOperation())
            .register(meterRegistry));
    }
    
    @Scheduled(fixedRate = 60000)
    public void collectDatabaseStats() {
        try {
            // 获取数据库统计信息
            Document stats = mongoTemplate.getDb().runCommand(new Document("dbStats", 1));
            
            // 注册指标
            Gauge.builder("mongo.db.size")
                .description("Database size in bytes")
                .register(meterRegistry, stats, s -> s.getDouble("dataSize"));
                
            Gauge.builder("mongo.db.collections")
                .description("Number of collections")
                .register(meterRegistry, stats, s -> s.getInteger("collections"));
                
        } catch (Exception e) {
            log.error("Failed to collect MongoDB stats", e);
        }
    }
    
    // 慢查询监控
    @EventListener
    public void onSlowQuery(MongoSlowQueryEvent event) {
        if (event.getDuration().toMillis() > 1000) {  // 超过1秒的查询
            log.warn("Slow MongoDB query detected: collection={}, duration={}ms, query={}", 
                    event.getCollectionName(), 
                    event.getDuration().toMillis(),
                    event.getQuery());
                    
            // 记录到监控系统
            meterRegistry.counter("mongo.slow.query", 
                "collection", event.getCollectionName()).increment();
        }
    }
}
```

### 2. 健康检查

```java
@Component
public class MongoHealthIndicator implements HealthIndicator {
    
    @Autowired
    private MongoTemplate mongoTemplate;
    
    @Override
    public Health health() {
        try {
            // 执行简单查询测试连接
            mongoTemplate.execute(db -> {
                db.runCommand(new Document("ping", 1));
                return "ok";
            });
            
            // 检查副本集状态
            Document replSetStatus = mongoTemplate.execute(db -> {
                try {
                    return db.runCommand(new Document("replSetGetStatus", 1));
                } catch (Exception e) {
                    return null;  // 单节点部署
                }
            });
            
            Health.Builder builder = Health.up()
                .withDetail("database", mongoTemplate.getDb().getName());
                
            if (replSetStatus != null) {
                builder.withDetail("replicaSet", replSetStatus.getString("set"))
                       .withDetail("state", replSetStatus.getString("myState"));
            }
            
            return builder.build();
            
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

## 最佳实践

### 1. 文档设计原则

```java
// ✅ 好的文档设计
@Document(collection = "users")
public class User {
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String username;  // 经常查询的字段建索引
    
    private String email;
    private UserProfile profile;  // 嵌入小而相关的数据
    
    @DBRef
    private List<Role> roles;  // 大或独立的数据使用引用
    
    private List<String> tags;  // 数组字段适合MongoDB
    private Map<String, Object> metadata;  // 灵活的键值对
}

// ❌ 避免过度嵌套
@Document
public class BadOrderDesign {
    private Customer customer;  // 应该使用引用
    private List<Product> products;  // 产品信息可能很大，应该引用
    private List<Comment> comments;  // 评论可能无限增长，应该单独集合
}
```

### 2. 查询优化建议

```java
@Service
public class QueryOptimizationExamples {
    
    // ✅ 使用投影减少数据传输
    public List<UserBasicInfo> getUserBasicInfo() {
        Query query = new Query();
        query.fields().include("username", "email", "createTime");
        return mongoTemplate.find(query, UserBasicInfo.class, "users");
    }
    
    // ✅ 使用合适的索引
    public List<User> findActiveUsersByDepartment(String department) {
        // 确保有复合索引：{department: 1, status: 1, createTime: -1}
        Query query = Query.query(
            Criteria.where("department").is(department)
                .and("status").is("ACTIVE")
        ).with(Sort.by(Sort.Direction.DESC, "createTime"));
        
        return mongoTemplate.find(query, User.class);
    }
    
    // ✅ 避免大量数据的skip操作
    public List<User> paginateUsersEfficiently(String lastUserId, int limit) {
        Query query = new Query();
        
        if (StringUtils.hasText(lastUserId)) {
            // 使用范围查询而不是skip
            query.addCriteria(Criteria.where("_id").gt(lastUserId));
        }
        
        query.limit(limit);
        query.with(Sort.by(Sort.Direction.ASC, "_id"));
        
        return mongoTemplate.find(query, User.class);
    }
    
    // ❌ 避免没有索引的正则表达式查询
    public void badRegexQuery(String pattern) {
        Query query = Query.query(
            Criteria.where("description").regex(".*" + pattern + ".*")  // 前缀通配符性能差
        );
        // 应该使用文本索引或者前缀匹配
    }
}
```

### 3. 事务使用指南

```java
@Service
public class TransactionBestPractices {
    
    // ✅ 适合使用事务的场景
    @Transactional
    public void transferBetweenAccounts(String fromId, String toId, BigDecimal amount) {
        // 涉及多个文档的原子操作
        Account from = accountRepository.findById(fromId);
        Account to = accountRepository.findById(toId);
        
        from.subtract(amount);
        to.add(amount);
        
        accountRepository.save(from);
        accountRepository.save(to);
    }
    
    // ❌ 避免长时间运行的事务
    public void avoidLongTransaction() {
        // 不要在事务中执行：
        // - 长时间的外部API调用
        // - 大量数据处理
        // - 文件I/O操作
        // - 复杂的业务逻辑计算
    }
    
    // ✅ 使用乐观锁代替事务（当可能时）
    @Document
    public class OptimisticLockExample {
        @Id
        private String id;
        
        @Version
        private Long version;  // Spring Data自动处理版本控制
        
        private BigDecimal balance;
    }
}
```

通过以上配置和使用方式，你可以充分利用Nebula MongoDB模块的强大功能，高效地管理MongoDB文档数据库。
