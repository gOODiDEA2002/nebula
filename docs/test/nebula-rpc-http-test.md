# nebula-rpc-http 模块单元测试清单

## 模块说明

基于HTTP协议的RPC实现模块，支持声明式和编程式两种调用方式，集成服务发现和负载均衡。

## 核心功能

1. HTTP RPC客户端（@RpcClient声明式、编程式调用）
2. HTTP RPC服务器（@RpcService自动注册）
3. 同步和异步调用
4. 服务发现集成
5. 负载均衡

## 测试类清单

### 1. HttpRpcClientTest

**测试类路径**: `io.nebula.rpc.http.client.HttpRpcClient`  
**测试目的**: 验证HTTP RPC客户端的调用功能

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testCall() | call(Class, String, Object...) | 测试RPC同步调用 | RestTemplate |
| testCallAsync() | callAsync(Class, String, Object...) | 测试RPC异步调用 | RestTemplate |
| testCreateProxy() | createProxy(Class) | 测试创建代理对象 | RestTemplate |
| testProxyMethodInvocation() | 代理方法调用 | 测试代理对象方法调用 | RestTemplate |

**测试数据准备**:
- Mock RestTemplate
- 准备测试服务接口
- 准备测试响应数据

**验证要点**:
- HTTP请求正确发送
- 响应正确反序列化
- 异步调用返回CompletableFuture
- 代理对象方法正常工作

**Mock示例**:
```java
@Mock
private RestTemplate restTemplate;

@Test
void testCall() {
    HttpRpcClient client = new HttpRpcClient(restTemplate);
    
    when(restTemplate.postForObject(
        any(String.class), 
        any(), 
        eq(String.class)
    )).thenReturn("\"result\"");
    
    String result = client.call(UserService.class, "getUser", 123L);
    
    assertThat(result).isEqualTo("result");
    verify(restTemplate).postForObject(anyString(), any(), eq(String.class));
}
```

---

### 2. HttpRpcServerTest

**测试类路径**: `io.nebula.rpc.http.server.HttpRpcServer`  
**测试目的**: 验证HTTP RPC服务器的服务注册和方法调用

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testRegisterService() | registerService(Class, Object) | 测试注册RPC服务 | 无 |
| testInvokeMethod() | invoke(String, String, Object[]) | 测试方法调用 | 无 |
| testServiceNotFound() | invoke(...) | 测试服务不存在抛出异常 | 无 |
| testMethodNotFound() | invoke(...) | 测试方法不存在抛出异常 | 无 |

**测试数据准备**:
- 创建测试服务实现类
- 准备测试方法参数

**验证要点**:
- 服务正确注册
- 方法正确调用
- 异常正确抛出
- 返回值正确

---

### 3. RpcClientFactoryBeanTest

**测试类路径**: `@RpcClient`注解处理  
**测试目的**: 验证RPC客户端的自动代理创建

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testFactoryBeanCreation() | getObject() | 测试FactoryBean创建代理对象 | HttpRpcClient |
| testProxyInterfaceImplementation() | - | 测试代理实现接口方法 | - |

**测试数据准备**:
- 定义@RpcClient注解的接口
- Mock HttpRpcClient

**验证要点**:
- 代理对象正确创建
- 接口方法可调用
- 注解配置生效

---

## Mock策略

### 需要Mock的对象

| Mock对象 | 使用场景 | Mock行为 |
|---------|-----------|---------|
| RestTemplate | HTTP调用 | Mock postForObject(), exchange() |
| HttpRpcClient | FactoryBean测试 | Mock call(), callAsync() |
| ServiceDiscovery | 服务发现集成 | Mock getInstances() |

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
mvn test -pl nebula/infrastructure/rpc/nebula-rpc-http
```

---

## 验收标准

- 所有测试方法通过
- 核心功能测试覆盖率 >= 90%
- Mock对象使用正确
- 代理功能测试通过

