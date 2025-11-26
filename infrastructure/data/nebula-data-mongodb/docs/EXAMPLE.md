# Nebula Data MongoDB - 使用示例

> MongoDB数据访问完整使用指南，以票务系统为例

## 目录

- [快速开始](#快速开始)
- [基础CRUD操作](#基础crud操作)
- [复杂查询](#复杂查询)
- [聚合操作](#聚合操作)
- [索引管理](#索引管理)
- [事务管理](#事务管理)
- [地理位置查询](#地理位置查询)
- [批量操作](#批量操作)
- [票务系统完整示例](#票务系统完整示例)
- [最佳实践](#最佳实践)

---

## 快速开始

### 添加依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-data-mongodb</artifactId>
    <version>${nebula.version}</version>
</dependency>
```

### 基础配置

```yaml
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/ticket_db
      # 或者详细配置
      host: localhost
      port: 27017
      database: ticket_db
      username: admin
      password: password
      authentication-database: admin

nebula:
  data:
    mongodb:
      enabled: true
      auto-index-creation: true  # 自动创建索引
```

### 定义实体类

```java
/**
 * MongoDB文档基类
 */
@Data
public abstract class BaseDocument {
    
    @Id
    private String id;
    
    @Field("create_time")
    @CreatedDate
    private LocalDateTime createTime;
    
    @Field("update_time")
    @LastModifiedDate
    private LocalDateTime updateTime;
}
```

---

## 基础CRUD操作

### 1. 定义实体类（票务日志）

```java
/**
 * 订单操作日志
 * 使用MongoDB存储日志数据，支持大量写入和复杂查询
 */
@Data
@Document(collection = "order_logs")
@EqualsAndHashCode(callSuper = true)
public class OrderLog extends BaseDocument {
    
    /**
     * 订单号
     */
    @Indexed
    @Field("order_no")
    private String orderNo;
    
    /**
     * 用户ID
     */
    @Indexed
    @Field("user_id")
    private Long userId;
    
    /**
     * 操作类型：CREATE-创建，PAY-支付，CANCEL-取消，REFUND-退款
     */
    @Indexed
    @Field("action_type")
    private String actionType;
    
    /**
     * 操作描述
     */
    @Field("description")
    private String description;
    
    /**
     * IP地址
     */
    @Field("ip_address")
    private String ipAddress;
    
    /**
     * 设备信息
     */
    @Field("device_info")
    private String deviceInfo;
    
    /**
     * 操作前状态
     */
    @Field("before_status")
    private String beforeStatus;
    
    /**
     * 操作后状态
     */
    @Field("after_status")
    private String afterStatus;
    
    /**
     * 扩展数据（JSON格式）
     */
    @Field("extra_data")
    private Map<String, Object> extraData;
    
    /**
     * 操作时间戳
     */
    @Indexed
    @Field("timestamp")
    private Long timestamp;
}
```

### 2. 定义Repository

```java
/**
 * 订单日志Repository
 */
@Repository
public interface OrderLogRepository extends MongoRepository<OrderLog, String> {
    
    /**
     * 根据订单号查询日志
     */
    List<OrderLog> findByOrderNo(String orderNo);
    
    /**
     * 根据用户ID和操作类型查询
     */
    List<OrderLog> findByUserIdAndActionType(Long userId, String actionType);
    
    /**
     * 根据时间范围查询
     */
    List<OrderLog> findByTimestampBetween(Long startTime, Long endTime);
    
    /**
     * 统计用户的操作次数
     */
    long countByUserId(Long userId);
}
```

### 3. 使用Repository

```java
/**
 * 订单日志服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderLogService {
    
    private final OrderLogRepository orderLogRepository;
    
    /**
     * 记录订单操作日志
     */
    public void logOrderAction(Order order, String actionType, String description,
                                String ipAddress, String deviceInfo) {
        OrderLog log = new OrderLog();
        log.setOrderNo(order.getOrderNo());
        log.setUserId(order.getUserId());
        log.setActionType(actionType);
        log.setDescription(description);
        log.setIpAddress(ipAddress);
        log.setDeviceInfo(deviceInfo);
        log.setBeforeStatus(order.getStatus());
        log.setTimestamp(System.currentTimeMillis());
        
        // 扩展数据
        Map<String, Object> extraData = new HashMap<>();
        extraData.put("totalAmount", order.getTotalAmount());
        extraData.put("quantity", order.getQuantity());
        log.setExtraData(extraData);
        
        orderLogRepository.save(log);
        
        log.info("订单日志已记录：订单={}, 操作={}", order.getOrderNo(), actionType);
    }
    
    /**
     * 查询订单的所有操作日志
     */
    public List<OrderLog> getOrderLogs(String orderNo) {
        return orderLogRepository.findByOrderNo(orderNo);
    }
    
    /**
     * 查询用户的操作历史
     */
    public List<OrderLog> getUserActionHistory(Long userId, String actionType) {
        return orderLogRepository.findByUserIdAndActionType(userId, actionType);
    }
    
    /**
     * 统计用户的操作次数
     */
    public long countUserActions(Long userId) {
        return orderLogRepository.countByUserId(userId);
    }
}
```

---

## 复杂查询

### 1. Criteria查询

```java
/**
 * 复杂查询示例
 */
@Service
@RequiredArgsConstructor
public class OrderLogQueryService {
    
    private final MongoTemplate mongoTemplate;
    
    /**
     * 多条件查询订单日志
     */
    public List<OrderLog> searchLogs(OrderLogSearchRequest request) {
        Criteria criteria = new Criteria();
        
        // 订单号
        if (StringUtils.hasText(request.getOrderNo())) {
            criteria.and("order_no").is(request.getOrderNo());
        }
        
        // 用户ID
        if (request.getUserId() != null) {
            criteria.and("user_id").is(request.getUserId());
        }
        
        // 操作类型
        if (StringUtils.hasText(request.getActionType())) {
            criteria.and("action_type").is(request.getActionType());
        }
        
        // 时间范围
        if (request.getStartTime() != null && request.getEndTime() != null) {
            criteria.and("timestamp").gte(request.getStartTime()).lte(request.getEndTime());
        }
        
        // IP地址模糊查询
        if (StringUtils.hasText(request.getIpPattern())) {
            criteria.and("ip_address").regex(request.getIpPattern());
        }
        
        Query query = new Query(criteria);
        
        // 排序
        query.with(Sort.by(Sort.Direction.DESC, "timestamp"));
        
        // 分页
        if (request.getPageNum() != null && request.getPageSize() != null) {
            int skip = (request.getPageNum() - 1) * request.getPageSize();
            query.skip(skip).limit(request.getPageSize());
        }
        
        return mongoTemplate.find(query, OrderLog.class);
    }
    
    /**
     * 全文搜索（需要创建文本索引）
     */
    public List<OrderLog> fullTextSearch(String keyword) {
        TextCriteria textCriteria = TextCriteria.forDefaultLanguage().matchingAny(keyword);
        Query query = TextQuery.queryText(textCriteria)
                .sortByScore()
                .with(Sort.by(Sort.Direction.DESC, "timestamp"));
        
        return mongoTemplate.find(query, OrderLog.class);
    }
    
    /**
     * 查询指定字段（投影）
     */
    public List<OrderLogSummary> getLogSummaries(Long userId) {
        Query query = new Query(Criteria.where("user_id").is(userId));
        
        // 只查询指定字段
        query.fields()
                .include("order_no")
                .include("action_type")
                .include("timestamp")
                .exclude("_id");
        
        return mongoTemplate.find(query, OrderLogSummary.class, "order_logs");
    }
}

/**
 * 查询请求DTO
 */
@Data
public class OrderLogSearchRequest {
    private String orderNo;
    private Long userId;
    private String actionType;
    private Long startTime;
    private Long endTime;
    private String ipPattern;
    private Integer pageNum;
    private Integer pageSize;
}

/**
 * 日志摘要VO
 */
@Data
public class OrderLogSummary {
    private String orderNo;
    private String actionType;
    private Long timestamp;
}
```

### 2. 正则表达式查询

```java
/**
 * 正则表达式查询示例
 */
@Service
@RequiredArgsConstructor
public class PatternQueryService {
    
    private final MongoTemplate mongoTemplate;
    
    /**
     * 模糊查询订单号
     */
    public List<OrderLog> findByOrderNoPattern(String pattern) {
        Query query = new Query(Criteria.where("order_no").regex(pattern, "i")); // i: 不区分大小写
        return mongoTemplate.find(query, OrderLog.class);
    }
    
    /**
     * 查询特定IP段的日志
     */
    public List<OrderLog> findByIpRange(String ipPrefix) {
        // 查询IP以特定前缀开头的日志，如 "192.168"
        Query query = new Query(Criteria.where("ip_address").regex("^" + ipPrefix));
        return mongoTemplate.find(query, OrderLog.class);
    }
}
```

---

## 聚合操作

### 1. 基础聚合

```java
/**
 * 聚合查询示例
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderLogAggregationService {
    
    private final MongoTemplate mongoTemplate;
    
    /**
     * 统计每种操作类型的数量
     */
    public List<ActionTypeCount> countByActionType() {
        Aggregation aggregation = Aggregation.newAggregation(
                // 分组并计数
                Aggregation.group("action_type")
                        .count().as("count"),
                // 排序
                Aggregation.sort(Sort.Direction.DESC, "count")
        );
        
        AggregationResults<ActionTypeCount> results = mongoTemplate.aggregate(
                aggregation, "order_logs", ActionTypeCount.class);
        
        return results.getMappedResults();
    }
    
    /**
     * 统计每个用户的操作次数（Top 10）
     */
    public List<UserActionCount> getTopActiveUsers() {
        Aggregation aggregation = Aggregation.newAggregation(
                // 分组并计数
                Aggregation.group("user_id")
                        .count().as("actionCount"),
                // 排序
                Aggregation.sort(Sort.Direction.DESC, "actionCount"),
                // 限制前10名
                Aggregation.limit(10)
        );
        
        AggregationResults<UserActionCount> results = mongoTemplate.aggregate(
                aggregation, "order_logs", UserActionCount.class);
        
        return results.getMappedResults();
    }
    
    /**
     * 按小时统计订单创建数量
     */
    public List<HourlyOrderCount> countOrdersByHour(LocalDate date) {
        long startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000;
        long endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000;
        
        Aggregation aggregation = Aggregation.newAggregation(
                // 筛选条件
                Aggregation.match(Criteria.where("action_type").is("CREATE")
                        .and("timestamp").gte(startOfDay).lt(endOfDay)),
                // 添加小时字段
                Aggregation.project()
                        .and(DateOperators.Hour.hourOf("timestamp")).as("hour")
                        .andInclude("order_no"),
                // 按小时分组并计数
                Aggregation.group("hour")
                        .count().as("count"),
                // 排序
                Aggregation.sort(Sort.Direction.ASC, "_id")
        );
        
        AggregationResults<HourlyOrderCount> results = mongoTemplate.aggregate(
                aggregation, "order_logs", HourlyOrderCount.class);
        
        return results.getMappedResults();
    }
    
    /**
     * 复杂聚合：统计每个用户在每种状态下的操作次数
     */
    public List<UserStatusActionStats> getUserStatusActionStats(Long userId) {
        Aggregation aggregation = Aggregation.newAggregation(
                // 筛选用户
                Aggregation.match(Criteria.where("user_id").is(userId)),
                // 分组统计
                Aggregation.group("user_id", "action_type", "after_status")
                        .count().as("count"),
                // 排序
                Aggregation.sort(Sort.Direction.DESC, "count")
        );
        
        AggregationResults<UserStatusActionStats> results = mongoTemplate.aggregate(
                aggregation, "order_logs", UserStatusActionStats.class);
        
        return results.getMappedResults();
    }
}

/**
 * 操作类型统计结果
 */
@Data
public class ActionTypeCount {
    @Id
    private String actionType;
    private Long count;
}

/**
 * 用户操作统计结果
 */
@Data
public class UserActionCount {
    @Id
    private Long userId;
    private Long actionCount;
}

/**
 * 按小时统计结果
 */
@Data
public class HourlyOrderCount {
    @Id
    private Integer hour;
    private Long count;
}

/**
 * 用户状态操作统计
 */
@Data
public class UserStatusActionStats {
    private Long userId;
    private String actionType;
    private String afterStatus;
    private Long count;
}
```

### 2. 管道聚合

```java
/**
 * 复杂管道聚合示例
 */
@Service
@RequiredArgsConstructor
public class AdvancedAggregationService {
    
    private final MongoTemplate mongoTemplate;
    
    /**
     * 用户行为分析
     */
    public List<UserBehaviorAnalysis> analyzeUserBehavior(Long userId, int days) {
        long startTime = LocalDateTime.now().minusDays(days)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        
        Aggregation aggregation = Aggregation.newAggregation(
                // 1. 筛选条件
                Aggregation.match(Criteria.where("user_id").is(userId)
                        .and("timestamp").gte(startTime)),
                
                // 2. 添加日期字段
                Aggregation.project()
                        .andExpression("toDate(timestamp)").as("date")
                        .andInclude("action_type", "order_no"),
                
                // 3. 按日期分组
                Aggregation.group("date")
                        .count().as("totalActions")
                        .addToSet("action_type").as("actionTypes")
                        .addToSet("order_no").as("orders"),
                
                // 4. 添加计算字段
                Aggregation.project()
                        .andInclude("totalActions", "actionTypes", "orders")
                        .and("orders").size().as("orderCount"),
                
                // 5. 排序
                Aggregation.sort(Sort.Direction.DESC, "_id")
        );
        
        AggregationResults<UserBehaviorAnalysis> results = mongoTemplate.aggregate(
                aggregation, "order_logs", UserBehaviorAnalysis.class);
        
        return results.getMappedResults();
    }
}

/**
 * 用户行为分析结果
 */
@Data
public class UserBehaviorAnalysis {
    @Id
    private Date date;
    private Long totalActions;
    private List<String> actionTypes;
    private Integer orderCount;
}
```

---

## 索引管理

### 1. 注解方式创建索引

```java
/**
 * 审计事件实体（带索引）
 */
@Data
@Document(collection = "audit_events")
public class AuditEvent extends BaseDocument {
    
    /**
     * 事件类型（普通索引）
     */
    @Indexed(name = "idx_event_type")
    @Field("event_type")
    private String eventType;
    
    /**
     * 用户ID（普通索引）
     */
    @Indexed(name = "idx_user_id")
    @Field("user_id")
    private Long userId;
    
    /**
     * 事件描述（全文索引）
     */
    @TextIndexed
    @Field("description")
    private String description;
    
    /**
     * 时间戳（组合索引的一部分）
     */
    @Field("timestamp")
    private Long timestamp;
    
    /**
     * IP地址
     */
    @Field("ip_address")
    private String ipAddress;
    
    /**
     * 事件数据
     */
    @Field("event_data")
    private Map<String, Object> eventData;
}

/**
 * 组合索引配置
 */
@Configuration
public class MongoIndexConfig {
    
    @Autowired
    private MongoTemplate mongoTemplate;
    
    @PostConstruct
    public void initIndexes() {
        // 创建组合索引
        IndexOperations indexOps = mongoTemplate.indexOps(AuditEvent.class);
        
        // 组合索引：user_id + timestamp（降序）
        Index userTimeIndex = new Index()
                .on("user_id", Sort.Direction.ASC)
                .on("timestamp", Sort.Direction.DESC)
                .named("idx_user_time");
        indexOps.ensureIndex(userTimeIndex);
        
        // TTL索引：自动删除过期数据（保留30天）
        Index ttlIndex = new Index()
                .on("create_time", Sort.Direction.ASC)
                .expire(30, TimeUnit.DAYS)
                .named("idx_ttl");
        indexOps.ensureIndex(ttlIndex);
        
        log.info("MongoDB索引创建完成");
    }
}
```

### 2. 程序化创建索引

```java
/**
 * 动态索引管理
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IndexManagementService {
    
    private final MongoTemplate mongoTemplate;
    
    /**
     * 创建单字段索引
     */
    public void createSingleIndex(String collectionName, String fieldName, boolean ascending) {
        IndexOperations indexOps = mongoTemplate.indexOps(collectionName);
        
        Index index = new Index()
                .on(fieldName, ascending ? Sort.Direction.ASC : Sort.Direction.DESC)
                .named("idx_" + fieldName);
        
        indexOps.ensureIndex(index);
        
        log.info("索引创建成功：集合={}, 字段={}", collectionName, fieldName);
    }
    
    /**
     * 创建唯一索引
     */
    public void createUniqueIndex(String collectionName, String fieldName) {
        IndexOperations indexOps = mongoTemplate.indexOps(collectionName);
        
        Index index = new Index()
                .on(fieldName, Sort.Direction.ASC)
                .unique()
                .named("idx_unique_" + fieldName);
        
        indexOps.ensureIndex(index);
        
        log.info("唯一索引创建成功：集合={}, 字段={}", collectionName, fieldName);
    }
    
    /**
     * 删除索引
     */
    public void dropIndex(String collectionName, String indexName) {
        IndexOperations indexOps = mongoTemplate.indexOps(collectionName);
        indexOps.dropIndex(indexName);
        
        log.info("索引已删除：集合={}, 索引={}", collectionName, indexName);
    }
    
    /**
     * 查询所有索引
     */
    public List<IndexInfo> listIndexes(String collectionName) {
        IndexOperations indexOps = mongoTemplate.indexOps(collectionName);
        return indexOps.getIndexInfo();
    }
}
```

---

## 事务管理

### MongoDB事务支持（需要副本集）

```java
/**
 * MongoDB事务示例
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MongoTransactionService {
    
    private final MongoTemplate mongoTemplate;
    private final OrderLogRepository orderLogRepository;
    private final AuditEventRepository auditEventRepository;
    
    /**
     * 使用@Transactional注解（需要配置TransactionManager）
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveLogAndAuditWithTransaction(Order order, String action) {
        // 1. 保存订单日志
        OrderLog log = new OrderLog();
        log.setOrderNo(order.getOrderNo());
        log.setUserId(order.getUserId());
        log.setActionType(action);
        log.setTimestamp(System.currentTimeMillis());
        orderLogRepository.save(log);
        
        // 2. 保存审计事件
        AuditEvent audit = new AuditEvent();
        audit.setEventType("ORDER_" + action);
        audit.setUserId(order.getUserId());
        audit.setDescription("订单" + order.getOrderNo() + "执行了" + action + "操作");
        audit.setTimestamp(System.currentTimeMillis());
        auditEventRepository.save(audit);
        
        // 如果这里抛出异常，上面的两个保存操作都会回滚
        if (order.getTotalAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("金额不能为负数");
        }
        
        log.info("事务提交：订单日志和审计事件已保存");
    }
    
    /**
     * 编程式事务
     */
    public void saveWithProgrammaticTransaction(Order order, String action) {
        MongoTransactionManager transactionManager = new MongoTransactionManager(
                mongoTemplate.getMongoDatabaseFactory());
        
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        
        transactionTemplate.execute(status -> {
            try {
                // 事务操作
                OrderLog log = new OrderLog();
                log.setOrderNo(order.getOrderNo());
                log.setUserId(order.getUserId());
                log.setActionType(action);
                log.setTimestamp(System.currentTimeMillis());
                orderLogRepository.save(log);
                
                AuditEvent audit = new AuditEvent();
                audit.setEventType("ORDER_" + action);
                audit.setUserId(order.getUserId());
                audit.setDescription("订单操作");
                audit.setTimestamp(System.currentTimeMillis());
                auditEventRepository.save(audit);
                
                return null;
            } catch (Exception e) {
                status.setRollbackOnly();
                throw e;
            }
        });
    }
}
```

---

## 地理位置查询

### 1. 定义地理位置实体

```java
/**
 * 演出场馆实体（带地理位置）
 */
@Data
@Document(collection = "venues")
public class Venue extends BaseDocument {
    
    /**
     * 场馆名称
     */
    @Field("name")
    private String name;
    
    /**
     * 地址
     */
    @Field("address")
    private String address;
    
    /**
     * 地理位置（GeoJSON格式）
     */
    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    @Field("location")
    private GeoJsonPoint location;
    
    /**
     * 容量
     */
    @Field("capacity")
    private Integer capacity;
    
    /**
     * 设施
     */
    @Field("facilities")
    private List<String> facilities;
}
```

### 2. 地理位置查询

```java
/**
 * 地理位置查询服务
 */
@Service
@RequiredArgsConstructor
public class VenueLocationService {
    
    private final MongoTemplate mongoTemplate;
    
    /**
     * 查询附近的场馆（指定半径）
     */
    public List<Venue> findNearbyVenues(double longitude, double latitude, double radiusKm) {
        Point point = new Point(longitude, latitude);
        Distance distance = new Distance(radiusKm, Metrics.KILOMETERS);
        
        // 创建圆形区域
        Circle circle = new Circle(point, distance);
        
        Query query = new Query(Criteria.where("location").withinSphere(circle));
        
        return mongoTemplate.find(query, Venue.class);
    }
    
    /**
     * 查询附近的场馆并按距离排序
     */
    public List<Venue> findNearbyVenuesSorted(double longitude, double latitude, int limit) {
        Point point = new Point(longitude, latitude);
        
        NearQuery nearQuery = NearQuery.near(point)
                .maxDistance(new Distance(50, Metrics.KILOMETERS)) // 最大50公里
                .limit(limit)
                .spherical(true);
        
        GeoResults<Venue> results = mongoTemplate.geoNear(nearQuery, Venue.class);
        
        return results.getContent().stream()
                .map(GeoResult::getContent)
                .collect(Collectors.toList());
    }
    
    /**
     * 查询矩形区域内的场馆
     */
    public List<Venue> findVenuesInBox(double minLon, double minLat, double maxLon, double maxLat) {
        Point lowerLeft = new Point(minLon, minLat);
        Point upperRight = new Point(maxLon, maxLat);
        Box box = new Box(lowerLeft, upperRight);
        
        Query query = new Query(Criteria.where("location").within(box));
        
        return mongoTemplate.find(query, Venue.class);
    }
}
```

---

## 批量操作

```java
/**
 * 批量操作示例
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BatchOperationService {
    
    private final MongoTemplate mongoTemplate;
    private final OrderLogRepository orderLogRepository;
    
    /**
     * 批量插入（使用Repository）
     */
    public void batchInsertLogs(List<OrderLog> logs) {
        orderLogRepository.saveAll(logs);
        log.info("批量插入{}条日志", logs.size());
    }
    
    /**
     * 批量插入（使用MongoTemplate）
     */
    public void batchInsertLogsOptimized(List<OrderLog> logs) {
        mongoTemplate.insertAll(logs);
        log.info("批量插入{}条日志（优化版）", logs.size());
    }
    
    /**
     * 批量更新
     */
    public long batchUpdateActionType(String oldType, String newType) {
        Query query = new Query(Criteria.where("action_type").is(oldType));
        Update update = new Update().set("action_type", newType);
        
        UpdateResult result = mongoTemplate.updateMulti(query, update, OrderLog.class);
        
        log.info("批量更新{}条记录", result.getModifiedCount());
        
        return result.getModifiedCount();
    }
    
    /**
     * 批量删除
     */
    public long batchDeleteOldLogs(int daysAgo) {
        long cutoffTime = LocalDateTime.now().minusDays(daysAgo)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        
        Query query = new Query(Criteria.where("timestamp").lt(cutoffTime));
        DeleteResult result = mongoTemplate.remove(query, OrderLog.class);
        
        log.info("批量删除{}条旧日志", result.getDeletedCount());
        
        return result.getDeletedCount();
    }
    
    /**
     * BulkOperations批量操作
     */
    public void bulkOperations(List<OrderLog> logsToInsert, List<OrderLog> logsToUpdate) {
        BulkOperations bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.ORDERED, OrderLog.class);
        
        // 批量插入
        bulkOps.insert(logsToInsert);
        
        // 批量更新
        for (OrderLog log : logsToUpdate) {
            Query query = new Query(Criteria.where("_id").is(log.getId()));
            Update update = new Update()
                    .set("description", log.getDescription())
                    .set("update_time", LocalDateTime.now());
            bulkOps.updateOne(query, update);
        }
        
        BulkWriteResult result = bulkOps.execute();
        
        log.info("批量操作完成：插入={}, 更新={}", 
                result.getInsertedCount(), result.getModifiedCount());
    }
}
```

---

## 票务系统完整示例

### 场景：完整的日志和审计系统

```java
/**
 * 票务系统日志和审计服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TicketingLogAuditService {
    
    private final MongoTemplate mongoTemplate;
    private final OrderLogRepository orderLogRepository;
    private final AuditEventRepository auditEventRepository;
    private final VenueRepository venueRepository;
    
    /**
     * 完整的订单操作记录（日志+审计）
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordOrderOperation(Order order, String action, String description,
                                     HttpServletRequest request) {
        // 1. 记录订单操作日志
        OrderLog log = createOrderLog(order, action, description, request);
        orderLogRepository.save(log);
        
        // 2. 记录审计事件
        AuditEvent audit = createAuditEvent(order, action, description, request);
        auditEventRepository.save(audit);
        
        log.info("订单操作已记录：订单={}, 操作={}", order.getOrderNo(), action);
    }
    
    /**
     * 查询订单的完整操作历史
     */
    public OrderOperationHistory getOrderHistory(String orderNo) {
        // 1. 查询订单日志
        List<OrderLog> logs = orderLogRepository.findByOrderNo(orderNo);
        
        // 2. 查询审计事件
        List<AuditEvent> audits = auditEventRepository.findByEventDataField("orderNo", orderNo);
        
        // 3. 组合结果
        OrderOperationHistory history = new OrderOperationHistory();
        history.setOrderNo(orderNo);
        history.setLogs(logs);
        history.setAudits(audits);
        history.setTotalOperations(logs.size());
        
        // 4. 统计各操作类型次数
        Map<String, Long> actionCounts = logs.stream()
                .collect(Collectors.groupingBy(OrderLog::getActionType, Collectors.counting()));
        history.setActionCounts(actionCounts);
        
        return history;
    }
    
    /**
     * 用户行为分析
     */
    public UserBehaviorReport analyzeUserBehavior(Long userId, int days) {
        long startTime = LocalDateTime.now().minusDays(days)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        
        // 1. 聚合查询：按操作类型统计
        Aggregation actionAgg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("user_id").is(userId)
                        .and("timestamp").gte(startTime)),
                Aggregation.group("action_type").count().as("count"),
                Aggregation.sort(Sort.Direction.DESC, "count")
        );
        
        List<ActionTypeCount> actionCounts = mongoTemplate.aggregate(
                actionAgg, "order_logs", ActionTypeCount.class).getMappedResults();
        
        // 2. 统计总操作次数
        long totalActions = orderLogRepository.countByUserIdAndTimestampGreaterThanEqual(
                userId, startTime);
        
        // 3. 查询最近的操作
        Query recentQuery = new Query(Criteria.where("user_id").is(userId))
                .with(Sort.by(Sort.Direction.DESC, "timestamp"))
                .limit(10);
        List<OrderLog> recentLogs = mongoTemplate.find(recentQuery, OrderLog.class);
        
        // 4. 组合结果
        UserBehaviorReport report = new UserBehaviorReport();
        report.setUserId(userId);
        report.setDays(days);
        report.setTotalActions(totalActions);
        report.setActionCounts(actionCounts);
        report.setRecentLogs(recentLogs);
        
        return report;
    }
    
    /**
     * 查询附近场馆的演出活动
     */
    public List<VenueWithShowtimes> findNearbyVenuesWithShowtimes(double longitude, double latitude,
                                                                   double radiusKm) {
        // 1. 查询附近的场馆
        Point point = new Point(longitude, latitude);
        Distance distance = new Distance(radiusKm, Metrics.KILOMETERS);
        Circle circle = new Circle(point, distance);
        
        Query venueQuery = new Query(Criteria.where("location").withinSphere(circle));
        List<Venue> venues = mongoTemplate.find(venueQuery, Venue.class);
        
        // 2. 为每个场馆查询演出（假设演出信息也在MongoDB中）
        List<VenueWithShowtimes> result = new ArrayList<>();
        for (Venue venue : venues) {
            Query showtimeQuery = new Query(Criteria.where("venue_id").is(venue.getId())
                    .and("status").is("UPCOMING"));
            List<ShowtimeDocument> showtimes = mongoTemplate.find(showtimeQuery, ShowtimeDocument.class);
            
            VenueWithShowtimes vws = new VenueWithShowtimes();
            vws.setVenue(venue);
            vws.setShowtimes(showtimes);
            
            result.add(vws);
        }
        
        return result;
    }
    
    /**
     * 定时清理过期日志
     */
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
    public void cleanupExpiredLogs() {
        // 删除30天前的日志
        long cutoffTime = LocalDateTime.now().minusDays(30)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        
        Query query = new Query(Criteria.where("timestamp").lt(cutoffTime));
        
        DeleteResult result = mongoTemplate.remove(query, OrderLog.class);
        
        log.info("定时清理完成，删除{}条过期日志", result.getDeletedCount());
    }
    
    // 辅助方法
    
    private OrderLog createOrderLog(Order order, String action, String description,
                                     HttpServletRequest request) {
        OrderLog log = new OrderLog();
        log.setOrderNo(order.getOrderNo());
        log.setUserId(order.getUserId());
        log.setActionType(action);
        log.setDescription(description);
        log.setIpAddress(getClientIp(request));
        log.setDeviceInfo(request.getHeader("User-Agent"));
        log.setBeforeStatus(order.getStatus());
        log.setTimestamp(System.currentTimeMillis());
        
        Map<String, Object> extraData = new HashMap<>();
        extraData.put("totalAmount", order.getTotalAmount());
        extraData.put("quantity", order.getQuantity());
        log.setExtraData(extraData);
        
        return log;
    }
    
    private AuditEvent createAuditEvent(Order order, String action, String description,
                                        HttpServletRequest request) {
        AuditEvent audit = new AuditEvent();
        audit.setEventType("ORDER_" + action);
        audit.setUserId(order.getUserId());
        audit.setDescription(description);
        audit.setIpAddress(getClientIp(request));
        audit.setTimestamp(System.currentTimeMillis());
        
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("orderNo", order.getOrderNo());
        eventData.put("showtimeId", order.getShowtimeId());
        eventData.put("totalAmount", order.getTotalAmount());
        audit.setEventData(eventData);
        
        return audit;
    }
    
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}

/**
 * 订单操作历史VO
 */
@Data
public class OrderOperationHistory {
    private String orderNo;
    private List<OrderLog> logs;
    private List<AuditEvent> audits;
    private Integer totalOperations;
    private Map<String, Long> actionCounts;
}

/**
 * 用户行为报告VO
 */
@Data
public class UserBehaviorReport {
    private Long userId;
    private Integer days;
    private Long totalActions;
    private List<ActionTypeCount> actionCounts;
    private List<OrderLog> recentLogs;
}

/**
 * 场馆及演出信息VO
 */
@Data
public class VenueWithShowtimes {
    private Venue venue;
    private List<ShowtimeDocument> showtimes;
}
```

---

## 最佳实践

### 1. 数据模型设计

- **嵌入 vs 引用**：频繁一起查询的数据使用嵌入，大文档或需要独立管理的数据使用引用
- **避免深层嵌套**：嵌套层级不要超过2-3层
- **适度冗余**：为了查询性能，可以适度冗余数据
- **使用合适的字段类型**：数字用Number而非String，日期用Date或Long

### 2. 查询优化

- **创建合适的索引**：为常用查询字段创建索引
- **使用投影**：只查询需要的字段
- **避免全表扫描**：查询条件尽量使用索引字段
- **使用聚合管道**：复杂统计使用聚合而非应用层计算

### 3. 写入优化

- **批量操作**：批量插入/更新比逐条操作效率高10倍以上
- **适度使用事务**：事务有性能开销，只在必要时使用
- **异步写入**：非关键数据可以异步写入

### 4. 数据生命周期管理

- **TTL索引**：自动清理过期数据
- **定时归档**：将历史数据归档到冷存储
- **分集合存储**：按时间分片（如按月创建集合）

### 5. MongoDB适用场景

**适合使用MongoDB的场景**：
- 日志数据（高写入，灵活结构）
- 审计记录（复杂查询，聚合分析）
- 事件溯源（时序数据，append-only）
- 用户行为追踪（大量写入，复杂查询）
- 地理位置数据（地理空间索引）
- 半结构化数据（JSON文档）

**不适合使用MongoDB的场景**：
- 强事务要求（复杂的多表事务）
- 关系型数据（大量JOIN操作）
- 核心业务数据（订单、支付等，建议MySQL）

---

## 相关文档

- [README.md](./README.md) - 模块介绍
- [CONFIG.md](./CONFIG.md) - 配置指南
- [TESTING.md](./TESTING.md) - 测试指南
- [ROADMAP.md](./ROADMAP.md) - 发展路线图

---

**最后更新**: 2025-11-20  
**文档版本**: v2.0
