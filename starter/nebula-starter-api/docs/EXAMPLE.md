# Nebula Starter API - 使用示例

> API契约模块专用Starter的完整使用示例，展示如何定义RPC接口、DTO、实体等。

## 示例概览

本文档包含以下示例：

- [示例1：基础RPC接口定义](#示例1基础rpc接口定义)
- [示例2：DTO设计](#示例2dto设计)
- [示例3：实体类定义](#示例3实体类定义)
- [示例4：参数验证](#示例4参数验证)
- [示例5：接口版本管理](#示例5接口版本管理)
- [示例6：复杂数据结构](#示例6复杂数据结构)
- [票务系统API契约](#票务系统api契约)

## 前提条件

### 环境要求

- **Java**：21+
- **Maven**：3.8+

### 依赖配置

```xml
<dependency>
    <groupId>io.nebula</groupId>
    <artifactId>nebula-starter-api</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>
```

---

## 示例1：基础RPC接口定义

### 场景说明

定义一个简单的用户服务RPC接口。

### 项目结构

```
user-api/
├── pom.xml
└── src/
    └── main/
        └── java/
            └── com/example/user/api/
                ├── rpc/
                │   └── UserRpcService.java
                ├── dto/
                │   ├── CreateUserDto.java
                │   └── GetUserDto.java
                └── entity/
                    └── User.java
```

### 实现步骤

#### 步骤1：POM配置

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>user-api</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <name>User Service API</name>
    <description>User Service RPC API Contract</description>

    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <nebula.version>2.0.1-SNAPSHOT</nebula.version>
    </properties>

    <dependencies>
        <!-- Nebula Starter API -->
        <dependency>
            <groupId>io.nebula</groupId>
            <artifactId>nebula-starter-api</artifactId>
            <version>${nebula.version}</version>
        </dependency>
    </dependencies>
</project>
```

#### 步骤2：定义RPC接口

```java
package com.example.user.api.rpc;

import com.example.user.api.dto.CreateUserDto;
import com.example.user.api.dto.GetUserDto;
import com.example.user.api.dto.UpdateUserDto;
import io.nebula.rpc.annotation.RpcService;
import org.springframework.web.bind.annotation.*;

/**
 * 用户服务RPC接口
 */
@RpcService(name = "user-service", path = "/api/v1/user")
public interface UserRpcService {
    
    /**
     * 创建用户
     */
    @PostMapping
    CreateUserDto.Response createUser(@RequestBody CreateUserDto.Request request);
    
    /**
     * 根据ID查询用户
     */
    @GetMapping("/{id}")
    GetUserDto.Response getUserById(@PathVariable("id") String id);
    
    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    UpdateUserDto.Response updateUser(
        @PathVariable("id") String id,
        @RequestBody UpdateUserDto.Request request
    );
    
    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    void deleteUser(@PathVariable("id") String id);
    
    /**
     * 列表查询
     */
    @GetMapping
    GetUserDto.ListResponse listUsers(
        @RequestParam(defaultValue = "1") Integer pageNum,
        @RequestParam(defaultValue = "10") Integer pageSize
    );
}
```

#### 步骤3：定义DTO

```java
package com.example.user.api.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

/**
 * 创建用户DTO
 */
public class CreateUserDto {
    
    @Data
    public static class Request {
        
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 20, message = "用户名长度为3-20个字符")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
        private String username;
        
        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 20, message = "密码长度为6-20个字符")
        private String password;
        
        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        private String email;
        
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        private String phone;
        
        private String nickname;
    }
    
    @Data
    public static class Response {
        
        @NotNull
        private String userId;
        
        private String username;
        private String email;
        private String message;
    }
}
```

#### 步骤4：定义实体

```java
package com.example.user.api.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    private String id;
    private String username;
    private String email;
    private String phone;
    private String nickname;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

---

## 示例2：DTO设计

### 场景说明

设计规范的DTO，包括Request/Response分离。

### 实现代码

```java
package com.example.user.api.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 获取用户DTO
 */
public class GetUserDto {
    
    /**
     * 单个用户响应
     */
    @Data
    public static class Response {
        
        private String id;
        private String username;
        private String email;
        private String phone;
        private String nickname;
        private Integer status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
    
    /**
     * 用户列表响应
     */
    @Data
    public static class ListResponse {
        
        private List<Response> users;
        private Long total;
        private Integer pageNum;
        private Integer pageSize;
    }
}

/**
 * 更新用户DTO
 */
public class UpdateUserDto {
    
    @Data
    public static class Request {
        
        @Size(min = 3, max = 20, message = "用户名长度为3-20个字符")
        private String username;
        
        @Email(message = "邮箱格式不正确")
        private String email;
        
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        private String phone;
        
        private String nickname;
    }
    
    @Data
    public static class Response {
        
        private String userId;
        private String message;
        private LocalDateTime updatedAt;
    }
}
```

---

## 示例3：实体类定义

### 场景说明

定义可复用的实体类。

### 实现代码

```java
package com.example.user.api.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 基础实体
 */
@Data
public abstract class BaseEntity implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String id;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}

/**
 * 用户实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class User extends BaseEntity {
    
    private String username;
    private String email;
    private String phone;
    private String nickname;
    private String avatar;
    private Integer status;
    private String remark;
}

/**
 * 用户角色关联
 */
@Data
public class UserRole implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String userId;
    private String roleId;
    private LocalDateTime assignedAt;
}
```

---

## 示例4：参数验证

### 场景说明

使用Jakarta Validation进行参数验证。

### 实现代码

```java
package com.example.user.api.dto;

import lombok.Data;
import jakarta.validation.constraints.*;
import jakarta.validation.GroupSequence;

/**
 * 用户注册DTO
 */
public class RegisterDto {
    
    /**
     * 验证组 - 基础验证
     */
    public interface BasicValidation {}
    
    /**
     * 验证组 - 完整验证
     */
    public interface CompleteValidation {}
    
    /**
     * 验证顺序
     */
    @GroupSequence({BasicValidation.class, CompleteValidation.class})
    public interface ValidationOrder {}
    
    @Data
    public static class Request {
        
        @NotBlank(message = "用户名不能为空", groups = BasicValidation.class)
        @Size(min = 3, max = 20, message = "用户名长度为3-20个字符", groups = CompleteValidation.class)
        @Pattern(
            regexp = "^[a-zA-Z0-9_]+$",
            message = "用户名只能包含字母、数字和下划线",
            groups = CompleteValidation.class
        )
        private String username;
        
        @NotBlank(message = "密码不能为空", groups = BasicValidation.class)
        @Size(min = 6, max = 20, message = "密码长度为6-20个字符", groups = CompleteValidation.class)
        @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*#?&]+$",
            message = "密码必须包含字母和数字",
            groups = CompleteValidation.class
        )
        private String password;
        
        @NotBlank(message = "确认密码不能为空", groups = BasicValidation.class)
        private String confirmPassword;
        
        @NotBlank(message = "邮箱不能为空", groups = BasicValidation.class)
        @Email(message = "邮箱格式不正确", groups = CompleteValidation.class)
        private String email;
        
        @NotBlank(message = "手机号不能为空", groups = BasicValidation.class)
        @Pattern(
            regexp = "^1[3-9]\\d{9}$",
            message = "手机号格式不正确",
            groups = CompleteValidation.class
        )
        private String phone;
        
        @NotBlank(message = "验证码不能为空", groups = BasicValidation.class)
        @Size(min = 4, max = 6, message = "验证码长度为4-6位", groups = CompleteValidation.class)
        private String verifyCode;
    }
    
    @Data
    public static class Response {
        private String userId;
        private String username;
        private String token;
        private String message;
    }
}
```

---

## 示例5：接口版本管理

### 场景说明

管理API版本，保持向后兼容。

### 实现代码

```java
package com.example.user.api.rpc;

/**
 * 用户服务RPC接口 - V1版本
 */
@RpcService(name = "user-service", path = "/api/v1/user")
public interface UserRpcServiceV1 {
    
    @PostMapping
    CreateUserDto.Response createUser(@RequestBody CreateUserDto.Request request);
    
    @GetMapping("/{id}")
    GetUserDto.Response getUserById(@PathVariable("id") String id);
}

/**
 * 用户服务RPC接口 - V2版本
 * 新增功能，保持V1接口不变
 */
@RpcService(name = "user-service", path = "/api/v2/user")
public interface UserRpcServiceV2 extends UserRpcServiceV1 {
    
    /**
     * V2新增：批量查询用户
     */
    @PostMapping("/batch")
    List<GetUserDto.Response> batchGetUsers(@RequestBody List<String> ids);
    
    /**
     * V2新增：搜索用户
     */
    @GetMapping("/search")
    GetUserDto.ListResponse searchUsers(
        @RequestParam String keyword,
        @RequestParam(defaultValue = "1") Integer pageNum,
        @RequestParam(defaultValue = "10") Integer pageSize
    );
}
```

---

## 示例6：复杂数据结构

### 场景说明

定义包含嵌套对象、集合等复杂数据结构。

### 实现代码

```java
package com.example.order.api.dto;

import lombok.Data;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 创建订单DTO
 */
public class CreateOrderDto {
    
    @Data
    public static class Request {
        
        @NotBlank(message = "用户ID不能为空")
        private String userId;
        
        @NotNull(message = "订单项不能为空")
        @Size(min = 1, message = "至少需要一个订单项")
        @Valid
        private List<OrderItem> items;
        
        @NotNull(message = "配送地址不能为空")
        @Valid
        private DeliveryAddress address;
        
        private String couponCode;
        private String remark;
    }
    
    /**
     * 订单项
     */
    @Data
    public static class OrderItem {
        
        @NotBlank(message = "产品ID不能为空")
        private String productId;
        
        @NotNull(message = "数量不能为空")
        @Min(value = 1, message = "数量至少为1")
        private Integer quantity;
        
        @NotNull(message = "价格不能为空")
        @DecimalMin(value = "0.01", message = "价格必须大于0")
        private BigDecimal price;
    }
    
    /**
     * 配送地址
     */
    @Data
    public static class DeliveryAddress {
        
        @NotBlank(message = "收货人不能为空")
        private String receiverName;
        
        @NotBlank(message = "联系电话不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        private String receiverPhone;
        
        @NotBlank(message = "省份不能为空")
        private String province;
        
        @NotBlank(message = "城市不能为空")
        private String city;
        
        @NotBlank(message = "区县不能为空")
        private String district;
        
        @NotBlank(message = "详细地址不能为空")
        private String detailAddress;
        
        private String postcode;
    }
    
    @Data
    public static class Response {
        
        private String orderId;
        private String orderNo;
        private BigDecimal totalAmount;
        private String status;
        private LocalDateTime createdAt;
        private String message;
    }
}
```

---

## 票务系统API契约

### 项目结构

```
ticket-api/
├── pom.xml
└── src/
    └── main/
        └── java/
            └── com/ticketsystem/api/
                ├── rpc/
                │   ├── UserRpcService.java
                │   ├── MovieRpcService.java
                │   ├── ShowtimeRpcService.java
                │   └── OrderRpcService.java
                ├── dto/
                │   ├── user/
                │   ├── movie/
                │   ├── showtime/
                │   └── order/
                ├── entity/
                │   ├── User.java
                │   ├── Movie.java
                │   ├── Cinema.java
                │   ├── Showtime.java
                │   └── Order.java
                └── enums/
                    ├── OrderStatus.java
                    └── SeatStatus.java
```

### 电影服务API

```java
package com.ticketsystem.api.rpc;

import com.ticketsystem.api.dto.movie.*;
import io.nebula.rpc.annotation.RpcService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 电影服务RPC接口
 */
@RpcService(name = "movie-service", path = "/api/v1/movie")
public interface MovieRpcService {
    
    /**
     * 查询电影详情
     */
    @GetMapping("/{id}")
    GetMovieDto.Response getMovieById(@PathVariable("id") String id);
    
    /**
     * 查询正在上映的电影
     */
    @GetMapping("/showing")
    GetMovieDto.ListResponse getShowingMovies(
        @RequestParam(defaultValue = "1") Integer pageNum,
        @RequestParam(defaultValue = "10") Integer pageSize
    );
    
    /**
     * 查询即将上映的电影
     */
    @GetMapping("/coming-soon")
    GetMovieDto.ListResponse getComingSoonMovies(
        @RequestParam(defaultValue = "1") Integer pageNum,
        @RequestParam(defaultValue = "10") Integer pageSize
    );
    
    /**
     * 搜索电影
     */
    @GetMapping("/search")
    GetMovieDto.ListResponse searchMovies(
        @RequestParam String keyword,
        @RequestParam(defaultValue = "1") Integer pageNum,
        @RequestParam(defaultValue = "10") Integer pageSize
    );
}
```

### 订单服务API

```java
package com.ticketsystem.api.rpc;

import com.ticketsystem.api.dto.order.*;
import io.nebula.rpc.annotation.RpcService;
import org.springframework.web.bind.annotation.*;

/**
 * 订单服务RPC接口
 */
@RpcService(name = "order-service", path = "/api/v1/order")
public interface OrderRpcService {
    
    /**
     * 创建订单
     */
    @PostMapping
    CreateOrderDto.Response createOrder(@RequestBody CreateOrderDto.Request request);
    
    /**
     * 查询订单
     */
    @GetMapping("/{id}")
    GetOrderDto.Response getOrderById(@PathVariable("id") String id);
    
    /**
     * 支付订单
     */
    @PostMapping("/{id}/pay")
    PayOrderDto.Response payOrder(
        @PathVariable("id") String id,
        @RequestBody PayOrderDto.Request request
    );
    
    /**
     * 取消订单
     */
    @PostMapping("/{id}/cancel")
    CancelOrderDto.Response cancelOrder(@PathVariable("id") String id);
    
    /**
     * 查询用户订单列表
     */
    @GetMapping("/user/{userId}")
    GetOrderDto.ListResponse getUserOrders(
        @PathVariable("userId") String userId,
        @RequestParam(defaultValue = "1") Integer pageNum,
        @RequestParam(defaultValue = "10") Integer pageSize
    );
}
```

### 订单DTO

```java
package com.ticketsystem.api.dto.order;

import lombok.Data;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 创建订单DTO
 */
public class CreateOrderDto {
    
    @Data
    public static class Request {
        
        @NotBlank(message = "用户ID不能为空")
        private String userId;
        
        @NotBlank(message = "场次ID不能为空")
        private String showtimeId;
        
        @NotNull(message = "座位不能为空")
        @Size(min = 1, max = 5, message = "选座数量为1-5个")
        private List<String> seatIds;
        
        private String couponCode;
        private String lockToken;  // 座位锁定令牌
    }
    
    @Data
    public static class Response {
        
        private String orderId;
        private String orderNo;
        private String userId;
        private String showtimeId;
        private String movieName;
        private String cinemaName;
        private LocalDateTime showtime;
        private List<String> seatNos;
        private BigDecimal totalAmount;
        private String status;
        private LocalDateTime expireTime;
        private LocalDateTime createdAt;
    }
}

/**
 * 支付订单DTO
 */
public class PayOrderDto {
    
    @Data
    public static class Request {
        
        @NotBlank(message = "支付方式不能为空")
        private String paymentMethod;  // wechat, alipay
        
        private String paymentAccount;
    }
    
    @Data
    public static class Response {
        
        private String orderId;
        private String status;
        private String paymentNo;
        private LocalDateTime paidAt;
        private String message;
    }
}
```

---

## 最佳实践

### 实践1：使用内部类组织DTO

```java
public class UserDto {
    
    @Data
    public static class CreateRequest {
        // 创建用户请求
    }
    
    @Data
    public static class CreateResponse {
        // 创建用户响应
    }
    
    @Data
    public static class UpdateRequest {
        // 更新用户请求
    }
}
```

### 实践2：统一使用@NotNull/@NotBlank

```java
@NotNull(message = "ID不能为空")  // 用于对象
@NotBlank(message = "名称不能为空")  // 用于String
@Size(min = 1, message = "列表不能为空")  // 用于集合
```

### 实践3：使用@ApiModel和@ApiModelProperty

```java
@Data
@ApiModel(description = "用户信息")
public class User {
    
    @ApiModelProperty(value = "用户ID", example = "123")
    private String id;
    
    @ApiModelProperty(value = "用户名", example = "john")
    private String username;
}
```

### 实践4：版本控制

- 重大变更创建新版本接口
- 保持旧版本向后兼容
- 使用@Deprecated标记过时接口

---

## 相关文档

- [README.md](./README.md) - 模块介绍
- [CONFIG.md](./CONFIG.md) - 配置参考
- [TESTING.md](./TESTING.md) - 测试指南
- [ROADMAP.md](./ROADMAP.md) - 未来规划

---

> 如有问题或建议，欢迎提Issue。

