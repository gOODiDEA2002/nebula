# nebula-web 模块单元测试清单

## 模块说明

基于Spring Boot的Web框架模块，提供认证、限流、缓存、性能监控、健康检查、数据脱敏和请求日志等企业级功能。

## 核心功能

1. 认证系统（JWT认证）
2. 限流（Rate Limiting）
3. 响应缓存（Response Caching）
4. 性能监控（Performance Monitoring）
5. 健康检查（Health Checks）
6. 数据脱敏（Data Masking）
7. 请求日志（Request Logging）

## 测试类清单

### 1. AuthServiceTest

**测试类路径**: `io.nebula.web.auth.AuthService`  
**测试目的**: 验证JWT认证功能

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testGenerateToken() | generateToken(AuthUser) | 测试生成JWT Token | 无 |
| testParseToken() | parseToken(String) | 测试解析JWT Token | 无 |
| testValidateToken() | validateToken(String) | 测试验证Token有效性 | 无 |
| testTokenExpired() | validateToken() | 测试过期Token | 无 |
| testInvalidToken() | parseToken() | 测试无效Token | 无 |

**测试数据准备**:
- 准备AuthUser测试对象
- 准备JWT配置（secret, expiration）

**验证要点**:
- Token正确生成
- Token正确解析
- 过期Token被拒绝
- 无效Token被拒绝

**Mock示例**:
```java
@Test
void testGenerateToken() {
    AuthUser user = AuthUser.builder()
        .userId("user-123")
        .username("testuser")
        .roles(List.of("USER"))
        .build();
    
    String token = authService.generateToken(user);
    
    assertThat(token).isNotEmpty();
    
    // 解析验证
    AuthUser parsed = authService.parseToken(token);
    assertThat(parsed.getUserId()).isEqualTo("user-123");
    assertThat(parsed.getUsername()).isEqualTo("testuser");
}
```

---

### 2. AuthInterceptorTest

**测试类路径**: `io.nebula.web.auth.AuthInterceptor`  
**测试目的**: 验证认证拦截器功能

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testAuthSuccess() | preHandle() | 测试认证成功 | HttpServletRequest, AuthService |
| testAuthFailure() | preHandle() | 测试认证失败 | HttpServletRequest, AuthService |
| testIgnorePath() | preHandle() | 测试忽略路径 | HttpServletRequest |
| testMissingToken() | preHandle() | 测试缺少Token | HttpServletRequest |

**测试数据准备**:
- Mock HttpServletRequest
- Mock AuthService

**验证要点**:
- 有效Token通过认证
- 无效Token被拒绝
- 忽略路径不需要认证
- 缺少Token返回401

---

### 3. RateLimiterTest

**测试类路径**: `io.nebula.web.ratelimit.RateLimiter`  
**测试目的**: 验证限流功能

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testAllowRequest() | tryAcquire(String) | 测试允许请求 | 无 |
| testBlockRequest() | tryAcquire() | 测试阻止超限请求 | 无 |
| testSlidingWindow() | - | 测试滑动窗口算法 | 无 |
| testResetWindow() | - | 测试窗口重置 | 无 |

**测试数据准备**:
- 配置限流参数（每秒请求数、时间窗口）

**验证要点**:
- 限流阈值正确
- 滑动窗口算法正确
- 超限请求被阻止
- 窗口重置后允许请求

**Mock示例**:
```java
@Test
void testRateLimiter() {
    RateLimiter limiter = new SlidingWindowRateLimiter(10, 1); // 每秒10个请求
    
    String key = "user:123";
    
    // 前10个请求应该通过
    for (int i = 0; i < 10; i++) {
        assertThat(limiter.tryAcquire(key)).isTrue();
    }
    
    // 第11个请求应该被拒绝
    assertThat(limiter.tryAcquire(key)).isFalse();
    
    // 等待1秒后应该可以再次请求
    Thread.sleep(1000);
    assertThat(limiter.tryAcquire(key)).isTrue();
}
```

---

### 4. ResponseCacheTest

**测试类路径**: `io.nebula.web.cache.ResponseCache`  
**测试目的**: 验证响应缓存功能

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testCacheResponse() | cacheResponse(String, CachedResponse) | 测试缓存响应 | 无 |
| testGetCachedResponse() | getCachedResponse(String) | 测试获取缓存 | 无 |
| testCacheExpiration() | - | 测试缓存过期 | 无 |
| testEvictCache() | evict(String) | 测试清除缓存 | 无 |

**测试数据准备**:
- 准备CachedResponse对象
- 配置TTL

**验证要点**:
- 响应正确缓存
- 缓存正确获取
- TTL过期后缓存失效
- 清除操作生效

---

### 5. PerformanceMonitorTest

**测试类路径**: `io.nebula.web.performance.PerformanceMonitor`  
**测试目的**: 验证性能监控功能

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testRecordRequest() | recordRequest() | 测试记录请求 | 无 |
| testCalculateMetrics() | getMetrics() | 测试计算性能指标 | 无 |
| testSlowRequestDetection() | - | 测试慢请求检测 | 无 |
| testResetMetrics() | reset() | 测试重置指标 | 无 |

**测试数据准备**:
- 模拟多个请求
- 配置慢请求阈值

**验证要点**:
- 请求正确记录
- 平均响应时间正确计算
- 慢请求正确检测
- 成功率正确计算

**Mock示例**:
```java
@Test
void testPerformanceMonitor() {
    PerformanceMonitor monitor = new PerformanceMonitor();
    
    // 记录请求
    monitor.recordRequest(200, 100);
    monitor.recordRequest(200, 150);
    monitor.recordRequest(500, 200);
    
    PerformanceMetrics metrics = monitor.getMetrics();
    
    assertThat(metrics.getTotalRequests()).isEqualTo(3);
    assertThat(metrics.getSuccessfulRequests()).isEqualTo(2);
    assertThat(metrics.getFailedRequests()).isEqualTo(1);
    assertThat(metrics.getAverageResponseTime()).isCloseTo(150.0, within(1.0));
}
```

---

### 6. HealthCheckerTest

**测试类路径**: `io.nebula.web.health.HealthChecker`  
**测试目的**: 验证健康检查功能

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testHealthCheckUp() | check() | 测试健康状态 | - |
| testHealthCheckDown() | check() | 测试不健康状态 | - |
| testMemoryHealthCheck() | - | 测试内存健康检查 | - |
| testDiskHealthCheck() | - | 测试磁盘健康检查 | - |

**测试数据准备**:
- 准备HealthChecker实现类

**验证要点**:
- 健康检查正确执行
- 健康状态正确返回
- 详细信息正确提供

**Mock示例**:
```java
@Test
void testMemoryHealthChecker() {
    MemoryHealthChecker checker = new MemoryHealthChecker();
    
    HealthCheckResult result = checker.check();
    
    assertThat(result.getStatus()).isIn(HealthStatus.UP, HealthStatus.DOWN);
    assertThat(result.getDetails()).containsKey("usedMemory");
    assertThat(result.getDetails()).containsKey("maxMemory");
}
```

---

### 7. DataMaskingTest

**测试类路径**: `io.nebula.web.masking.DataMasker`  
**测试目的**: 验证数据脱敏功能

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testMaskPhone() | mask(String, MaskType) | 测试手机号脱敏 | 无 |
| testMaskEmail() | mask() | 测试邮箱脱敏 | 无 |
| testMaskIdCard() | mask() | 测试身份证脱敏 | 无 |
| testMaskPassword() | mask() | 测试密码脱敏 | 无 |

**测试数据准备**:
- 准备各类敏感数据

**验证要点**:
- 手机号正确脱敏（138****8888）
- 邮箱正确脱敏（test***@example.com）
- 身份证正确脱敏（前6后4）
- 密码完全脱敏（******）

**Mock示例**:
```java
@Test
void testDataMasking() {
    DataMasker masker = new DataMasker();
    
    String phone = "13812345678";
    String maskedPhone = masker.mask(phone, MaskType.PHONE);
    assertThat(maskedPhone).isEqualTo("138****5678");
    
    String email = "test@example.com";
    String maskedEmail = masker.mask(email, MaskType.EMAIL);
    assertThat(maskedEmail).matches(".*\\*\\*\\*@.*");
    
    String idCard = "110101199001011234";
    String maskedIdCard = masker.mask(idCard, MaskType.ID_CARD);
    assertThat(maskedIdCard).startsWith("110101");
    assertThat(maskedIdCard).endsWith("1234");
}
```

---

### 8. RequestLoggingInterceptorTest

**测试类路径**: `io.nebula.web.logging.RequestLoggingInterceptor`  
**测试目的**: 验证请求日志功能

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testLogRequest() | preHandle() | 测试记录请求 | HttpServletRequest |
| testLogResponse() | afterCompletion() | 测试记录响应 | HttpServletResponse |
| testLogWithMasking() | - | 测试敏感信息脱敏 | - |
| testIgnorePath() | - | 测试忽略路径 | HttpServletRequest |

**测试数据准备**:
- Mock HttpServletRequest和HttpServletResponse
- 配置忽略路径

**验证要点**:
- 请求信息正确记录
- 响应信息正确记录
- 敏感信息正确脱敏
- 忽略路径不记录日志

---

## Mock策略

### 需要Mock的对象

| Mock对象 | 使用场景 | Mock行为 |
|---------|-----------|---------|
| HttpServletRequest | 拦截器测试 | Mock getHeader(), getRequestURI() |
| HttpServletResponse | 拦截器测试 | Mock getStatus() |
| AuthService | 认证测试 | Mock validateToken() |

### 不需要Mock的功能
- JWT生成和解析（使用真实实现）
- 限流算法（使用真实实现）
- 数据脱敏（使用真实实现）

---

## 测试依赖

```xml
<dependencies>
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-core</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.assertj</groupId>
        <artifactId>assertj-core</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## 测试执行

```bash
mvn test -pl nebula/application/nebula-web
```

---

## 验收标准

- 所有测试方法通过
- 核心功能测试覆盖率 >= 90%
- 认证和限流测试通过
- 性能监控和健康检查测试通过
- 数据脱敏和请求日志测试通过

