# Nebula Data Persistence - 使用示例

> 基于 MyBatis-Plus 的数据持久层完整使用指南，以票务系统为例

## 目录

- [快速开始](#快速开始)
- [基础CRUD操作](#基础crud操作)
- [复杂查询](#复杂查询)
- [分页查询](#分页查询)
- [批量操作](#批量操作)
- [事务管理](#事务管理)
- [读写分离](#读写分离)
- [分库分表](#分库分表)
- [票务系统完整示例](#票务系统完整示例)
- [最佳实践](#最佳实践)

---

## 快速开始

### 添加依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-data-persistence</artifactId>
    <version>${nebula.version}</version>
</dependency>

<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
</dependency>
```

### 基础配置

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
          password: password
          driver-class-name: com.mysql.cj.jdbc.Driver
          pool:
            min-size: 5
            max-size: 20
            connection-timeout: 30s

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

---

## 基础CRUD操作

### 1. 定义基础实体类

框架提供了 `DefaultMetaObjectHandler` 自动填充创建时间、更新时间等字段：

```java
/**
 * 基础实体类
 * 所有业务实体继承此类，自动获得审计字段
 */
@Data
public abstract class BaseEntity implements Serializable {
    
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 创建时间（自动填充）
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /**
     * 更新时间（自动填充）
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    /**
     * 创建人ID（自动填充）
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;
    
    /**
     * 更新人ID（自动填充）
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
    
    /**
     * 乐观锁版本号（自动填充）
     */
    @TableField(fill = FieldFill.INSERT)
    @Version
    private Integer version;
    
    /**
     * 逻辑删除标记（自动填充）
     * 0-未删除，1-已删除
     */
    @TableField(fill = FieldFill.INSERT)
    @TableLogic
    private Integer deleted;
}
```

### 2. 定义业务实体

#### 示例：票务实体

```java
/**
 * 演出票务实体
 */
@Data
@TableName("t_showtime")
@EqualsAndHashCode(callSuper = true)
public class Showtime extends BaseEntity {
    
    /**
     * 演出名称
     */
    private String name;
    
    /**
     * 演出描述
     */
    private String description;
    
    /**
     * 演出场馆
     */
    private String venue;
    
    /**
     * 演出时间
     */
    private LocalDateTime showTime;
    
    /**
     * 演出结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 票价（元）
     */
    private BigDecimal price;
    
    /**
     * 总座位数
     */
    private Integer totalSeats;
    
    /**
     * 剩余座位数
     */
    private Integer availableSeats;
    
    /**
     * 状态：UPCOMING-即将开始，ONGOING-进行中，FINISHED-已结束，CANCELLED-已取消
     */
    private String status;
}
```

#### 示例：订单实体

```java
/**
 * 订单实体
 */
@Data
@TableName("t_order")
@EqualsAndHashCode(callSuper = true)
public class Order extends BaseEntity {
    
    /**
     * 订单号
     */
    private String orderNo;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 演出场次ID
     */
    private Long showtimeId;
    
    /**
     * 购买数量
     */
    private Integer quantity;
    
    /**
     * 座位号（逗号分隔）
     */
    private String seats;
    
    /**
     * 总金额
     */
    private BigDecimal totalAmount;
    
    /**
     * 订单状态：PENDING-待支付，PAID-已支付，CANCELLED-已取消，COMPLETED-已完成，REFUNDED-已退款
     */
    private String status;
    
    /**
     * 支付时间
     */
    private LocalDateTime payTime;
    
    /**
     * 过期时间（未支付订单自动取消）
     */
    private LocalDateTime expireTime;
}
```

#### 示例：电子票实体

```java
/**
 * 电子票实体
 */
@Data
@TableName("t_ticket")
@EqualsAndHashCode(callSuper = true)
public class Ticket extends BaseEntity {
    
    /**
     * 票号
     */
    private String ticketNo;
    
    /**
     * 订单号
     */
    private String orderNo;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 演出场次ID
     */
    private Long showtimeId;
    
    /**
     * 座位号
     */
    private String seatNo;
    
    /**
     * 票务二维码URL
     */
    private String qrCodeUrl;
    
    /**
     * 票务状态：VALID-有效，USED-已使用，EXPIRED-已过期，REFUNDED-已退款
     */
    private String status;
    
    /**
     * 验票时间
     */
    private LocalDateTime verifyTime;
}
```

### 3. 定义Mapper接口

```java
/**
 * 演出场次Mapper
 */
@Mapper
public interface ShowtimeMapper extends BaseMapper<Showtime> {
    // 继承 BaseMapper 即可获得所有CRUD方法，无需额外定义
}

/**
 * 订单Mapper
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {
    
    /**
     * 根据订单号查询
     */
    @Select("SELECT * FROM t_order WHERE order_no = #{orderNo} AND deleted = 0")
    Order selectByOrderNo(@Param("orderNo") String orderNo);
    
    /**
     * 查询即将过期的订单（未支付且过期时间在指定时间之前）
     */
    @Select("SELECT * FROM t_order WHERE status = 'PENDING' AND expire_time <= #{expireTime} AND deleted = 0")
    List<Order> selectExpiringOrders(@Param("expireTime") LocalDateTime expireTime);
}

/**
 * 电子票Mapper
 */
@Mapper
public interface TicketMapper extends BaseMapper<Ticket> {
    
    /**
     * 根据票号查询
     */
    @Select("SELECT * FROM t_ticket WHERE ticket_no = #{ticketNo} AND deleted = 0")
    Ticket selectByTicketNo(@Param("ticketNo") String ticketNo);
}
```

### 4. 定义Service层

```java
/**
 * 演出场次服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShowtimeService extends ServiceImpl<ShowtimeMapper, Showtime> {
    
    /**
     * 创建演出场次
     */
    public Long createShowtime(Showtime showtime) {
        log.info("创建演出场次: {}", showtime.getName());
        
        // 设置默认值
        showtime.setAvailableSeats(showtime.getTotalSeats());
        showtime.setStatus("UPCOMING");
        
        // MyBatis-Plus会自动填充createTime, updateTime, version, deleted
        boolean success = save(showtime);
        
        if (!success) {
            throw new SystemException("创建演出场次失败");
        }
        
        return showtime.getId();
    }
    
    /**
     * 更新剩余座位数（库存扣减）
     */
    public boolean updateAvailableSeats(Long showtimeId, int quantity) {
        log.info("更新演出场次{}剩余座位数，减少{}", showtimeId, quantity);
        
        // 乐观锁更新，防止超卖
        Showtime showtime = getById(showtimeId);
        if (showtime == null) {
            throw new BusinessException("演出场次不存在");
        }
        
        if (showtime.getAvailableSeats() < quantity) {
            throw new BusinessException("座位不足");
        }
        
        showtime.setAvailableSeats(showtime.getAvailableSeats() - quantity);
        
        // updateById 会自动检查version字段（乐观锁）
        boolean success = updateById(showtime);
        
        if (!success) {
            log.warn("更新座位数失败，可能是并发冲突，场次ID: {}", showtimeId);
        }
        
        return success;
    }
    
    /**
     * 查询即将开始的演出
     */
    public List<Showtime> getUpcomingShowtimes() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime futureTime = now.plusDays(30);
        
        return lambdaQuery()
                .eq(Showtime::getStatus, "UPCOMING")
                .between(Showtime::getShowTime, now, futureTime)
                .orderByAsc(Showtime::getShowTime)
                .list();
    }
}
```

```java
/**
 * 订单服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService extends ServiceImpl<OrderMapper, Order> {
    
    private final OrderMapper orderMapper;
    private final ShowtimeService showtimeService;
    private final IdGenerator idGenerator;
    
    /**
     * 创建订单
     */
    public String createOrder(Long userId, Long showtimeId, Integer quantity, String seats) {
        log.info("用户{}创建订单，演出{}, 数量{}", userId, showtimeId, quantity);
        
        // 检查演出场次
        Showtime showtime = showtimeService.getById(showtimeId);
        if (showtime == null) {
            throw new BusinessException("演出场次不存在");
        }
        
        if (showtime.getAvailableSeats() < quantity) {
            throw new BusinessException("座位不足");
        }
        
        // 计算总金额
        BigDecimal totalAmount = showtime.getPrice().multiply(new BigDecimal(quantity));
        
        // 生成订单号
        String orderNo = idGenerator.generateBusinessId("ORDER");
        
        // 创建订单
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setShowtimeId(showtimeId);
        order.setQuantity(quantity);
        order.setSeats(seats);
        order.setTotalAmount(totalAmount);
        order.setStatus("PENDING");
        order.setExpireTime(LocalDateTime.now().plusMinutes(30)); // 30分钟后过期
        
        save(order);
        
        log.info("订单创建成功，订单号: {}", orderNo);
        
        return orderNo;
    }
    
    /**
     * 根据订单号查询订单
     */
    public Order getOrderByOrderNo(String orderNo) {
        return orderMapper.selectByOrderNo(orderNo);
    }
    
    /**
     * 更新订单状态为已支付
     */
    public boolean markOrderPaid(String orderNo) {
        Order order = getOrderByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        
        if (!"PENDING".equals(order.getStatus())) {
            throw new BusinessException("订单状态不正确");
        }
        
        order.setStatus("PAID");
        order.setPayTime(LocalDateTime.now());
        
        return updateById(order);
    }
    
    /**
     * 查询用户的订单列表
     */
    public List<Order> getUserOrders(Long userId) {
        return lambdaQuery()
                .eq(Order::getUserId, userId)
                .orderByDesc(Order::getCreateTime)
                .list();
    }
    
    /**
     * 取消过期订单
     */
    public int cancelExpiredOrders() {
        LocalDateTime now = LocalDateTime.now();
        List<Order> expiredOrders = orderMapper.selectExpiringOrders(now);
        
        if (expiredOrders.isEmpty()) {
            return 0;
        }
        
        log.info("发现{}个过期订单，开始取消", expiredOrders.size());
        
        int count = 0;
        for (Order order : expiredOrders) {
            order.setStatus("CANCELLED");
            if (updateById(order)) {
                count++;
            }
        }
        
        log.info("成功取消{}个过期订单", count);
        
        return count;
    }
}
```

---

## 复杂查询

### 1. Lambda查询（推荐）

Lambda查询提供类型安全的查询构建方式：

```java
@Service
@RequiredArgsConstructor
public class ShowtimeQueryService {
    
    private final ShowtimeService showtimeService;
    
    /**
     * 按场馆和价格范围查询演出
     */
    public List<Showtime> searchShowtimes(String venue, BigDecimal minPrice, BigDecimal maxPrice) {
        return showtimeService.lambdaQuery()
                .eq(venue != null, Showtime::getVenue, venue)
                .ge(minPrice != null, Showtime::getPrice, minPrice)
                .le(maxPrice != null, Showtime::getPrice, maxPrice)
                .eq(Showtime::getStatus, "UPCOMING")
                .orderByAsc(Showtime::getShowTime)
                .list();
    }
    
    /**
     * 模糊搜索演出名称
     */
    public List<Showtime> searchByName(String keyword) {
        return showtimeService.lambdaQuery()
                .like(Showtime::getName, keyword)
                .or()
                .like(Showtime::getDescription, keyword)
                .eq(Showtime::getStatus, "UPCOMING")
                .list();
    }
    
    /**
     * 统计某场馆的演出数量
     */
    public long countByVenue(String venue) {
        return showtimeService.lambdaQuery()
                .eq(Showtime::getVenue, venue)
                .count();
    }
    
    /**
     * 查询即将开始且有余票的演出
     */
    public List<Showtime> getAvailableShowtimes() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextWeek = now.plusWeeks(1);
        
        return showtimeService.lambdaQuery()
                .eq(Showtime::getStatus, "UPCOMING")
                .gt(Showtime::getAvailableSeats, 0)
                .between(Showtime::getShowTime, now, nextWeek)
                .orderByAsc(Showtime::getShowTime)
                .list();
    }
    
    /**
     * 按多条件查询订单
     */
    public List<Order> searchOrders(Long userId, String status, LocalDateTime startTime, LocalDateTime endTime) {
        return orderService.lambdaQuery()
                .eq(userId != null, Order::getUserId, userId)
                .eq(status != null, Order::getStatus, status)
                .ge(startTime != null, Order::getCreateTime, startTime)
                .le(endTime != null, Order::getCreateTime, endTime)
                .orderByDesc(Order::getCreateTime)
                .list();
    }
}
```

### 2. QueryWrapper查询

对于更复杂的查询，可以使用 QueryWrapper：

```java
/**
 * 复杂查询示例
 */
public List<Order> complexQuery() {
    QueryWrapper<Order> wrapper = new QueryWrapper<>();
    
    wrapper.nested(w -> w
            .eq("status", "PAID")
            .or()
            .eq("status", "COMPLETED")
    )
    .ge("total_amount", 100)
    .apply("DATE(create_time) = {0}", "2025-01-20")
    .orderByDesc("create_time");
    
    return orderService.list(wrapper);
}

/**
 * 关联查询（使用自定义SQL）
 */
@Select("SELECT o.*, s.name AS showtime_name, s.venue " +
        "FROM t_order o " +
        "JOIN t_showtime s ON o.showtime_id = s.id " +
        "WHERE o.user_id = #{userId} AND o.deleted = 0")
List<OrderVO> selectOrdersWithShowtime(@Param("userId") Long userId);
```

---

## 分页查询

### 1. 基础分页

```java
/**
 * 分页查询演出
 */
public Page<Showtime> getShowtimesPage(int pageNum, int pageSize) {
    Page<Showtime> page = new Page<>(pageNum, pageSize);
    
    return showtimeService.lambdaQuery()
            .eq(Showtime::getStatus, "UPCOMING")
            .orderByAsc(Showtime::getShowTime)
            .page(page);
}
```

### 2. 分页查询返回VO

```java
/**
 * 分页查询订单并转换为VO
 */
@Service
@RequiredArgsConstructor
public class OrderQueryService {
    
    private final OrderService orderService;
    private final ShowtimeService showtimeService;
    
    public PageResult<OrderVO> getOrdersPage(Long userId, int pageNum, int pageSize) {
        // 分页查询订单
        Page<Order> page = new Page<>(pageNum, pageSize);
        page = orderService.lambdaQuery()
                .eq(Order::getUserId, userId)
                .orderByDesc(Order::getCreateTime)
                .page(page);
        
        // 转换为VO
        List<OrderVO> voList = page.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        
        // 返回分页结果
        return PageResult.<OrderVO>builder()
                .records(voList)
                .total(page.getTotal())
                .pageNum(pageNum)
                .pageSize(pageSize)
                .pages(page.getPages())
                .build();
    }
    
    private OrderVO convertToVO(Order order) {
        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order, vo);
        
        // 查询关联的演出信息
        Showtime showtime = showtimeService.getById(order.getShowtimeId());
        if (showtime != null) {
            vo.setShowtimeName(showtime.getName());
            vo.setVenue(showtime.getVenue());
            vo.setShowTime(showtime.getShowTime());
        }
        
        return vo;
    }
}
```

---

## 批量操作

### 1. 批量插入

```java
/**
 * 批量生成电子票
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TicketService extends ServiceImpl<TicketMapper, Ticket> {
    
    private final IdGenerator idGenerator;
    
    /**
     * 为订单批量生成电子票
     */
    public List<String> batchGenerateTickets(Order order) {
        List<String> seatNos = Arrays.asList(order.getSeats().split(","));
        List<Ticket> tickets = new ArrayList<>();
        List<String> ticketNos = new ArrayList<>();
        
        for (String seatNo : seatNos) {
            String ticketNo = idGenerator.generateBusinessId("TICKET");
            
            Ticket ticket = new Ticket();
            ticket.setTicketNo(ticketNo);
            ticket.setOrderNo(order.getOrderNo());
            ticket.setUserId(order.getUserId());
            ticket.setShowtimeId(order.getShowtimeId());
            ticket.setSeatNo(seatNo.trim());
            ticket.setStatus("VALID");
            // qrCodeUrl 后续生成
            
            tickets.add(ticket);
            ticketNos.add(ticketNo);
        }
        
        // 批量插入，每批1000条
        boolean success = saveBatch(tickets, 1000);
        
        if (!success) {
            throw new SystemException("批量生成电子票失败");
        }
        
        log.info("为订单{}批量生成{}张电子票", order.getOrderNo(), tickets.size());
        
        return ticketNos;
    }
}
```

### 2. 批量更新

```java
/**
 * 批量更新订单状态
 */
public boolean batchUpdateOrderStatus(List<String> orderNos, String newStatus) {
    List<Order> orders = lambdaQuery()
            .in(Order::getOrderNo, orderNos)
            .list();
    
    if (orders.isEmpty()) {
        return false;
    }
    
    orders.forEach(order -> order.setStatus(newStatus));
    
    // 批量更新
    return updateBatchById(orders, 1000);
}
```

### 3. 批量删除（逻辑删除）

```java
/**
 * 批量删除过期的未支付订单
 */
public int batchRemoveExpiredOrders(LocalDateTime expireTime) {
    List<Order> expiredOrders = lambdaQuery()
            .eq(Order::getStatus, "PENDING")
            .le(Order::getExpireTime, expireTime)
            .list();
    
    if (expiredOrders.isEmpty()) {
        return 0;
    }
    
    List<Long> orderIds = expiredOrders.stream()
            .map(Order::getId)
            .collect(Collectors.toList());
    
    // 批量逻辑删除
    boolean success = removeByIds(orderIds);
    
    return success ? orderIds.size() : 0;
}
```

---

## 事务管理

### 1. 声明式事务

```java
/**
 * 购票服务（完整事务示例）
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TicketPurchaseService {
    
    private final ShowtimeService showtimeService;
    private final OrderService orderService;
    private final TicketService ticketService;
    
    /**
     * 购买票务（事务处理）
     */
    @Transactional(rollbackFor = Exception.class)
    public String purchaseTickets(Long userId, Long showtimeId, Integer quantity, String seats) {
        log.info("用户{}购买票务，演出{}, 数量{}", userId, showtimeId, quantity);
        
        // 1. 检查演出场次
        Showtime showtime = showtimeService.getById(showtimeId);
        if (showtime == null) {
            throw new BusinessException("演出场次不存在");
        }
        
        if (!"UPCOMING".equals(showtime.getStatus())) {
            throw new BusinessException("演出不可购买");
        }
        
        // 2. 扣减库存（乐观锁，防止超卖）
        boolean stockReduced = false;
        int retryCount = 0;
        int maxRetries = 3;
        
        while (!stockReduced && retryCount < maxRetries) {
            try {
                stockReduced = showtimeService.updateAvailableSeats(showtimeId, quantity);
            } catch (Exception e) {
                retryCount++;
                log.warn("扣减库存失败，重试次数: {}", retryCount);
                if (retryCount >= maxRetries) {
                    throw new BusinessException("库存不足或并发冲突，请稍后重试");
                }
            }
        }
        
        // 3. 创建订单
        String orderNo = orderService.createOrder(userId, showtimeId, quantity, seats);
        
        // 4. 生成电子票
        Order order = orderService.getOrderByOrderNo(orderNo);
        List<String> ticketNos = ticketService.batchGenerateTickets(order);
        
        log.info("购票成功，订单号: {}, 生成{}张电子票", orderNo, ticketNos.size());
        
        return orderNo;
    }
    
    /**
     * 支付订单（事务处理）
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean payOrder(String orderNo) {
        log.info("处理订单支付: {}", orderNo);
        
        // 1. 更新订单状态
        boolean orderUpdated = orderService.markOrderPaid(orderNo);
        if (!orderUpdated) {
            throw new BusinessException("订单状态更新失败");
        }
        
        // 2. 生成支付记录（此处省略）
        
        // 3. 发送通知（此处省略）
        
        log.info("订单{}支付成功", orderNo);
        
        return true;
    }
    
    /**
     * 退票（事务处理）
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean refundTickets(String orderNo) {
        log.info("处理退票: {}", orderNo);
        
        // 1. 查询订单
        Order order = orderService.getOrderByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        
        if (!"PAID".equals(order.getStatus())) {
            throw new BusinessException("订单状态不正确，无法退票");
        }
        
        // 2. 检查演出是否已开始
        Showtime showtime = showtimeService.getById(order.getShowtimeId());
        if (LocalDateTime.now().isAfter(showtime.getShowTime())) {
            throw new BusinessException("演出已开始，无法退票");
        }
        
        // 3. 更新订单状态
        order.setStatus("REFUNDED");
        orderService.updateById(order);
        
        // 4. 恢复库存
        showtimeService.lambdaUpdate()
                .setSql("available_seats = available_seats + " + order.getQuantity())
                .eq(Showtime::getId, order.getShowtimeId())
                .update();
        
        // 5. 作废电子票
        ticketService.lambdaUpdate()
                .set(Ticket::getStatus, "REFUNDED")
                .eq(Ticket::getOrderNo, orderNo)
                .update();
        
        log.info("订单{}退票成功", orderNo);
        
        return true;
    }
}
```

### 2. 编程式事务

```java
/**
 * 使用TransactionTemplate进行编程式事务管理
 */
@Service
@RequiredArgsConstructor
public class OrderRefundService {
    
    private final TransactionTemplate transactionTemplate;
    private final OrderService orderService;
    private final TicketService ticketService;
    
    public boolean refundOrderProgrammatically(String orderNo) {
        return Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            try {
                // 业务逻辑
                Order order = orderService.getOrderByOrderNo(orderNo);
                order.setStatus("REFUNDED");
                orderService.updateById(order);
                
                // 作废电子票
                ticketService.lambdaUpdate()
                        .set(Ticket::getStatus, "REFUNDED")
                        .eq(Ticket::getOrderNo, orderNo)
                        .update();
                
                return true;
            } catch (Exception e) {
                // 手动回滚
                status.setRollbackOnly();
                return false;
            }
        }));
    }
}
```

---

## 读写分离

### 1. 配置读写分离

参见 [CONFIG.md](./CONFIG.md#读写分离配置)

### 2. 使用注解控制数据源

```java
/**
 * 读写分离示例
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderReadWriteService {
    
    private final OrderService orderService;
    
    /**
     * 查询订单（从库）
     * 默认读操作会路由到从库
     */
    public Order getOrder(String orderNo) {
        return orderService.getOrderByOrderNo(orderNo);
    }
    
    /**
     * 支付后立即查询订单（强制主库）
     * 避免主从延迟导致查询不到最新数据
     */
    @Transactional(readOnly = false)
    public Order getOrderAfterPayment(String orderNo) {
        // 事务中的查询会自动使用主库
        return orderService.getOrderByOrderNo(orderNo);
    }
    
    /**
     * 统计订单数量（从库）
     */
    public long countOrders(Long userId) {
        return orderService.lambdaQuery()
                .eq(Order::getUserId, userId)
                .count();
    }
    
    /**
     * 创建订单（主库）
     * 写操作自动路由到主库
     */
    public String createOrder(Long userId, Long showtimeId, Integer quantity, String seats) {
        return orderService.createOrder(userId, showtimeId, quantity, seats);
    }
}
```

### 3. 主从延迟处理

```java
/**
 * 处理主从延迟的最佳实践
 */
@Service
@RequiredArgsConstructor
public class OrderConsistencyService {
    
    private final OrderService orderService;
    private final StringRedisTemplate redisTemplate;
    
    /**
     * 创建订单后立即查询（使用缓存避免主从延迟）
     */
    public Order createAndGetOrder(Long userId, Long showtimeId, Integer quantity, String seats) {
        // 1. 创建订单（主库）
        String orderNo = orderService.createOrder(userId, showtimeId, quantity, seats);
        
        // 2. 写入缓存
        Order order = orderService.getOrderByOrderNo(orderNo); // 主库查询
        String cacheKey = "order:" + orderNo;
        redisTemplate.opsForValue().set(cacheKey, JsonUtils.toJson(order), 5, TimeUnit.MINUTES);
        
        return order;
    }
    
    /**
     * 查询订单（优先缓存）
     */
    public Order getOrder(String orderNo) {
        // 1. 先查缓存
        String cacheKey = "order:" + orderNo;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return JsonUtils.fromJson(cached, Order.class);
        }
        
        // 2. 查询数据库（从库）
        Order order = orderService.getOrderByOrderNo(orderNo);
        
        // 3. 写入缓存
        if (order != null) {
            redisTemplate.opsForValue().set(cacheKey, JsonUtils.toJson(order), 5, TimeUnit.MINUTES);
        }
        
        return order;
    }
}
```

---

## 分库分表

### 1. 分库分表配置

参见 [CONFIG.md](./CONFIG.md#分库分表配置)

### 2. 使用分片键查询

```java
/**
 * 分库分表查询示例
 */
@Service
@RequiredArgsConstructor
public class ShardingQueryService {
    
    private final OrderService orderService;
    
    /**
     * 根据用户ID查询订单（使用分片键）
     * 会路由到对应的分库
     */
    public List<Order> getUserOrders(Long userId) {
        return orderService.lambdaQuery()
                .eq(Order::getUserId, userId) // userId是分片键
                .orderByDesc(Order::getCreateTime)
                .list();
    }
    
    /**
     * 根据用户ID和时间范围查询订单（使用分片键和分表键）
     * 会路由到对应的分库和分表
     */
    public List<Order> getUserOrdersByMonth(Long userId, YearMonth month) {
        LocalDateTime startTime = month.atDay(1).atStartOfDay();
        LocalDateTime endTime = month.atEndOfMonth().atTime(LocalTime.MAX);
        
        return orderService.lambdaQuery()
                .eq(Order::getUserId, userId) // 分库分片键
                .between(Order::getCreateTime, startTime, endTime) // 分表分片键
                .orderByDesc(Order::getCreateTime)
                .list();
    }
    
    /**
     * 不使用分片键的查询（广播查询，性能较差）
     * 会扫描所有分库分表
     */
    public Order getOrderByOrderNo(String orderNo) {
        // 警告：此查询不包含分片键，会进行全库扫描
        return orderService.lambdaQuery()
                .eq(Order::getOrderNo, orderNo)
                .one();
    }
}
```

### 3. 分片事务

```java
/**
 * 跨分片事务示例
 */
@Service
@RequiredArgsConstructor
public class ShardingTransactionService {
    
    private final OrderService orderService;
    private final ShowtimeService showtimeService;
    
    /**
     * 跨分片创建订单
     * 注意：跨分片事务性能较差，建议避免
     */
    @Transactional(rollbackFor = Exception.class)
    public String createOrderAcrossShards(Long userId, Long showtimeId, Integer quantity, String seats) {
        // 1. 扣减演出库存（可能在不同分片）
        boolean stockReduced = showtimeService.updateAvailableSeats(showtimeId, quantity);
        if (!stockReduced) {
            throw new BusinessException("库存不足");
        }
        
        // 2. 创建订单（根据userId路由到对应分片）
        String orderNo = orderService.createOrder(userId, showtimeId, quantity, seats);
        
        return orderNo;
    }
}
```

---

## 票务系统完整示例

### 场景：用户购票完整流程

```java
/**
 * 票务购买完整流程示例
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CompletePurchaseService {
    
    private final ShowtimeService showtimeService;
    private final OrderService orderService;
    private final TicketService ticketService;
    private final LockService lockService; // 分布式锁服务
    
    /**
     * 完整购票流程
     */
    @Transactional(rollbackFor = Exception.class)
    public PurchaseResult completePurchase(PurchaseRequest request) {
        Long userId = request.getUserId();
        Long showtimeId = request.getShowtimeId();
        Integer quantity = request.getQuantity();
        List<String> seatNos = request.getSeatNos();
        
        log.info("开始购票流程：用户={}, 演出={}, 数量={}", userId, showtimeId, quantity);
        
        // 1. 验证演出场次
        Showtime showtime = validateShowtime(showtimeId);
        
        // 2. 锁定座位（使用分布式锁防止超卖）
        String lockKey = "showtime:seat:lock:" + showtimeId;
        boolean locked = lockService.tryLock(lockKey, 30, TimeUnit.SECONDS);
        if (!locked) {
            throw new BusinessException("系统繁忙，请稍后重试");
        }
        
        try {
            // 3. 验证座位是否可用
            validateSeats(showtimeId, seatNos);
            
            // 4. 扣减库存
            boolean stockReduced = showtimeService.updateAvailableSeats(showtimeId, quantity);
            if (!stockReduced) {
                throw new BusinessException("座位不足或并发冲突");
            }
            
            // 5. 创建订单
            String seats = String.join(",", seatNos);
            String orderNo = orderService.createOrder(userId, showtimeId, quantity, seats);
            
            // 6. 标记座位已占用（省略实现）
            markSeatsOccupied(showtimeId, seatNos, orderNo);
            
            // 7. 生成待支付的电子票
            Order order = orderService.getOrderByOrderNo(orderNo);
            List<String> ticketNos = ticketService.batchGenerateTickets(order);
            
            log.info("购票流程完成：订单号={}, 票数={}", orderNo, ticketNos.size());
            
            return PurchaseResult.builder()
                    .success(true)
                    .orderNo(orderNo)
                    .ticketNos(ticketNos)
                    .totalAmount(order.getTotalAmount())
                    .expireTime(order.getExpireTime())
                    .build();
        } finally {
            // 8. 释放锁
            lockService.unlock(lockKey);
        }
    }
    
    private Showtime validateShowtime(Long showtimeId) {
        Showtime showtime = showtimeService.getById(showtimeId);
        if (showtime == null) {
            throw new BusinessException("演出场次不存在");
        }
        
        if (!"UPCOMING".equals(showtime.getStatus())) {
            throw new BusinessException("演出已开始或已结束，无法购票");
        }
        
        if (LocalDateTime.now().isAfter(showtime.getShowTime().minusHours(2))) {
            throw new BusinessException("演出开始前2小时停止售票");
        }
        
        return showtime;
    }
    
    private void validateSeats(Long showtimeId, List<String> seatNos) {
        // 检查座位是否已被占用（省略实现）
        // 实际应该查询座位占用表
        for (String seatNo : seatNos) {
            boolean occupied = checkSeatOccupied(showtimeId, seatNo);
            if (occupied) {
                throw new BusinessException("座位 " + seatNo + " 已被占用");
            }
        }
    }
    
    private boolean checkSeatOccupied(Long showtimeId, String seatNo) {
        // 省略实现
        return false;
    }
    
    private void markSeatsOccupied(Long showtimeId, List<String> seatNos, String orderNo) {
        // 省略实现
        log.info("标记座位已占用：演出={}, 座位={}, 订单={}", showtimeId, seatNos, orderNo);
    }
}

/**
 * 购票请求
 */
@Data
public class PurchaseRequest {
    private Long userId;
    private Long showtimeId;
    private Integer quantity;
    private List<String> seatNos;
}

/**
 * 购票结果
 */
@Data
@Builder
public class PurchaseResult {
    private Boolean success;
    private String orderNo;
    private List<String> ticketNos;
    private BigDecimal totalAmount;
    private LocalDateTime expireTime;
}
```

---

## 最佳实践

### 1. 实体类设计

- **使用基础实体类**：所有实体继承 `BaseEntity`，自动获得审计字段
- **使用 @TableName**：明确指定表名，避免命名冲突
- **使用 @TableField**：控制字段映射、填充策略、查询条件
- **逻辑删除**：使用 `@TableLogic` 标记删除字段
- **乐观锁**：使用 `@Version` 标记版本字段

### 2. Service层设计

- **继承ServiceImpl**：获得MyBatis-Plus的增强CRUD方法
- **使用Lambda查询**：类型安全，避免硬编码字段名
- **合理使用事务**：只在必要的地方使用 `@Transactional`
- **异常处理**：抛出业务异常，让事务自动回滚

### 3. 性能优化

- **批量操作**：使用 `saveBatch`、`updateBatchById` 等批量方法
- **分页查询**：使用 `Page` 对象进行分页，避免全表扫描
- **索引优化**：确保查询字段建立索引
- **读写分离**：读多写少的场景使用读写分离
- **避免N+1查询**：使用关联查询或缓存

### 4. 分库分表最佳实践

- **合理选择分片键**：根据业务查询模式选择分片键
- **避免跨分片查询**：尽量在单个分片内完成查询
- **分片键必须在查询条件中**：提高查询效率
- **避免跨分片事务**：分布式事务性能较差

### 5. 数据一致性

- **乐观锁防超卖**：使用 `@Version` 字段
- **分布式锁**：高并发场景使用分布式锁
- **缓存一致性**：更新数据库时同步更新缓存
- **主从延迟处理**：关键业务使用主库查询或缓存

### 6. 日志和监控

- **记录关键操作**：创建、更新、删除订单等关键操作需记录日志
- **慢SQL监控**：监控慢SQL，及时优化
- **异常监控**：监控数据库连接异常、事务回滚等

---

## 相关文档

- [README.md](./README.md) - 模块介绍
- [CONFIG.md](./CONFIG.md) - 配置指南（包含票务系统配置）
- [TESTING.md](./TESTING.md) - 测试指南
- [ROADMAP.md](./ROADMAP.md) - 发展路线图

---

**最后更新**: 2025-11-20  
**文档版本**: v2.0
