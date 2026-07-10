# Nebula RPC Core - 测试指南

> RPC通信核心抽象模块的测试策略与实践

## 目录

- [测试策略](#测试策略)
- [单元测试](#单元测试)
- [接口契约测试](#接口契约测试)
- [Mock测试](#mock测试)
- [测试工具](#测试工具)

---

## 测试策略

### 测试层次

由于 `nebula-rpc-core` 是抽象模块，测试主要分为：

1. **单元测试**：测试RPC请求/响应模型和工具类
2. **接口契约测试**：定义实现模块必须遵守的契约
3. **Mock测试**：使用Mock进行RPC调用测试

### 测试覆盖目标

- RPC模型类：100%
- 接口定义完整性：100%
- 异常处理：100%

---

## 单元测试

### 1. RpcRequest测试

```java
/**
 * RpcRequest测试
 */
class RpcRequestTest {
    
    @Test
    void testRequestBuilder() {
        RpcRequest request = RpcRequest.builder()
                .requestId("req-001")
                .serviceName("UserService")
                .methodName("getUserById")
                .parameters(new Object[]{1L})
                .timeout(Duration.ofSeconds(5))
                .build();
        
        assertThat(request.getRequestId()).isEqualTo("req-001");
        assertThat(request.getServiceName()).isEqualTo("UserService");
        assertThat(request.getMethodName()).isEqualTo("getUserById");
        assertThat(request.getParameters()).hasSize(1);
        assertThat(request.getTimeout()).isEqualTo(Duration.ofSeconds(5));
    }
    
    @Test
    void testRequestWithHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("trace-id", "trace-123");
        headers.put("user-id", "user-456");
        
        RpcRequest request = RpcRequest.builder()
                .requestId("req-002")
                .serviceName("OrderService")
                .methodName("createOrder")
                .headers(headers)
                .build();
        
        assertThat(request.getHeaders())
                .containsEntry("trace-id", "trace-123")
                .containsEntry("user-id", "user-456");
    }
    
    @Test
    void testRequestIdGeneration() {
        RpcRequest request = RpcRequest.builder()
                .serviceName("TestService")
                .methodName("testMethod")
                .build();
        
        // 应该自动生成requestId
        assertThat(request.getRequestId()).isNotBlank();
    }
}
```

### 2. RpcResponse测试

```java
/**
 * RpcResponse测试
 */
class RpcResponseTest {
    
    @Test
    void testSuccessResponse() {
        RpcResponse<String> response = RpcResponse.success("req-001", "Success result");
        
        assertThat(response.getRequestId()).isEqualTo("req-001");
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getResult()).isEqualTo("Success result");
        assertThat(response.getError()).isNull();
    }
    
    @Test
    void testFailureResponse() {
        RpcResponse<?> response = RpcResponse.failure("req-002", "Service unavailable");
        
        assertThat(response.getRequestId()).isEqualTo("req-002");
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getError()).isEqualTo("Service unavailable");
        assertThat(response.getResult()).isNull();
    }
    
    @Test
    void testResponseWithCode() {
        RpcResponse<String> response = RpcResponse.<String>builder()
                .requestId("req-003")
                .success(true)
                .result("result")
                .code(200)
                .build();
        
        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.isSuccess()).isTrue();
    }
    
    @Test
    void testResponseWithException() {
        Exception exception = new RuntimeException("Test exception");
        
        RpcResponse<?> response = RpcResponse.failure("req-004", exception);
        
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getException()).isEqualTo(exception);
        assertThat(response.getError()).contains("Test exception");
    }
}
```

### 3. RpcServiceRegistry测试

```java
/**
 * RpcServiceRegistry测试
 */
class RpcServiceRegistryTest {
    
    private RpcServiceRegistry registry;
    
    @BeforeEach
    void setUp() {
        registry = new DefaultRpcServiceRegistry();
    }
    
    @Test
    void testRegisterService() {
        UserService userService = new UserServiceImpl();
        
        registry.register("UserService", userService);
        
        assertThat(registry.isRegistered("UserService")).isTrue();
        assertThat(registry.getService("UserService")).isEqualTo(userService);
    }
    
    @Test
    void testUnregisterService() {
        UserService userService = new UserServiceImpl();
        
        registry.register("UserService", userService);
        assertThat(registry.isRegistered("UserService")).isTrue();
        
        registry.unregister("UserService");
        assertThat(registry.isRegistered("UserService")).isFalse();
    }
    
    @Test
    void testGetNonExistentService() {
        assertThat(registry.getService("NonExistent")).isNull();
    }
    
    @Test
    void testGetAllServices() {
        registry.register("Service1", new Object());
        registry.register("Service2", new Object());
        registry.register("Service3", new Object());
        
        assertThat(registry.getAllServiceNames()).hasSize(3);
        assertThat(registry.getAllServiceNames())
                .contains("Service1", "Service2", "Service3");
    }
    
    interface UserService {
        String getUserName(Long userId);
    }
    
    static class UserServiceImpl implements UserService {
        @Override
        public String getUserName(Long userId) {
            return "User" + userId;
        }
    }
}
```

---

## 接口契约测试

### 1. RpcClient契约测试基类

```java
/**
 * RpcClient接口契约测试基类
 * 
 * 所有RpcClient的实现类都应该继承此类并实现抽象方法
 */
public abstract class RpcClientContractTest {
    
    /**
     * 子类需要提供RpcClient实现
     */
    protected abstract RpcClient getRpcClient();
    
    /**
     * 子类需要提供测试服务名称
     */
    protected abstract String getTestServiceName();
    
    /**
     * 子类需要启动测试RPC服务器
     */
    protected abstract void startTestServer();
    
    /**
     * 子类需要停止测试RPC服务器
     */
    protected abstract void stopTestServer();
    
    @BeforeEach
    void setUp() {
        startTestServer();
    }
    
    @AfterEach
    void tearDown() {
        stopTestServer();
    }
    
    @Test
    void testSimpleRpcCall() {
        RpcClient client = getRpcClient();
        String serviceName = getTestServiceName();
        
        RpcRequest request = RpcRequest.builder()
                .serviceName(serviceName)
                .methodName("echo")
                .parameters(new Object[]{"Hello"})
                .build();
        
        RpcResponse<String> response = client.call(request);
        
        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getResult()).isEqualTo("Hello");
    }
    
    @Test
    void testAsyncRpcCall() throws Exception {
        RpcClient client = getRpcClient();
        String serviceName = getTestServiceName();
        
        RpcRequest request = RpcRequest.builder()
                .serviceName(serviceName)
                .methodName("echo")
                .parameters(new Object[]{"Async Hello"})
                .build();
        
        CompletableFuture<RpcResponse<String>> future = client.callAsync(request);
        
        assertThat(future).isNotNull();
        
        RpcResponse<String> response = future.get(5, TimeUnit.SECONDS);
        
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getResult()).isEqualTo("Async Hello");
    }
    
    @Test
    void testRpcCallWithTimeout() {
        RpcClient client = getRpcClient();
        String serviceName = getTestServiceName();
        
        RpcRequest request = RpcRequest.builder()
                .serviceName(serviceName)
                .methodName("slowMethod")
                .timeout(Duration.ofSeconds(1))
                .build();
        
        RpcResponse<?> response = client.call(request);
        
        // 应该超时
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getError()).containsIgnoringCase("timeout");
    }
    
    @Test
    void testRpcCallWithException() {
        RpcClient client = getRpcClient();
        String serviceName = getTestServiceName();
        
        RpcRequest request = RpcRequest.builder()
                .serviceName(serviceName)
                .methodName("throwException")
                .build();
        
        RpcResponse<?> response = client.call(request);
        
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getException()).isNotNull();
    }
    
    @Test
    void testRpcCallNonExistentService() {
        RpcClient client = getRpcClient();
        
        RpcRequest request = RpcRequest.builder()
                .serviceName("NonExistentService")
                .methodName("anyMethod")
                .build();
        
        RpcResponse<?> response = client.call(request);
        
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getError()).containsIgnoringCase("not found");
    }
    
    @Test
    void testRpcCallNonExistentMethod() {
        RpcClient client = getRpcClient();
        String serviceName = getTestServiceName();
        
        RpcRequest request = RpcRequest.builder()
                .serviceName(serviceName)
                .methodName("nonExistentMethod")
                .build();
        
        RpcResponse<?> response = client.call(request);
        
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getError()).containsIgnoringCase("method not found");
    }
}
```

### 2. RpcServer契约测试基类

```java
/**
 * RpcServer接口契约测试基类
 */
public abstract class RpcServerContractTest {
    
    /**
     * 子类需要提供RpcServer实现
     */
    protected abstract RpcServer getRpcServer();
    
    /**
     * 子类需要提供RpcClient实现（用于测试）
     */
    protected abstract RpcClient getRpcClient();
    
    @Test
    void testServerStartAndStop() {
        RpcServer server = getRpcServer();
        
        // 启动服务器
        server.start();
        assertThat(server.isRunning()).isTrue();
        
        // 停止服务器
        server.stop();
        assertThat(server.isRunning()).isFalse();
    }
    
    @Test
    void testRegisterAndCallService() {
        RpcServer server = getRpcServer();
        RpcClient client = getRpcClient();
        
        // 注册服务
        TestService testService = new TestServiceImpl();
        server.registerService("TestService", testService);
        
        // 启动服务器
        server.start();
        
        // 调用服务
        RpcRequest request = RpcRequest.builder()
                .serviceName("TestService")
                .methodName("sayHello")
                .parameters(new Object[]{"World"})
                .build();
        
        RpcResponse<String> response = client.call(request);
        
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getResult()).isEqualTo("Hello, World!");
        
        // 停止服务器
        server.stop();
    }
    
    @Test
    void testUnregisterService() {
        RpcServer server = getRpcServer();
        RpcClient client = getRpcClient();
        
        // 注册服务
        TestService testService = new TestServiceImpl();
        server.registerService("TestService", testService);
        server.start();
        
        // 取消注册
        server.unregisterService("TestService");
        
        // 尝试调用
        RpcRequest request = RpcRequest.builder()
                .serviceName("TestService")
                .methodName("sayHello")
                .build();
        
        RpcResponse<?> response = client.call(request);
        
        assertThat(response.isSuccess()).isFalse();
        
        server.stop();
    }
    
    interface TestService {
        String sayHello(String name);
    }
    
    static class TestServiceImpl implements TestService {
        @Override
        public String sayHello(String name) {
            return "Hello, " + name + "!";
        }
    }
}
```

---

## Mock测试

### 1. Mock RpcClient测试

```java
/**
 * Mock RpcClient测试
 */
@ExtendWith(MockitoExtension.class)
class MockRpcClientTest {
    
    @Mock
    private RpcClient rpcClient;
    
    @Test
    void testMockCall() {
        // 准备Mock响应
        RpcResponse<String> mockResponse = RpcResponse.success("req-001", "Mocked result");
        
        when(rpcClient.call(any(RpcRequest.class)))
                .thenReturn(mockResponse);
        
        // 执行调用
        RpcRequest request = RpcRequest.builder()
                .serviceName("TestService")
                .methodName("testMethod")
                .build();
        
        RpcResponse<String> response = rpcClient.call(request);
        
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getResult()).isEqualTo("Mocked result");
        
        // 验证调用
        verify(rpcClient).call(request);
    }
    
    @Test
    void testMockAsyncCall() {
        // 准备Mock响应
        RpcResponse<String> mockResponse = RpcResponse.success("req-002", "Async result");
        CompletableFuture<RpcResponse<String>> future = CompletableFuture.completedFuture(mockResponse);
        
        when(rpcClient.callAsync(any(RpcRequest.class)))
                .thenReturn(future);
        
        // 执行异步调用
        RpcRequest request = RpcRequest.builder()
                .serviceName("TestService")
                .methodName("asyncMethod")
                .build();
        
        CompletableFuture<RpcResponse<String>> result = rpcClient.callAsync(request);
        
        assertThat(result).isCompleted();
        assertThat(result.join().getResult()).isEqualTo("Async result");
    }
}
```

### 2. 服务调用测试

```java
/**
 * 服务调用测试
 */
@SpringBootTest
class ServiceInvocationTest {
    
    @MockBean
    private RpcClient rpcClient;
    
    @Autowired
    private UserServiceClient userServiceClient;
    
    @Test
    void testGetUser() {
        // Mock RPC响应
        User mockUser = new User(1L, "John Doe", "john@example.com");
        RpcResponse<User> mockResponse = RpcResponse.success("req-001", mockUser);
        
        when(rpcClient.call(any(RpcRequest.class)))
                .thenReturn(mockResponse);
        
        // 调用服务
        User user = userServiceClient.getUserById(1L);
        
        assertThat(user).isNotNull();
        assertThat(user.getId()).isEqualTo(1L);
        assertThat(user.getName()).isEqualTo("John Doe");
        
        // 验证RPC调用
        verify(rpcClient).call(argThat(request ->
                "UserService".equals(request.getServiceName()) &&
                "getUserById".equals(request.getMethodName())
        ));
    }
    
    @Test
    void testGetUserNotFound() {
        // Mock RPC失败响应
        RpcResponse<User> mockResponse = RpcResponse.failure("req-002", "User not found");
        
        when(rpcClient.call(any(RpcRequest.class)))
                .thenReturn(mockResponse);
        
        // 验证异常
        assertThatThrownBy(() -> userServiceClient.getUserById(999L))
                .isInstanceOf(RpcException.class)
                .hasMessageContaining("User not found");
    }
    
    @Data
    @AllArgsConstructor
    static class User {
        private Long id;
        private String name;
        private String email;
    }
    
    @Service
    static class UserServiceClient {
        
        private final RpcClient rpcClient;
        
        UserServiceClient(RpcClient rpcClient) {
            this.rpcClient = rpcClient;
        }
        
        public User getUserById(Long userId) {
            RpcRequest request = RpcRequest.builder()
                    .serviceName("UserService")
                    .methodName("getUserById")
                    .parameters(new Object[]{userId})
                    .build();
            
            RpcResponse<User> response = rpcClient.call(request);
            
            if (!response.isSuccess()) {
                throw new RpcException(response.getError());
            }
            
            return response.getResult();
        }
    }
}
```

---

## 测试工具

### 1. 测试RPC请求构建器

```java
/**
 * 测试RPC请求构建器
 */
public class TestRpcRequestBuilder {
    
    /**
     * 创建简单RPC请求
     */
    public static RpcRequest createSimple(String serviceName, String methodName, Object... parameters) {
        return RpcRequest.builder()
                .requestId(UUID.randomUUID().toString())
                .serviceName(serviceName)
                .methodName(methodName)
                .parameters(parameters)
                .build();
    }
    
    /**
     * 创建带超时的RPC请求
     */
    public static RpcRequest createWithTimeout(String serviceName, String methodName, 
                                               Duration timeout, Object... parameters) {
        return RpcRequest.builder()
                .requestId(UUID.randomUUID().toString())
                .serviceName(serviceName)
                .methodName(methodName)
                .parameters(parameters)
                .timeout(timeout)
                .build();
    }
    
    /**
     * 创建带Headers的RPC请求
     */
    public static RpcRequest createWithHeaders(String serviceName, String methodName, 
                                               Map<String, String> headers, Object... parameters) {
        return RpcRequest.builder()
                .requestId(UUID.randomUUID().toString())
                .serviceName(serviceName)
                .methodName(methodName)
                .parameters(parameters)
                .headers(headers)
                .build();
    }
}
```

### 2. 测试RPC服务实现

```java
/**
 * 测试RPC服务实现
 */
public class TestRpcService {
    
    /**
     * Echo方法
     */
    public String echo(String message) {
        return message;
    }
    
    /**
     * 延迟方法
     */
    public String slowMethod() throws InterruptedException {
        Thread.sleep(5000);
        return "Completed";
    }
    
    /**
     * 抛出异常的方法
     */
    public void throwException() {
        throw new RuntimeException("Test exception");
    }
    
    /**
     * 计算方法
     */
    public int add(int a, int b) {
        return a + b;
    }
    
    /**
     * 复杂对象方法
     */
    public User getUserById(Long userId) {
        return new User(userId, "User" + userId, "user" + userId + "@example.com");
    }
    
    @Data
    @AllArgsConstructor
    public static class User {
        private Long id;
        private String name;
        private String email;
    }
}
```

---

## 相关文档

- [README.md](./README.md) - 模块介绍
- [EXAMPLE.md](./EXAMPLE.md) - 使用示例
- [CONFIG.md](./CONFIG.md) - 配置指南
- [ROADMAP.md](./ROADMAP.md) - 发展路线图

---

**最后更新**: 2025-11-20  
**文档版本**: v1.0

