# Nebula 框架快速开始

> 本文档提供不同场景下的快速开始指南，帮助您快速上手 Nebula 框架

## 环境准备

### 系统要求

| 组件 | 版本要求 | 说明 |
|-----|---------|------|
| JDK | 21+ | 必需 |
| Maven | 3.8+ | 构建工具 |
| IDE | IDEA 2023+ / Eclipse | 推荐 IDEA |
| Docker | 20.10+ | 可选（用于本地中间件）|

### 安装 JDK 21

```bash
# macOS (使用 Homebrew)
brew install openjdk@21

# Linux (Ubuntu/Debian)
sudo apt-get install openjdk-21-jdk

# 验证安装
java -version
# 应输出: openjdk version "21.0.x"
```

### 安装 Maven

```bash
# macOS
brew install maven

# Linux
sudo apt-get install maven

# 验证安装
mvn -version
# 应输出: Apache Maven 3.8.x
```

### 配置 Maven

在 `~/.m2/settings.xml` 中配置（如果没有则创建）：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 
          http://maven.apache.org/xsd/settings-1.0.0.xsd">
    
    <mirrors>
        <!-- 阿里云镜像（可选，提升下载速度） -->
        <mirror>
            <id>alimaven</id>
            <name>aliyun maven</name>
            <url>https://maven.aliyun.com/repository/public</url>
            <mirrorOf>central</mirrorOf>
        </mirror>
    </mirrors>
</settings>
```

## 场景1：单体 Web 应用

> 适合：中小型项目、快速原型、学习使用

### 步骤1：创建项目

使用 Maven 创建项目：

```bash
mvn archetype:generate \
    -DgroupId=com.example \
    -DartifactId=demo-web \
    -DarchetypeArtifactId=maven-archetype-quickstart \
    -DinteractiveMode=false

cd demo-web
```

### 步骤2：添加依赖

编辑 `pom.xml`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>demo-web</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.5.8</version>
    </parent>

    <properties>
        <java.version>21</java.version>
        <nebula.version>2.0.1-SNAPSHOT</nebula.version>
    </properties>

    <dependencies>
        <!-- Nebula Web Starter -->
        <dependency>
            <groupId>io.nebula</groupId>
            <artifactId>nebula-starter-web</artifactId>
            <version>${nebula.version}</version>
        </dependency>

        <!-- 测试依赖 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

### 步骤3：创建配置文件

创建 `src/main/resources/application.yml`：

```yaml
spring:
  application:
    name: demo-web

server:
  port: 8080

nebula:
  # 启用 Web 支持
  web:
    enabled: true
  
  # 数据访问配置（如需要）
  data:
    persistence:
      enabled: true
  
  # 缓存配置（如需要）
  cache:
    enabled: true
    type: caffeine
```

### 步骤4：创建启动类

创建 `src/main/java/com/example/DemoWebApplication.java`：

```java
package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoWebApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoWebApplication.class, args);
    }
}
```

### 步骤5：创建第一个接口

创建 `src/main/java/com/example/controller/HelloController.java`：

```java
package com.example.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {
    
    @GetMapping("/hello")
    public String hello(@RequestParam(defaultValue = "World") String name) {
        return "Hello, " + name + "!";
    }
}
```

### 步骤6：运行应用

```bash
# 编译
mvn clean package

# 运行
mvn spring-boot:run

# 或者直接运行 JAR
java -jar target/demo-web-1.0.0.jar
```

### 步骤7：测试接口

```bash
# 访问接口
curl http://localhost:8080/hello
# 输出: Hello, World!

curl http://localhost:8080/hello?name=Nebula
# 输出: Hello, Nebula!
```

## 场景2：微服务应用

> 适合：大型项目、分布式系统、高并发场景

### 步骤1：准备中间件

使用 Docker Compose 启动必需的中间件：

创建 `docker-compose.yml`：

```yaml
version: '3.8'

services:
  # MySQL
  mysql:
    image: mysql:8.0
    ports:
      - "3306:3306"
    environment:
      MYSQL_ROOT_PASSWORD: password
      MYSQL_DATABASE: demo
    volumes:
      - mysql-data:/var/lib/mysql

  # Redis
  redis:
    image: redis:7.0
    ports:
      - "6379:6379"

  # RabbitMQ
  rabbitmq:
    image: rabbitmq:3.12-management
    ports:
      - "5672:5672"
      - "15672:15672"
    environment:
      RABBITMQ_DEFAULT_USER: guest
      RABBITMQ_DEFAULT_PASS: guest

  # Nacos
  nacos:
    image: nacos/nacos-server:v2.3.0
    ports:
      - "8848:8848"
    environment:
      MODE: standalone

volumes:
  mysql-data:
```

启动中间件：

```bash
docker-compose up -d

# 等待服务启动（约30秒）
sleep 30

# 验证服务
docker-compose ps
```

### 步骤2：创建服务项目

创建用户服务：

```bash
mvn archetype:generate \
    -DgroupId=com.example \
    -DartifactId=user-service \
    -DarchetypeArtifactId=maven-archetype-quickstart \
    -DinteractiveMode=false

cd user-service
```

### 步骤3：添加依赖

编辑 `pom.xml`：

```xml
<dependencies>
    <!-- Nebula Service Starter (包含所有微服务需要的模块) -->
    <dependency>
        <groupId>io.nebula</groupId>
        <artifactId>nebula-starter-service</artifactId>
        <version>${nebula.version}</version>
    </dependency>

    <!-- 数据库驱动 -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
    </dependency>
</dependencies>
```

### 步骤4：配置服务

创建 `src/main/resources/application.yml`：

```yaml
spring:
  application:
    name: user-service

  # 数据库配置
  datasource:
    url: jdbc:mysql://localhost:3306/demo
    username: root
    password: password
    driver-class-name: com.mysql.cj.jdbc.Driver

  # Redis 配置
  data:
    redis:
      host: localhost
      port: 6379

  # RabbitMQ 配置
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest

server:
  port: 8081

# Nebula 配置
nebula:
  # 数据访问
  data:
    persistence:
      enabled: true
    cache:
      enabled: true
      type: redis
  
  # 消息队列
  messaging:
    rabbitmq:
      enabled: true
  
  # RPC
  rpc:
    grpc:
      server:
        port: 9090
  
  # 服务发现
  discovery:
    nacos:
      server-addr: localhost:8848
      namespace: dev
```

### 步骤5：创建实体类

创建 `src/main/java/com/example/entity/User.java`：

```java
package com.example.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("users")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String username;
    private String email;
    private String phone;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
```

### 步骤6：创建 Mapper

创建 `src/main/java/com/example/mapper/UserMapper.java`：

```java
package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
```

### 步骤7：创建 Service

创建 `src/main/java/com/example/service/UserService.java`：

```java
package com.example.service;

import com.example.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

public interface UserService extends IService<User> {
    User getByUsername(String username);
}
```

实现类 `src/main/java/com/example/service/impl/UserServiceImpl.java`：

```java
package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.entity.User;
import com.example.mapper.UserMapper;
import com.example.service.UserService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    
    @Override
    @Cacheable(value = "user", key = "#username")
    public User getByUsername(String username) {
        QueryWrapper<User> query = new QueryWrapper<>();
        query.eq("username", username);
        return getOne(query);
    }
}
```

### 步骤8：创建 Controller

创建 `src/main/java/com/example/controller/UserController.java`：

```java
package com.example.controller;

import com.example.entity.User;
import com.example.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @PostMapping
    public User create(@RequestBody User user) {
        userService.save(user);
        return user;
    }
    
    @GetMapping("/{id}")
    public User getById(@PathVariable Long id) {
        return userService.getById(id);
    }
    
    @GetMapping("/username/{username}")
    public User getByUsername(@PathVariable String username) {
        return userService.getByUsername(username);
    }
}
```

### 步骤9：创建启动类

创建 `src/main/java/com/example/UserServiceApplication.java`：

```java
package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
```

### 步骤10：运行和测试

```bash
# 运行服务
mvn spring-boot:run

# 测试接口
# 创建用户
curl -X POST http://localhost:8081/api/users \
  -H "Content-Type: application/json" \
  -d '{"username":"john","email":"john@example.com","phone":"1234567890"}'

# 查询用户
curl http://localhost:8081/api/users/1
```

## 场景3：票务系统快速开始

> 适合：高并发抢票场景、复杂业务流程

### 业务场景

构建一个完整的影院票务系统，包括：
- 用户服务：用户注册、登录、认证
- 订单服务：创建订单、支付、取消
- 票务服务：票务查询、座位管理
- 支付服务：支付处理
- 通知服务：消息通知

### 步骤1：创建项目骨架

```bash
# 创建父项目
mkdir ticket-system
cd ticket-system

# 创建各个服务
mkdir -p user-service/src/main/java/com/ticket/user
mkdir -p order-service/src/main/java/com/ticket/order
mkdir -p ticket-service/src/main/java/com/ticket/ticket
mkdir -p payment-service/src/main/java/com/ticket/payment
mkdir -p notification-service/src/main/java/com/ticket/notification
```

### 步骤2：订单服务实现（核心服务）

#### 2.1 添加依赖

`order-service/pom.xml`：

```xml
<dependencies>
    <!-- Nebula Service Starter -->
    <dependency>
        <groupId>io.nebula</groupId>
        <artifactId>nebula-starter-service</artifactId>
        <version>${nebula.version}</version>
    </dependency>

    <!-- 数据库 -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
    </dependency>
</dependencies>
```

#### 2.2 配置文件

`order-service/src/main/resources/application.yml`：

```yaml
spring:
  application:
    name: order-service

  datasource:
    url: jdbc:mysql://localhost:3306/ticket_order
    username: root
    password: password

  data:
    redis:
      host: localhost
      port: 6379

  rabbitmq:
    host: localhost
    port: 5672

server:
  port: 8082

nebula:
  data:
    persistence:
      enabled: true
    cache:
      enabled: true
      type: redis
      redis:
        time-to-live: 900  # 15分钟
  
  messaging:
    rabbitmq:
      enabled: true
  
  rpc:
    grpc:
      server:
        port: 9092
      client:
        user-service:
          address: localhost:9091
        ticket-service:
          address: localhost:9093
  
  # 分布式锁（关键配置）
  lock:
    redis:
      enabled: true
      lock-timeout: 30000
      retry-times: 3
  
  discovery:
    nacos:
      server-addr: localhost:8848
```

#### 2.3 创建订单实体

```java
package com.ticket.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("orders")
public class Order {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String orderNo;
    private String userId;
    private String showtimeId;
    private Integer seatCount;
    private String seatInfo;  // JSON 格式
    private BigDecimal finalAmount;
    private String orderStatus;  // pending, paid, cancelled, expired
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    private LocalDateTime expiredAt;
    private LocalDateTime paidAt;
}
```

#### 2.4 创建订单服务（防超卖关键代码）

```java
package com.ticket.order.service.impl;

import com.ticket.order.entity.Order;
import com.ticket.order.mapper.OrderMapper;
import com.ticket.order.service.OrderService;
import io.nebula.lock.annotation.DistributedLock;
import io.nebula.messaging.MessageProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    
    private final OrderMapper orderMapper;
    private final MessageProducer messageProducer;
    private final TicketServiceClient ticketServiceClient;  // gRPC 客户端
    
    /**
     * 创建订单（防超卖的关键实现）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @DistributedLock(
        key = "'order:lock:' + #showtimeId",
        leaseTime = 30,
        waitTime = 3
    )
    public Order createOrder(String userId, String showtimeId, List<String> seatIds) {
        log.info("用户 {} 开始创建订单，场次：{}，座位：{}", userId, showtimeId, seatIds);
        
        // 1. 检查库存（通过 gRPC 调用票务服务）
        int availableStock = ticketServiceClient.getAvailableStock(showtimeId);
        if (availableStock < seatIds.size()) {
            throw new BusinessException("库存不足，当前可用：" + availableStock);
        }
        
        // 2. 锁定座位
        boolean locked = ticketServiceClient.lockSeats(showtimeId, seatIds);
        if (!locked) {
            throw new BusinessException("座位已被占用");
        }
        
        try {
            // 3. 创建订单
            Order order = new Order();
            order.setOrderNo("ORD" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8));
            order.setUserId(userId);
            order.setShowtimeId(showtimeId);
            order.setSeatCount(seatIds.size());
            order.setSeatInfo(JSON.toJSONString(seatIds));
            order.setFinalAmount(calculateAmount(showtimeId, seatIds.size()));
            order.setOrderStatus("pending");
            order.setExpiredAt(LocalDateTime.now().plusMinutes(15));  // 15分钟后过期
            
            orderMapper.insert(order);
            log.info("订单创建成功：{}", order.getOrderNo());
            
            // 4. 发送订单创建消息（异步处理）
            messageProducer.send("order.created", order);
            
            // 5. 发送延时消息（15分钟后检查订单是否支付）
            messageProducer.sendDelayed("order.timeout", order.getId(), 15 * 60 * 1000);
            
            return order;
            
        } catch (Exception e) {
            // 异常时释放座位
            ticketServiceClient.unlockSeats(showtimeId, seatIds);
            throw e;
        }
    }
    
    /**
     * 支付订单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payOrder(Long orderId, String paymentId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        
        if (!"pending".equals(order.getOrderStatus())) {
            throw new BusinessException("订单状态异常");
        }
        
        if (order.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("订单已过期");
        }
        
        // 更新订单状态
        order.setOrderStatus("paid");
        order.setPaidAt(LocalDateTime.now());
        orderMapper.updateById(order);
        
        // 发送支付成功消息
        messageProducer.send("order.paid", order);
        
        log.info("订单支付成功：{}", order.getOrderNo());
    }
    
    /**
     * 取消订单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        
        if ("paid".equals(order.getOrderStatus())) {
            throw new BusinessException("已支付订单不能取消");
        }
        
        // 更新订单状态
        order.setOrderStatus("cancelled");
        orderMapper.updateById(order);
        
        // 释放座位
        List<String> seatIds = JSON.parseArray(order.getSeatInfo(), String.class);
        ticketServiceClient.unlockSeats(order.getShowtimeId(), seatIds);
        
        // 发送取消消息
        messageProducer.send("order.cancelled", order);
        
        log.info("订单取消成功：{}", order.getOrderNo());
    }
    
    private BigDecimal calculateAmount(String showtimeId, int seatCount) {
        // 计算订单金额（简化实现）
        BigDecimal unitPrice = new BigDecimal("49.00");
        return unitPrice.multiply(new BigDecimal(seatCount));
    }
}
```

#### 2.5 创建消息监听器

```java
package com.ticket.order.listener;

import com.ticket.order.service.OrderService;
import io.nebula.messaging.core.annotation.MessageHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderMessageListener {
    
    private final OrderService orderService;
    
    /**
     * 处理订单超时
     */
    @MessageHandler(topic = "order.timeout")
    public void handleOrderTimeout(Long orderId) {
        log.info("处理订单超时：{}", orderId);
        
        // 检查订单状态，如果未支付则自动取消
        Order order = orderService.getById(orderId);
        if (order != null && "pending".equals(order.getOrderStatus())) {
            orderService.cancelOrder(orderId);
            log.info("订单 {} 超时自动取消", order.getOrderNo());
        }
    }
}
```

### 步骤3：运行票务系统

```bash
# 1. 启动中间件
docker-compose up -d

# 2. 创建数据库
mysql -h localhost -u root -p -e "CREATE DATABASE ticket_order"
mysql -h localhost -u root -p -e "CREATE DATABASE ticket_user"
mysql -h localhost -u root -p -e "CREATE DATABASE ticket_ticket"

# 3. 启动各个服务
cd user-service && mvn spring-boot:run &
cd ticket-service && mvn spring-boot:run &
cd order-service && mvn spring-boot:run &
cd payment-service && mvn spring-boot:run &
cd notification-service && mvn spring-boot:run &

# 4. 等待服务启动
sleep 30
```

### 步骤4：测试票务系统

```bash
# 1. 创建订单（模拟抢票）
curl -X POST http://localhost:8082/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user001",
    "showtimeId": "show001",
    "seatIds": ["A1", "A2"]
  }'

# 响应示例：
# {
#   "code": "200",
#   "message": "success",
#   "data": {
#     "id": 1,
#     "orderNo": "ORD1700000000001a2b3c4d",
#     "userId": "user001",
#     "showtimeId": "show001",
#     "seatCount": 2,
#     "finalAmount": 98.00,
#     "orderStatus": "pending",
#     "expiredAt": "2025-11-20T12:15:00"
#   }
# }

# 2. 查询订单
curl http://localhost:8082/api/orders/1

# 3. 支付订单
curl -X POST http://localhost:8082/api/orders/1/pay \
  -H "Content-Type: application/json" \
  -d '{"paymentId": "PAY001"}'

# 4. 查询用户订单列表
curl http://localhost:8082/api/orders/user/user001
```

### 步骤5：验证防超卖机制

使用并发测试工具（如 JMeter 或 ab）进行压测：

```bash
# 安装 Apache Bench
brew install httpd  # macOS

# 并发100个请求抢同一场次的票
ab -n 100 -c 10 -p order.json -T application/json \
  http://localhost:8082/api/orders

# order.json 内容：
# {
#   "userId": "user001",
#   "showtimeId": "show001",
#   "seatIds": ["A1", "A2"]
# }

# 结果：只有第一个请求成功，其他请求返回"库存不足"或"座位已被占用"
```

## 场景4：电商系统快速开始

> 适合：商品管理、订单处理、库存管理

### 核心特点

- 商品管理和库存控制
- 秒杀场景支持
- 订单流程处理
- 支付集成

### 快速开始

#### 步骤1：添加依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-starter-service</artifactId>
</dependency>
```

#### 步骤2：配置

```yaml
nebula:
  data:
    persistence:
      enabled: true
    cache:
      enabled: true
  lock:
    redis:
      enabled: true
  integration:
    payment:
      enabled: true
```

#### 步骤3：实现秒杀服务

```java
@Service
public class SecKillService {
    
    @DistributedLock(key = "'seckill:' + #productId")
    public Order seckill(String userId, String productId) {
        // 1. 检查库存
        int stock = stockService.getStock(productId);
        if (stock <= 0) {
            throw new BusinessException("商品已售罄");
        }
        
        // 2. 扣减库存
        stockService.deductStock(productId, 1);
        
        // 3. 创建订单
        Order order = createOrder(userId, productId);
        
        // 4. 发送消息
        messageProducer.send("order.created", order);
        
        return order;
    }
}
```

## 场景5：AI 应用快速开始

> 适合：智能推荐、AI 客服、内容生成

### 快速开始

#### 步骤1：添加依赖

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-starter-ai</artifactId>
</dependency>
```

#### 步骤2：配置

```yaml
nebula:
  ai:
    spring:
      enabled: true
      openai:
        api-key: ${OPENAI_API_KEY}
        model: gpt-4
```

#### 步骤3：实现 AI 服务

```java
@Service
public class AiRecommendationService {
    
    @Autowired
    private ChatClient chatClient;
    
    public String recommend(String userId) {
        String prompt = "根据用户 " + userId + " 的观影历史推荐电影";
        
        return chatClient.call(prompt);
    }
}
```

## 常见问题

### Q1: 启动失败怎么办？

**检查清单**：
1. JDK 版本是否为 21+
2. 必需的中间件是否启动
3. 数据库连接是否正确
4. 端口是否被占用

**查看日志**：
```bash
# 查看详细日志
mvn spring-boot:run -X
```

### Q2: 如何热部署？

使用 Spring Boot DevTools：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

### Q3: 如何调试？

在 IDEA 中：
1. 添加断点
2. 点击 Debug 按钮运行
3. 发送请求触发断点

### Q4: 生产环境如何部署？

```bash
# 1. 打包
mvn clean package -DskipTests

# 2. 运行
java -jar target/app.jar \
  --spring.profiles.active=prod \
  --server.port=8080
```

## 下一步

### 学习路径

1. **了解架构**: [架构设计](ARCHITECTURE.md)
2. **模块选择**: [Starter 选择指南](../STARTER_SELECTION_GUIDE.md)
3. **最佳实践**: [最佳实践](../Nebula框架使用指南.md#最佳实践)
4. **性能调优**: [监控与运维](../Nebula框架使用指南.md#监控与运维)

### 示例项目

- [完整票务系统](../../example/ticket-system/)
- [电商秒杀系统](../../example/ecommerce-system/)
- [AI 推荐系统](../../example/ai-recommendation/)

### 获取帮助

- **文档**: [完整文档](../INDEX.md)
- **社区**: [GitHub Discussions](https://github.com/nebula/nebula/discussions)
- **问题**: [GitHub Issues](https://github.com/nebula/nebula/issues)

---

**Nebula 团队**  
**最后更新**: 2025-11-20  
**文档版本**: v1.0

