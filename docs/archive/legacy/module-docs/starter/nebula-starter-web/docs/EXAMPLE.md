# Nebula Starter Web - 使用示例

> Web应用专用Starter的完整使用示例，涵盖REST API、JWT认证、缓存、数据库访问等典型场景。

## 示例概览

本文档包含以下示例：

- [示例1：简单REST API](#示例1简单rest-api)
- [示例2：带JWT认证的API](#示例2带jwt认证的api)
- [示例3：CRUD操作](#示例3crud操作)
- [示例4：分页查询](#示例4分页查询)
- [示例5：参数验证](#示例5参数验证)
- [示例6：异常处理](#示例6异常处理)
- [示例7：文件上传下载](#示例7文件上传下载)
- [示例8：缓存应用](#示例8缓存应用)
- [票务系统应用场景](#票务系统应用场景)

## 前提条件

### 环境要求

- **Java**：21+
- **Maven**：3.8+
- **Spring Boot**：3.2+
- **MySQL**：8.0+（如使用数据库）
- **Redis**：7.0+（如使用缓存）

### 依赖配置

在 `pom.xml` 中添加：

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-starter-web</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>
```

---

## 示例1：简单REST API

### 场景说明

创建一个简单的REST API，返回数据。

### 实现步骤

#### 步骤1：创建主类

```java
package com.example.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Web应用主类
 */
@SpringBootApplication
public class WebApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(WebApplication.class, args);
    }
}
```

#### 步骤2：创建Controller

```java
package com.example.web.controller;

import io.nebula.web.controller.BaseController;
import io.nebula.core.model.Result;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

/**
 * Hello Controller
 */
@RestController
@RequestMapping("/api")
@Slf4j
public class HelloController extends BaseController {
    
    /**
     * 简单问候
     */
    @GetMapping("/hello")
    public Result<String> hello() {
        return success("Hello Nebula Web!");
    }
    
    /**
     * 带参数问候
     */
    @GetMapping("/hello/{name}")
    public Result<String> helloName(@PathVariable String name) {
        return success("Hello, " + name + "!");
    }
    
    /**
     * POST请求
     */
    @PostMapping("/echo")
    public Result<Map<String, Object>> echo(@RequestBody Map<String, Object> data) {
        log.info("收到数据: {}", data);
        return success(data);
    }
}
```

#### 步骤3：配置文件

`application.yml`:

```yaml
server:
  port: 8080

spring:
  application:
    name: web-app

logging:
  level:
    root: INFO
    com.example: DEBUG
```

#### 步骤4：启动并测试

```bash
# 启动应用
mvn spring-boot:run

# 测试API
curl http://localhost:8080/api/hello
curl http://localhost:8080/api/hello/张三
curl -X POST http://localhost:8080/api/echo -H "Content-Type: application/json" -d '{"key":"value"}'
```

### 运行结果

```json
{
  "code": "0",
  "message": "success",
  "data": "Hello Nebula Web!",
  "timestamp": "2025-11-20T10:00:00"
}
```

---

## 示例2：带JWT认证的API

### 场景说明

实现用户登录和JWT认证保护的API。

### 实现步骤

#### 步骤1：用户实体

```java
package com.example.web.entity;

import lombok.Data;

/**
 * 用户实体
 */
@Data
public class User {
    private String id;
    private String username;
    private String password;
    private String nickname;
    private String email;
    private String phone;
}
```

#### 步骤2：登录请求

```java
package com.example.web.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求
 */
@Data
public class LoginRequest {
    
    @NotBlank(message = "用户名不能为空")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    private String password;
}
```

#### 步骤3：认证Service

```java
package com.example.web.service;

import io.nebula.core.exception.BusinessException;
import io.nebula.core.util.JwtUtils;
import io.nebula.core.util.CryptoUtils;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证服务
 */
@Service
@Slf4j
public class AuthService {
    
    /**
     * 用户登录
     */
    public String login(String username, String password) {
        // 1. 查询用户（示例：模拟查询）
        User user = getUserByUsername(username);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        // 2. 验证密码
        if (!CryptoUtils.matchesPassword(password, user.getPassword())) {
            throw new BusinessException("密码错误");
        }
        
        // 3. 生成JWT Token
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        
        String token = JwtUtils.generateToken(user.getId(), claims);
        
        log.info("用户登录成功: {}", username);
        return token;
    }
    
    /**
     * 验证Token
     */
    public boolean validateToken(String token) {
        try {
            return JwtUtils.validateToken(token);
        } catch (Exception e) {
            log.error("Token验证失败", e);
            return false;
        }
    }
    
    /**
     * 获取用户信息
     */
    public User getCurrentUser(String token) {
        String userId = JwtUtils.getSubject(token);
        return getUserById(userId);
    }
    
    // 模拟数据库查询
    private User getUserByUsername(String username) {
        if ("admin".equals(username)) {
            User user = new User();
            user.setId("1");
            user.setUsername("admin");
            user.setPassword(CryptoUtils.encryptPassword("123456"));
            user.setNickname("管理员");
            return user;
        }
        return null;
    }
    
    private User getUserById(String id) {
        // 模拟查询
        return getUserByUsername("admin");
    }
}
```

#### 步骤4：认证Controller

```java
package com.example.web.controller;

import io.nebula.web.controller.BaseController;
import io.nebula.core.model.Result;
import com.example.web.dto.LoginRequest;
import com.example.web.service.AuthService;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证Controller
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController extends BaseController {
    
    private final AuthService authService;
    
    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<Map<String, String>> login(@Valid @RequestBody LoginRequest request) {
        String token = authService.login(request.getUsername(), request.getPassword());
        
        Map<String, String> result = new HashMap<>();
        result.put("token", token);
        result.put("tokenType", "Bearer");
        
        return success(result);
    }
    
    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public Result<User> getCurrentUser(@RequestHeader("Authorization") String authorization) {
        String token = authorization.replace("Bearer ", "");
        User user = authService.getCurrentUser(token);
        return success(user);
    }
    
    /**
     * 登出
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        // 实际应该清除Token缓存
        return success();
    }
}
```

#### 步骤5：JWT拦截器

```java
package com.example.web.interceptor;

import io.nebula.core.util.JwtUtils;
import io.nebula.core.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;
import lombok.extern.slf4j.Slf4j;

/**
 * JWT认证拦截器
 */
@Slf4j
public class JwtInterceptor implements HandlerInterceptor {
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // OPTIONS请求直接放行
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }
        
        // 获取Token
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new BusinessException("未登录或Token已过期");
        }
        
        String token = authorization.replace("Bearer ", "");
        
        // 验证Token
        if (!JwtUtils.validateToken(token)) {
            throw new BusinessException("Token无效");
        }
        
        // 将用户信息放入请求属性
        String userId = JwtUtils.getSubject(token);
        request.setAttribute("userId", userId);
        
        return true;
    }
}
```

#### 步骤6：配置拦截器

```java
package com.example.web.config;

import com.example.web.interceptor.JwtInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new JwtInterceptor())
            .addPathPatterns("/api/**")  // 拦截所有API
            .excludePathPatterns(
                "/api/auth/login",  // 登录接口不拦截
                "/api/auth/register"  // 注册接口不拦截
            );
    }
}
```

#### 步骤7：测试API

```bash
# 登录获取Token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'

# 使用Token访问受保护的API
TOKEN="your_token_here"
curl http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer $TOKEN"
```

---

## 示例3：CRUD操作

### 场景说明

实现完整的CRUD（增删改查）操作。

### 实现代码

#### 实体类

```java
package com.example.web.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 商品实体
 */
@Data
public class Product {
    private String id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private String category;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

#### Service层

```java
package com.example.web.service;

import com.example.web.entity.Product;
import io.nebula.core.exception.BusinessException;
import io.nebula.core.util.IdGenerator;
import io.nebula.core.util.DateUtils;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 商品服务
 */
@Service
@Slf4j
public class ProductService {
    
    // 模拟数据库（实际应该使用MyBatis）
    private final Map<String, Product> products = new ConcurrentHashMap<>();
    
    /**
     * 创建商品
     */
    public Product create(Product product) {
        product.setId(IdGenerator.snowflakeIdString());
        product.setCreateTime(DateUtils.nowDateTime());
        product.setUpdateTime(DateUtils.nowDateTime());
        
        products.put(product.getId(), product);
        
        log.info("创建商品: {}", product.getName());
        return product;
    }
    
    /**
     * 更新商品
     */
    public Product update(String id, Product product) {
        Product existing = products.get(id);
        if (existing == null) {
            throw new BusinessException("商品不存在");
        }
        
        product.setId(id);
        product.setCreateTime(existing.getCreateTime());
        product.setUpdateTime(DateUtils.nowDateTime());
        
        products.put(id, product);
        
        log.info("更新商品: {}", id);
        return product;
    }
    
    /**
     * 删除商品
     */
    public void delete(String id) {
        Product product = products.remove(id);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        
        log.info("删除商品: {}", id);
    }
    
    /**
     * 查询商品
     */
    public Product getById(String id) {
        Product product = products.get(id);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        return product;
    }
    
    /**
     * 查询所有商品
     */
    public List<Product> list() {
        return new ArrayList<>(products.values());
    }
}
```

#### Controller层

```java
package com.example.web.controller;

import com.example.web.entity.Product;
import com.example.web.service.ProductService;
import io.nebula.web.controller.BaseController;
import io.nebula.core.model.Result;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 商品Controller
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController extends BaseController {
    
    private final ProductService productService;
    
    /**
     * 创建商品
     */
    @PostMapping
    public Result<Product> create(@Valid @RequestBody Product product) {
        Product created = productService.create(product);
        return success(created);
    }
    
    /**
     * 更新商品
     */
    @PutMapping("/{id}")
    public Result<Product> update(@PathVariable String id, @Valid @RequestBody Product product) {
        Product updated = productService.update(id, product);
        return success(updated);
    }
    
    /**
     * 删除商品
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        productService.delete(id);
        return success();
    }
    
    /**
     * 查询商品
     */
    @GetMapping("/{id}")
    public Result<Product> getById(@PathVariable String id) {
        Product product = productService.getById(id);
        return success(product);
    }
    
    /**
     * 查询所有商品
     */
    @GetMapping
    public Result<List<Product>> list() {
        List<Product> products = productService.list();
        return success(products);
    }
}
```

---

## 示例4：分页查询

### 实现代码

```java
package com.example.web.controller;

import io.nebula.web.controller.BaseController;
import io.nebula.core.model.Result;
import io.nebula.core.model.PageResult;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

/**
 * 分页查询Controller
 */
@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
public class MovieController extends BaseController {
    
    private final MovieService movieService;
    
    /**
     * 分页查询电影
     */
    @GetMapping
    public PageResult<Movie>> page(
        @RequestParam(defaultValue = "1") Integer pageNum,
        @RequestParam(defaultValue = "10") Integer pageSize,
        @RequestParam(required = false) String keyword) {
        
        PageResult<Movie> page = movieService.page(pageNum, pageSize, keyword);
        return success(page);
    }
}
```

**Service层**:

```java
@Service
public class MovieService {
    
    public PageResult<Movie> page(Integer pageNum, Integer pageSize, String keyword) {
        // 模拟分页查询
        List<Movie> records = queryMovies(pageNum, pageSize, keyword);
        long total = countMovies(keyword);
        
        return PageResult.of(records, total, pageNum, pageSize);
    }
}
```

---

## 示例5：参数验证

### 实现代码

```java
package com.example.web.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * 创建商品请求
 */
@Data
public class CreateProductRequest {
    
    @NotBlank(message = "商品名称不能为空")
    @Size(min = 2, max = 50, message = "商品名称长度为2-50个字符")
    private String name;
    
    @Size(max = 500, message = "商品描述不能超过500个字符")
    private String description;
    
    @NotNull(message = "商品价格不能为空")
    @DecimalMin(value = "0.01", message = "商品价格必须大于0")
    @DecimalMax(value = "999999.99", message = "商品价格不能超过999999.99")
    private BigDecimal price;
    
    @NotNull(message = "库存数量不能为空")
    @Min(value = 0, message = "库存数量不能为负数")
    private Integer stock;
    
    @NotBlank(message = "商品分类不能为空")
    private String category;
}
```

**Controller**:

```java
@PostMapping
public Result<Product> create(@Valid @RequestBody CreateProductRequest request) {
    // 验证通过后执行业务逻辑
    Product product = productService.create(request);
    return success(product);
}
```

**全局异常处理**（已在nebula-web中实现）会自动处理验证错误，返回：

```json
{
  "code": "VALIDATION_ERROR",
  "message": "参数验证失败",
  "data": null,
  "errors": [
    {
      "field": "name",
      "message": "商品名称不能为空"
    },
    {
      "field": "price",
      "message": "商品价格必须大于0"
    }
  ],
  "timestamp": "2025-11-20T10:00:00"
}
```

---

## 示例6：异常处理

### 实现代码

Nebula框架已提供全局异常处理，你只需抛出异常即可：

```java
@Service
public class OrderService {
    
    public Order getById(String id) {
        Order order = orderRepository.findById(id);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        return order;
    }
    
    public void cancelOrder(String id) {
        Order order = getById(id);
        
        if (order.getStatus() == OrderStatus.PAID) {
            throw new BusinessException("已支付订单不能取消");
        }
        
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException("订单已取消");
        }
        
        // 执行取消逻辑
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.update(order);
    }
}
```

异常会被全局处理器捕获并返回统一格式：

```json
{
  "code": "BUSINESS_ERROR",
  "message": "订单不存在",
  "data": null,
  "timestamp": "2025-11-20T10:00:00"
}
```

---

## 示例7：文件上传下载

### 实现代码

```java
package com.example.web.controller;

import io.nebula.web.controller.BaseController;
import io.nebula.core.model.Result;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletResponse;

import java.io.*;

/**
 * 文件Controller
 */
@RestController
@RequestMapping("/api/files")
public class FileController extends BaseController {
    
    /**
     * 文件上传
     */
    @PostMapping("/upload")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) throws IOException {
        // 验证文件
        if (file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }
        
        // 生成文件名
        String originalFilename = file.getOriginalFilename();
        String filename = IdGenerator.shortId() + "_" + originalFilename;
        
        // 保存文件
        String path = "/uploads/" + filename;
        File dest = new File(path);
        file.transferTo(dest);
        
        // 返回文件信息
        Map<String, String> result = new HashMap<>();
        result.put("filename", filename);
        result.put("url", "/api/files/download/" + filename);
        result.put("size", String.valueOf(file.getSize()));
        
        return success(result);
    }
    
    /**
     * 文件下载
     */
    @GetMapping("/download/{filename}")
    public void download(@PathVariable String filename, HttpServletResponse response) throws IOException {
        // 读取文件
        File file = new File("/uploads/" + filename);
        if (!file.exists()) {
            throw new BusinessException("文件不存在");
        }
        
        // 设置响应头
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=" + filename);
        
        // 输出文件
        try (InputStream in = new FileInputStream(file);
             OutputStream out = response.getOutputStream()) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
        }
    }
}
```

---

## 示例8：缓存应用

### 实现代码

```java
package com.example.web.service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Service;

/**
 * 使用缓存的服务
 */
@Service
public class MovieService {
    
    /**
     * 查询电影（使用缓存）
     */
    @Cacheable(value = "movies", key = "#id")
    public Movie getById(String id) {
        // 从数据库查询
        return movieRepository.findById(id);
    }
    
    /**
     * 更新电影（更新缓存）
     */
    @CachePut(value = "movies", key = "#result.id")
    public Movie update(String id, Movie movie) {
        // 更新数据库
        return movieRepository.update(id, movie);
    }
    
    /**
     * 删除电影（清除缓存）
     */
    @CacheEvict(value = "movies", key = "#id")
    public void delete(String id) {
        // 从数据库删除
        movieRepository.delete(id);
    }
    
    /**
     * 清空所有缓存
     */
    @CacheEvict(value = "movies", allEntries = true)
    public void clearCache() {
        // 清空缓存
    }
}
```

---

## 票务系统应用场景

### 场景1：电影查询API

```java
@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
public class MovieController extends BaseController {
    
    private final MovieService movieService;
    
    @GetMapping
    public PageResult<Movie>> page(
        @RequestParam(defaultValue = "1") Integer pageNum,
        @RequestParam(defaultValue = "10") Integer pageSize,
        @RequestParam(required = false) String keyword) {
        
        PageResult<Movie> page = movieService.page(pageNum, pageSize, keyword);
        return success(page);
    }
    
    @GetMapping("/{id}")
    public Result<Movie> getById(@PathVariable String id) {
        Movie movie = movieService.getById(id);
        return success(movie);
    }
    
    @GetMapping("/hot")
    public Result<List<Movie>> hot() {
        List<Movie> movies = movieService.getHotMovies();
        return success(movies);
    }
}
```

### 场景2：购票流程API

```java
@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController extends BaseController {
    
    private final TicketService ticketService;
    
    /**
     * 查询场次
     */
    @GetMapping("/showtimes")
    public Result<List<Showtime>> showtimes(
        @RequestParam String movieId,
        @RequestParam String date) {
        
        List<Showtime> showtimes = ticketService.getShowtimes(movieId, LocalDate.parse(date));
        return success(showtimes);
    }
    
    /**
     * 查询座位
     */
    @GetMapping("/seats/{showtimeId}")
    public Result<List<Seat>> seats(@PathVariable String showtimeId) {
        List<Seat> seats = ticketService.getSeats(showtimeId);
        return success(seats);
    }
    
    /**
     * 锁定座位
     */
    @PostMapping("/lock-seats")
    public Result<String> lockSeats(@Valid @RequestBody LockSeatsRequest request) {
        String lockToken = ticketService.lockSeats(request.getShowtimeId(), request.getSeatIds());
        return success(lockToken);
    }
    
    /**
     * 创建订单
     */
    @PostMapping("/orders")
    public Result<Order> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        Order order = ticketService.createOrder(request);
        return success(order);
    }
    
    /**
     * 支付订单
     */
    @PostMapping("/orders/{orderId}/pay")
    public Result<PaymentResult> pay(@PathVariable String orderId) {
        PaymentResult result = ticketService.pay(orderId);
        return success(result);
    }
}
```

### 场景3：用户管理API

```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController extends BaseController {
    
    private final UserService userService;
    
    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<User> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request);
        return success(user);
    }
    
    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public Result<User> me() {
        String userId = getCurrentUserId();
        User user = userService.getById(userId);
        return success(user);
    }
    
    /**
     * 更新用户信息
     */
    @PutMapping("/me")
    public Result<User> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        String userId = getCurrentUserId();
        User user = userService.updateProfile(userId, request);
        return success(user);
    }
    
    /**
     * 查询订单列表
     */
    @GetMapping("/me/orders")
    public PageResult<Order>> orders(
        @RequestParam(defaultValue = "1") Integer pageNum,
        @RequestParam(defaultValue = "10") Integer pageSize) {
        
        String userId = getCurrentUserId();
        PageResult<Order> page = userService.getOrders(userId, pageNum, pageSize);
        return success(page);
    }
}
```

---

## 最佳实践

### 实践1：继承BaseController

```java
@RestController
@RequestMapping("/api/products")
public class ProductController extends BaseController {
    // 可以直接使用success()、error()等方法
}
```

### 实践2：使用统一异常

```java
if (product == null) {
    throw new BusinessException("商品不存在");
}
```

### 实践3：参数验证

```java
@PostMapping
public Result<Product> create(@Valid @RequestBody CreateProductRequest request) {
    // 自动验证参数
}
```

### 实践4：使用缓存

```java
@Cacheable(value = "products", key = "#id")
public Product getById(String id) {
    return productRepository.findById(id);
}
```

---

## 完整示例项目

参考示例项目：`examples/starter-web-example`

---

## 相关文档

- [README.md](./README.md) - 模块介绍
- [CONFIG.md](./CONFIG.md) - 配置参考
- [TESTING.md](./TESTING.md) - 测试指南
- [ROADMAP.md](./ROADMAP.md) - 未来规划
- [Nebula Web文档](../../application/nebula-web/README.md) - Web模块详细文档

---

> 如有问题或建议，欢迎提Issue。

