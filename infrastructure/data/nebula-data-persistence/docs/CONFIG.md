# Nebula Data Persistence 配置指南

> MyBatis-Plus数据持久层配置说明

## 目录

- [概述](#概述)
- [基本配置](#基本配置)
- [读写分离配置](#读写分离配置)
- [分库分表配置](#分库分表配置)
- [连接池配置](#连接池配置)
- [票务系统场景配置](#票务系统场景配置)
- [性能优化配置](#性能优化配置)

## 概述

`nebula-data-persistence` 基于 MyBatis-Plus 和 ShardingSphere,提供企业级数据持久层能力。

### 配置层次

```
1. application-{profile}.yml  (环境配置)
2. application.yml            (通用配置)
3. Java @Configuration       (代码配置)
4. 默认值                     (模块内置)
```

## 基本配置

### Maven依赖

```xml
<dependency>
    <groupId>com.andy.nebula</groupId>
    <artifactId>nebula-data-persistence</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>
```

### 单数据源配置

```yaml
nebula:
  data:
    persistence:
      enabled: true
      primary: primary
      sources:
        primary:
          type: mysql
          url: jdbc:mysql://localhost:3306/ticket_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
          username: root
          password: ${DB_PASSWORD}
          driver-class-name: com.mysql.cj.jdbc.Driver
          pool:
            min-size: 5
            max-size: 20
            connection-timeout: 30s

# MyBatis-Plus配置
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl
  global-config:
    db-config:
      id-type: auto
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
```

## 读写分离配置

### 票务系统读写分离

```yaml
nebula:
  data:
    persistence:
      enabled: true
      primary: master
      read-write-splitting:
        enabled: true
        # 读写分离策略: round-robin(轮询) / random(随机) / weight(权重)
        load-balance-algorithm: round-robin
      
      sources:
        # 主库(写)
        master:
          type: mysql
          url: jdbc:mysql://master-db:3306/ticket_db
          username: root
          password: ${DB_MASTER_PASSWORD}
          driver-class-name: com.mysql.cj.jdbc.Driver
          pool:
            min-size: 10
            max-size: 50
            connection-timeout: 30s
        
        # 从库1(读)
        slave1:
          type: mysql
          url: jdbc:mysql://slave1-db:3306/ticket_db
          username: readonly
          password: ${DB_SLAVE_PASSWORD}
          driver-class-name: com.mysql.cj.jdbc.Driver
          pool:
            min-size: 5
            max-size: 30
            connection-timeout: 30s
        
        # 从库2(读)
        slave2:
          type: mysql
          url: jdbc:mysql://slave2-db:3306/ticket_db
          username: readonly
          password: ${DB_SLAVE_PASSWORD}
          driver-class-name: com.mysql.cj.jdbc.Driver
          pool:
            min-size: 5
            max-size: 30
            connection-timeout: 30s
```

### 强制主库读取

```java
@Service
public class OrderService {
    
    /**
     * 支付后立即查询订单(强制主库,避免主从延迟)
     */
    @ReadWrite(mode = ReadWriteMode.WRITE)
    public Order getOrderAfterPayment(String orderNo) {
        return orderMapper.selectByOrderNo(orderNo);
    }
    
    /**
     * 普通查询(从库)
     */
    public Order getOrder(String orderNo) {
        return orderMapper.selectByOrderNo(orderNo);
    }
}
```

## 分库分表配置

### 票务系统分库分表方案

**需求**:
- 用户表: 按 user_id % 16 分库
- 订单表: 按 user_id hash 分库,按月份分表
- 日志表: 按月份分表

```yaml
nebula:
  data:
    persistence:
      enabled: true
      sharding:
        enabled: true
        # 默认数据源
        default-datasource: ds0
        
        # 数据源配置(16个分库)
        datasources:
          ds0:
            url: jdbc:mysql://db0:3306/ticket_db_0
            username: root
            password: ${DB_PASSWORD}
          ds1:
            url: jdbc:mysql://db1:3306/ticket_db_1
            username: root
            password: ${DB_PASSWORD}
          # ... ds2 到 ds15
        
        # 分片规则
        tables:
          # 用户表分库
          t_user:
            actual-data-nodes: ds$->{0..15}.t_user
            database-strategy:
              standard:
                sharding-column: user_id
                sharding-algorithm-name: user-mod-algorithm
          
          # 订单表分库分表
          t_order:
            actual-data-nodes: ds$->{0..15}.t_order_$->{2024..2026}_$->{01..12}
            database-strategy:
              standard:
                sharding-column: user_id
                sharding-algorithm-name: user-hash-algorithm
            table-strategy:
              standard:
                sharding-column: create_time
                sharding-algorithm-name: order-month-algorithm
          
          # 日志表按月分表
          t_order_log:
            actual-data-nodes: ds0.t_order_log_$->{2024..2026}_$->{01..12}
            table-strategy:
              standard:
                sharding-column: create_time
                sharding-algorithm-name: log-month-algorithm
        
        # 分片算法
        sharding-algorithms:
          # 用户ID取模算法
          user-mod-algorithm:
            type: MOD
            props:
              sharding-count: 16
          
          # 用户ID哈希算法
          user-hash-algorithm:
            type: HASH_MOD
            props:
              sharding-count: 16
          
          # 订单按月分表算法
          order-month-algorithm:
            type: CLASS_BASED
            props:
              strategy: STANDARD
              algorithm-class-name: com.andy.ticket.sharding.OrderMonthShardingAlgorithm
          
          # 日志按月分表算法
          log-month-algorithm:
            type: CLASS_BASED
            props:
              strategy: STANDARD
              algorithm-class-name: com.andy.ticket.sharding.LogMonthShardingAlgorithm
```

### 自定义分片算法

```java
/**
 * 订单按月分表算法
 */
public class OrderMonthShardingAlgorithm implements StandardShardingAlgorithm<LocalDateTime> {
    
    @Override
    public String doSharding(
            Collection<String> availableTargetNames,
            PreciseShardingValue<LocalDateTime> shardingValue) {
        
        LocalDateTime createTime = shardingValue.getValue();
        String suffix = DateTimeFormatter.ofPattern("yyyy_MM").format(createTime);
        
        String targetTable = "t_order_" + suffix;
        
        if (availableTargetNames.contains(targetTable)) {
            return targetTable;
        }
        
        throw new IllegalArgumentException("表不存在: " + targetTable);
    }
    
    @Override
    public Collection<String> doSharding(
            Collection<String> availableTargetNames,
            RangeShardingValue<LocalDateTime> shardingValue) {
        
        Range<LocalDateTime> range = shardingValue.getValueRange();
        LocalDateTime start = range.lowerEndpoint();
        LocalDateTime end = range.upperEndpoint();
        
        Set<String> tables = new HashSet<>();
        YearMonth current = YearMonth.from(start);
        YearMonth endMonth = YearMonth.from(end);
        
        while (!current.isAfter(endMonth)) {
            String suffix = current.format(DateTimeFormatter.ofPattern("yyyy_MM"));
            String targetTable = "t_order_" + suffix;
            
            if (availableTargetNames.contains(targetTable)) {
                tables.add(targetTable);
            }
            
            current = current.plusMonths(1);
        }
        
        return tables;
    }
}
```

## 连接池配置

### HikariCP 配置

```yaml
nebula:
  data:
    persistence:
      sources:
        primary:
          pool:
            # 连接池类型
            type: hikari
            
            # 最小连接数
            min-size: 10
            
            # 最大连接数
            max-size: 50
            
            # 连接超时(秒)
            connection-timeout: 30s
            
            # 空闲超时(分钟)
            idle-timeout: 10m
            
            # 连接最大生命周期(分钟)
            max-lifetime: 30m
            
            # 连接测试查询
            connection-test-query: SELECT 1
            
            # 连接池名称
            pool-name: TicketPool
            
            # 自动提交
            auto-commit: true
            
            # 只读
            read-only: false
```

## 票务系统场景配置

### 高并发购票配置

```yaml
# 生产环境配置
nebula:
  data:
    persistence:
      enabled: true
      primary: master
      
      # 读写分离
      read-write-splitting:
        enabled: true
        load-balance-algorithm: round-robin
      
      sources:
        # 主库(写)
        master:
          url: jdbc:mysql://master-db:3306/ticket_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&rewriteBatchedStatements=true
          username: root
          password: ${DB_MASTER_PASSWORD}
          pool:
            min-size: 20
            max-size: 100          # 高并发需要更多连接
            connection-timeout: 10s  # 更短的超时时间
            idle-timeout: 5m
            max-lifetime: 20m
            connection-test-query: SELECT 1
        
        # 从库1(读)
        slave1:
          url: jdbc:mysql://slave1-db:3306/ticket_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
          username: readonly
          password: ${DB_SLAVE_PASSWORD}
          pool:
            min-size: 10
            max-size: 50
            connection-timeout: 10s
        
        # 从库2(读)
        slave2:
          url: jdbc:mysql://slave2-db:3306/ticket_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
          username: readonly
          password: ${DB_SLAVE_PASSWORD}
          pool:
            min-size: 10
            max-size: 50
            connection-timeout: 10s

# MyBatis-Plus优化配置
mybatis-plus:
  configuration:
    # 开启驼峰命名
    map-underscore-to-camel-case: true
    # 关闭自动映射
    auto-mapping-behavior: partial
    # 设置合理的默认超时
    default-statement-timeout: 30
    # 本地缓存
    local-cache-scope: statement
  
  global-config:
    # 关闭Banner
    banner: false
    db-config:
      # 主键类型
      id-type: auto
      # 逻辑删除
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
      # 乐观锁字段
      update-strategy: not_empty
```

### 批量插入优化

```java
@Service
public class TicketService {
    
    /**
     * 批量生成电子票
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchGenerateTickets(Order order) {
        List<String> seatNos = Arrays.asList(order.getSeats().split(","));
        List<Ticket> tickets = new ArrayList<>();
        
        for (String seatNo : seatNos) {
            Ticket ticket = new Ticket();
            ticket.setTicketNo(businessIdService.generateTicketNo());
            ticket.setOrderNo(order.getOrderNo());
            ticket.setSeatNo(seatNo);
            ticket.setStatus("VALID");
            tickets.add(ticket);
        }
        
        // 批量插入(一次性插入所有票)
        ticketService.saveBatch(tickets, 1000);  // 每批1000条
    }
}
```

## 性能优化配置

### SQL日志配置

```yaml
# 开发环境
mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl

# 生产环境
mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl

logging:
  level:
    com.andy.ticket.mapper: INFO  # 生产环境不打印SQL
```

### 分页插件配置

```java
@Configuration
public class MybatisPlusConfig {
    
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        
        // 分页插件
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor();
        paginationInterceptor.setMaxLimit(1000L);  // 最大查询1000条
        paginationInterceptor.setOverflow(false);   // 不允许溢出
        paginationInterceptor.setDbType(DbType.MYSQL);
        
        interceptor.addInnerInterceptor(paginationInterceptor);
        
        // 乐观锁插件
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        
        return interceptor;
    }
}
```

## 环境配置

### 开发环境

```yaml
nebula:
  data:
    persistence:
      enabled: true
      primary: primary
      sources:
        primary:
          url: jdbc:mysql://localhost:3306/ticket_dev
          username: root
          password: root
          pool:
            min-size: 2
            max-size: 10

mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl  # 打印SQL
```

### 生产环境

```yaml
nebula:
  data:
    persistence:
      enabled: true
      primary: master
      read-write-splitting:
        enabled: true
      sources:
        master:
          url: jdbc:mysql://${DB_HOST}:3306/ticket_prod?useSSL=true
          username: ${DB_USERNAME}
          password: ${DB_PASSWORD}
          pool:
            min-size: 20
            max-size: 100

mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl  # 使用日志框架
```

## 相关文档

- [README](README.md) - 模块介绍
- [EXAMPLE](EXAMPLE.md) - 使用示例
- [TESTING](TESTING.md) - 测试指南
- [ROADMAP](ROADMAP.md) - 发展路线图

---

**最后更新**: 2025-11-20  
**文档版本**: v1.0

