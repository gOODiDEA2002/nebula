# nebula-rpc-grpc 模块单元测试清单

## 模块说明

基于gRPC的高性能RPC实现模块，使用Protocol Buffers序列化，支持HTTP/2传输。

## 核心功能

1. gRPC客户端（创建代理、发起调用）
2. gRPC服务端（@RpcService自动注册）
3. 通用RPC消息（RpcRequest、RpcResponse）
4. 与HTTP RPC的统一接口

## 测试类清单

### 1. GrpcRpcClientTest

**测试类路径**: `io.nebula.rpc.grpc.client.GrpcRpcClient`  
**测试目的**: 验证gRPC客户端的代理创建和调用功能

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testCreateProxy() | createProxy(Class) | 测试创建gRPC代理对象 | ManagedChannel |
| testCall() | call(...) | 测试gRPC调用 | RpcServiceGrpc.RpcServiceBlockingStub |
| testCreateRequest() | createRequest(...) | 测试创建RPC请求对象 | 无 |
| testParseResponse() | parseResponse(...) | 测试解析RPC响应 | 无 |

**测试数据准备**:
- Mock ManagedChannel
- Mock gRPC Stub
- 准备测试接口和方法

**验证要点**:
- 代理对象正确创建
- RpcRequest正确构建
- RpcResponse正确解析
- 异常正确处理

**Mock示例**:
```java
@Mock
private ManagedChannel channel;

@Mock
private RpcServiceGrpc.RpcServiceBlockingStub stub;

@Test
void testCreateRequest() {
    GrpcRpcClient client = new GrpcRpcClient(channel);
    
    RpcRequest request = client.createRequest(
        "UserService",
        "getUser",
        new Class[]{Long.class},
        new Object[]{123L}
    );
    
    assertThat(request.getServiceName()).isEqualTo("UserService");
    assertThat(request.getMethodName()).isEqualTo("getUser");
    assertThat(request.getParametersCount()).isEqualTo(1);
}
```

---

### 2. GrpcRpcServerTest

**测试类路径**: `io.nebula.rpc.grpc.server.GrpcRpcServer`  
**测试目的**: 验证gRPC服务端的启动和服务注册

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testStart() | start() | 测试服务器启动 | Server, ServerBuilder |
| testRegisterService() | registerService(...) | 测试注册RPC服务 | 无 |
| testHandleRequest() | - | 测试处理RPC请求 | RpcRequest |
| testStop() | stop() | 测试服务器停止 | Server |

**测试数据准备**:
- Mock Server
- Mock ServerBuilder
- 创建测试服务实现

**验证要点**:
- 服务器正确启动
- 服务正确注册
- 请求正确路由
- 服务器正确停止

---

### 3. RpcMessageConverterTest

**测试类路径**: RPC消息转换器  
**测试目的**: 验证Java对象与Protobuf消息的转换

| 测试方法 | 被测试方法 | 测试目的 | Mock对象 |
|---------|-----------|---------|---------|
| testObjectToJson() | - | 测试对象转JSON | 无 |
| testJsonToObject() | - | 测试JSON转对象 | 无 |
| testSerializeParameters() | - | 测试序列化参数列表 | 无 |
| testDeserializeResult() | - | 测试反序列化返回值 | 无 |

**测试数据准备**:
- 准备测试POJO
- 准备测试JSON

**验证要点**:
- 序列化正确
- 反序列化正确
- 类型转换正确

---

## Mock策略

### 需要Mock的对象

| Mock对象 | 使用场景 | Mock行为 |
|---------|-----------|---------|
| ManagedChannel | gRPC客户端 | Mock newCall() |
| Server | gRPC服务端 | Mock start(), shutdown() |
| RpcServiceBlockingStub | gRPC调用 | Mock call() |

### 不需要真实gRPC服务器
**所有测试都应该Mock gRPC组件，不需要启动真实的gRPC服务器**。

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
        <groupId>io.grpc</groupId>
        <artifactId>grpc-testing</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## 测试执行

```bash
mvn test -pl nebula/infrastructure/rpc/nebula-rpc-grpc
```

---

## 验收标准

- 所有测试方法通过
- 核心功能测试覆盖率 >= 90%
- Mock对象使用正确
- Protobuf消息转换测试通过

