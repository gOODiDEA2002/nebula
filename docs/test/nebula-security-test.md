# nebula-security 模块单元测试清单

## 模块说明

提供JWT认证和RBAC授权功能，包括基于Token的无状态认证、基于角色的权限控制、安全注解和安全上下文。

## 核心功能

1. JWT认证（Token生成、验证、刷新）
2. RBAC授权（角色和权限检查）
3. 安全注解（@RequiresAuthentication、@RequiresPermission、@RequiresRole）
4. 安全上下文（SecurityContext，ThreadLocal存储）

## 测试类清单

### 1. JwtAuthenticationTokenTest

**测试类路径**: `io.nebula.security.auth.JwtAuthenticationToken`  
**测试目的**: 验证JWT认证Token的创建和属性访问

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testTokenCreation() | 构造函数 | 测试认证Token创建，验证token、principal和authorities正确设置 | UserPrincipal |
| testGetPrincipal() | getPrincipal() | 测试获取用户主体 | 无 |
| testGetAuthorities() | getAuthorities() | 测试获取权限列表 | GrantedAuthority |
| testIsAuthenticated() | isAuthenticated() | 测试认证状态，应返回true | 无 |
| testGetToken() | getToken() | 测试获取原始Token字符串 | 无 |

**测试数据准备**:
- 创建测试用UserPrincipal（userId, username, authorities）
- 创建测试用GrantedAuthority列表
- 准备测试Token字符串

**验证要点**:
- Token正确保存
- Principal信息完整
- Authorities不为空
- isAuthenticated返回true

---

### 2. UserPrincipalTest

**测试类路径**: `io.nebula.security.auth.UserPrincipal`  
**测试目的**: 验证用户主体信息的正确性

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testPrincipalCreation() | 构造函数 | 测试用户主体创建 | 无 |
| testGetUserId() | getUserId() | 测试获取用户ID | 无 |
| testGetUsername() | getUsername() | 测试获取用户名 | 无 |
| testGetAuthorities() | getAuthorities() | 测试获取权限列表 | 无 |

**测试数据准备**:
- userId: "user123"
- username: "testuser"
- authorities: ["ROLE_USER", "user:read"]

**验证要点**:
- 用户信息正确存储
- 权限列表不为null
- 权限列表元素正确

---

### 3. SecurityContextTest

**测试类路径**: `io.nebula.security.context.SecurityContext`  
**测试目的**: 验证安全上下文的ThreadLocal存储和访问

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testSetAuthentication() | setAuthentication(Authentication) | 测试设置认证信息 | Authentication |
| testGetAuthentication() | getAuthentication() | 测试获取认证信息 | 无 |
| testGetCurrentUserId() | getCurrentUserId() | 测试获取当前用户ID | Authentication |
| testGetCurrentUsername() | getCurrentUsername() | 测试获取当前用户名 | Authentication |
| testHasRole() | hasRole(String) | 测试判断是否有指定角色 | Authentication |
| testHasPermission() | hasPermission(String) | 测试判断是否有指定权限 | Authentication |
| testClear() | clear() | 测试清除上下文 | 无 |
| testThreadLocalIsolation() | - | 测试ThreadLocal隔离性，不同线程互不影响 | 无 |

**测试数据准备**:
- Mock Authentication对象
- Mock UserPrincipal对象
- 准备角色和权限列表

**验证要点**:
- ThreadLocal正确存储
- 不同线程隔离
- 清除操作生效
- 权限判断准确

---

### 4. AuthServiceTest

**测试类路径**: `io.nebula.security.service.AuthService`（如果有实现类）  
**测试目的**: 验证认证服务的登录、验证等功能

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testLogin() | login(LoginRequest) | 测试用户登录，生成Token | UserRepository, JwtUtils |
| testAuthenticateToken() | authenticateToken(String) | 测试Token认证，设置安全上下文 | JwtUtils |
| testLoadUserAuthorities() | loadUserAuthorities(Long) | 测试加载用户权限 | UserRepository, RoleRepository |
| testInvalidToken() | authenticateToken(String) | 测试无效Token抛出异常 | JwtUtils |

**测试数据准备**:
- Mock UserRepository
- Mock JwtUtils
- 准备测试用户信息

**验证要点**:
- 登录成功返回Token
- Token认证成功设置上下文
- 无效Token抛出异常
- 权限正确加载

---

## Mock策略

### 需要Mock的对象

| Mock对象 | 使用场景 | Mock行为 |
|---------|---------|---------|
| Authentication | SecurityContext测试 | Mock getPrincipal(), getAuthorities() |
| UserPrincipal | 认证Token测试 | Mock getUserId(), getUsername() |
| GrantedAuthority | 权限测试 | Mock getAuthority() |
| UserRepository | AuthService测试 | Mock findByUsername(), findById() |
| JwtUtils | Token生成和验证 | Mock generateToken(), parseToken() |

### Mock示例

```java
@Mock
private Authentication authentication;

@Mock
private UserPrincipal userPrincipal;

@BeforeEach
void setUp() {
    MockitoAnnotations.openMocks(this);
    
    // Mock UserPrincipal
    when(userPrincipal.getUserId()).thenReturn("user123");
    when(userPrincipal.getUsername()).thenReturn("testuser");
    
    // Mock Authentication
    when(authentication.getPrincipal()).thenReturn(userPrincipal);
    when(authentication.isAuthenticated()).thenReturn(true);
}
```

---

## 测试依赖

```xml
<dependencies>
    <!-- JUnit 5 -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- Mockito -->
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-core</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- AssertJ -->
    <dependency>
        <groupId>org.assertj</groupId>
        <artifactId>assertj-core</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- Spring Boot Test (如果需要) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## 测试执行

运行测试：
```bash
mvn test -pl nebula/core/nebula-security
```

查看测试报告：
```bash
mvn surefire-report:report
```

---

## 验收标准

- 所有测试方法通过
- 核心功能测试覆盖率 >= 90%
- Mock对象使用正确
- ThreadLocal隔离测试通过

