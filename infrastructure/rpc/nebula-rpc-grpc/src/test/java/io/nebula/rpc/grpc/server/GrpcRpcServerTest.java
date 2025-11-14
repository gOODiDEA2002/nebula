package io.nebula.rpc.grpc.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.stub.StreamObserver;
import io.nebula.rpc.grpc.proto.RpcRequest;
import io.nebula.rpc.grpc.proto.RpcResponse;
import io.nebula.rpc.grpc.test.TestRpcService;
import io.nebula.rpc.grpc.test.TestRpcServiceImpl;
import io.nebula.rpc.grpc.test.TestUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * GrpcRpcServer 测试类
 * 测试gRPC服务端的服务注册和请求处理
 */
@ExtendWith(MockitoExtension.class)
class GrpcRpcServerTest {

    @Mock
    private ApplicationContext mockApplicationContext;

    @Mock
    private StreamObserver<RpcResponse> mockResponseObserver;

    private GrpcRpcServer grpcRpcServer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        grpcRpcServer = new GrpcRpcServer(objectMapper);
        
        // 准备测试服务实例
        Map<String, Object> rpcServices = new HashMap<>();
        rpcServices.put("testRpcService", new TestRpcServiceImpl());
        
        // Mock ApplicationContext
        when(mockApplicationContext.getBeansWithAnnotation(any())).thenReturn(rpcServices);
        
        // 设置 ApplicationContext（会触发服务注册）
        grpcRpcServer.setApplicationContext(mockApplicationContext);
    }

    /**
     * 测试RPC服务注册
     */
    @Test
    void testRegisterService() {
        // 验证服务已注册（通过调用来验证）
        RpcRequest request = RpcRequest.newBuilder()
                .setRequestId("test-request-1")
                .setServiceName(TestRpcService.class.getName())
                .setMethodName("sayHello")
                .addParameterTypes(String.class.getName())
                .addParameters("\"World\"")
                .setTimestamp(System.currentTimeMillis())
                .build();
        
        grpcRpcServer.call(request, mockResponseObserver);
        
        // 验证 response 被发送
        verify(mockResponseObserver, times(1)).onNext(any(RpcResponse.class));
        verify(mockResponseObserver, times(1)).onCompleted();
    }

    /**
     * 测试处理RPC请求（成功场景）
     */
    @Test
    void testHandleRequestSuccess() throws Exception {
        // 准备请求
        RpcRequest request = RpcRequest.newBuilder()
                .setRequestId("test-request-2")
                .setServiceName(TestRpcService.class.getName())
                .setMethodName("sayHello")
                .addParameterTypes(String.class.getName())
                .addParameters("\"Test\"")
                .setTimestamp(System.currentTimeMillis())
                .build();
        
        // 执行调用
        grpcRpcServer.call(request, mockResponseObserver);
        
        // 捕获响应
        ArgumentCaptor<RpcResponse> responseCaptor = ArgumentCaptor.forClass(RpcResponse.class);
        verify(mockResponseObserver).onNext(responseCaptor.capture());
        
        // 验证响应
        RpcResponse response = responseCaptor.getValue();
        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getResult()).contains("Hello, Test");
    }

    /**
     * 测试处理RPC请求（带多个参数）
     */
    @Test
    void testHandleRequestWithMultipleParameters() {
        // 准备请求
        RpcRequest request = RpcRequest.newBuilder()
                .setRequestId("test-request-3")
                .setServiceName(TestRpcService.class.getName())
                .setMethodName("add")
                .addParameterTypes(Integer.class.getName())
                .addParameterTypes(Integer.class.getName())
                .addParameters("10")
                .addParameters("20")
                .setTimestamp(System.currentTimeMillis())
                .build();
        
        // 执行调用
        grpcRpcServer.call(request, mockResponseObserver);
        
        // 捕获响应
        ArgumentCaptor<RpcResponse> responseCaptor = ArgumentCaptor.forClass(RpcResponse.class);
        verify(mockResponseObserver).onNext(responseCaptor.capture());
        
        // 验证响应
        RpcResponse response = responseCaptor.getValue();
        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getResult()).isEqualTo("30");
    }

    /**
     * 测试处理RPC请求（返回对象）
     */
    @Test
    void testHandleRequestWithObjectReturn() {
        // 准备请求
        RpcRequest request = RpcRequest.newBuilder()
                .setRequestId("test-request-4")
                .setServiceName(TestRpcService.class.getName())
                .setMethodName("getUser")
                .addParameterTypes(Long.class.getName())
                .addParameters("123")
                .setTimestamp(System.currentTimeMillis())
                .build();
        
        // 执行调用
        grpcRpcServer.call(request, mockResponseObserver);
        
        // 捕获响应
        ArgumentCaptor<RpcResponse> responseCaptor = ArgumentCaptor.forClass(RpcResponse.class);
        verify(mockResponseObserver).onNext(responseCaptor.capture());
        
        // 验证响应
        RpcResponse response = responseCaptor.getValue();
        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getResult()).contains("User123");
    }

    /**
     * 测试处理RPC请求（服务未找到）
     */
    @Test
    void testHandleRequestServiceNotFound() {
        // 准备请求（不存在的服务）
        RpcRequest request = RpcRequest.newBuilder()
                .setRequestId("test-request-5")
                .setServiceName("com.example.NonExistentService")
                .setMethodName("someMethod")
                .setTimestamp(System.currentTimeMillis())
                .build();
        
        // 执行调用
        grpcRpcServer.call(request, mockResponseObserver);
        
        // 捕获响应
        ArgumentCaptor<RpcResponse> responseCaptor = ArgumentCaptor.forClass(RpcResponse.class);
        verify(mockResponseObserver).onNext(responseCaptor.capture());
        
        // 验证响应（应该是失败）
        RpcResponse response = responseCaptor.getValue();
        assertThat(response.getSuccess()).isFalse();
        assertThat(response.getErrorMessage()).contains("服务未找到");
    }

    /**
     * 测试处理RPC请求（方法未找到）
     */
    @Test
    void testHandleRequestMethodNotFound() {
        // 准备请求（不存在的方法）
        RpcRequest request = RpcRequest.newBuilder()
                .setRequestId("test-request-6")
                .setServiceName(TestRpcService.class.getName())
                .setMethodName("nonExistentMethod")
                .addParameterTypes(String.class.getName())
                .addParameters("\"Test\"")
                .setTimestamp(System.currentTimeMillis())
                .build();
        
        // 执行调用
        grpcRpcServer.call(request, mockResponseObserver);
        
        // 捕获响应
        ArgumentCaptor<RpcResponse> responseCaptor = ArgumentCaptor.forClass(RpcResponse.class);
        verify(mockResponseObserver).onNext(responseCaptor.capture());
        
        // 验证响应（应该是失败）
        RpcResponse response = responseCaptor.getValue();
        assertThat(response.getSuccess()).isFalse();
        assertThat(response.getErrorMessage()).contains("方法未找到");
    }

    /**
     * 测试处理RPC请求（无请求ID）
     */
    @Test
    void testHandleRequestWithoutRequestId() {
        // 准备请求（无请求ID）
        RpcRequest request = RpcRequest.newBuilder()
                .setServiceName(TestRpcService.class.getName())
                .setMethodName("sayHello")
                .addParameterTypes(String.class.getName())
                .addParameters("\"Test\"")
                .setTimestamp(System.currentTimeMillis())
                .build();
        
        // 执行调用
        grpcRpcServer.call(request, mockResponseObserver);
        
        // 捕获响应
        ArgumentCaptor<RpcResponse> responseCaptor = ArgumentCaptor.forClass(RpcResponse.class);
        verify(mockResponseObserver).onNext(responseCaptor.capture());
        
        // 验证响应（应该有自动生成的请求ID）
        RpcResponse response = responseCaptor.getValue();
        assertThat(response.getRequestId()).isNotEmpty();
        assertThat(response.getSuccess()).isTrue();
    }

    /**
     * 测试响应包含时间戳
     */
    @Test
    void testResponseContainsTimestamp() {
        // 准备请求
        RpcRequest request = RpcRequest.newBuilder()
                .setRequestId("test-request-7")
                .setServiceName(TestRpcService.class.getName())
                .setMethodName("sayHello")
                .addParameterTypes(String.class.getName())
                .addParameters("\"Test\"")
                .setTimestamp(System.currentTimeMillis())
                .build();
        
        // 执行调用
        grpcRpcServer.call(request, mockResponseObserver);
        
        // 捕获响应
        ArgumentCaptor<RpcResponse> responseCaptor = ArgumentCaptor.forClass(RpcResponse.class);
        verify(mockResponseObserver).onNext(responseCaptor.capture());
        
        // 验证响应包含时间戳
        RpcResponse response = responseCaptor.getValue();
        assertThat(response.getTimestamp()).isGreaterThan(0);
    }

    // ========== 服务自动注册测试 ==========

    /**
     * 测试服务自动发现和注册
     * 场景：@RpcService 标注的Bean被自动扫描并注册
     * 验证：serviceRegistry 包含正确的服务名称和实例
     */
    @Test
    void testAutoServiceDiscovery() {
        // Given: setUp() 已经触发了服务注册
        
        // When: 通过反射获取 serviceRegistry
        @SuppressWarnings("unchecked")
        Map<String, Object> serviceRegistry = (Map<String, Object>) 
            ReflectionTestUtils.getField(grpcRpcServer, "serviceRegistry");
        
        // Then: 验证服务已注册
        assertThat(serviceRegistry).isNotNull();
        assertThat(serviceRegistry).containsKey(TestRpcService.class.getName());
        assertThat(serviceRegistry.get(TestRpcService.class.getName()))
            .isInstanceOf(TestRpcServiceImpl.class);
    }

    /**
     * 测试接口自动推导
     * 场景：@RpcService 没有指定接口，自动查找标注了 @RpcClient 的接口
     * 验证：服务名称使用接口的全限定名
     */
    @Test
    void testAutoInterfaceDetection() {
        // Given: TestRpcServiceImpl 实现了 @RpcClient 标注的 TestRpcService 接口
        
        // When: 通过反射获取 serviceRegistry
        @SuppressWarnings("unchecked")
        Map<String, Object> serviceRegistry = (Map<String, Object>) 
            ReflectionTestUtils.getField(grpcRpcServer, "serviceRegistry");
        
        // Then: 验证服务名称是接口的全限定名（而非实现类的名称）
        String expectedServiceName = TestRpcService.class.getName();
        assertThat(serviceRegistry).containsKey(expectedServiceName);
        
        // 验证不包含实现类的名称
        String implClassName = TestRpcServiceImpl.class.getName();
        assertThat(serviceRegistry).doesNotContainKey(implClassName);
    }

    /**
     * 测试多个RPC服务同时注册
     * 场景：多个 @RpcService Bean 被同时注册
     * 验证：serviceRegistry 包含所有服务
     */
    @Test
    void testMultipleServiceRegistration() {
        // Given: 准备多个测试服务
        Map<String, Object> rpcServices = new HashMap<>();
        rpcServices.put("testRpcService1", new TestRpcServiceImpl());
        rpcServices.put("testRpcService2", new TestRpcServiceImpl());
        
        // Mock ApplicationContext
        ApplicationContext multiServiceContext = mock(ApplicationContext.class);
        when(multiServiceContext.getBeansWithAnnotation(any())).thenReturn(rpcServices);
        
        // When: 重新创建 server 并设置 context（触发注册）
        GrpcRpcServer multiServiceServer = new GrpcRpcServer(objectMapper);
        multiServiceServer.setApplicationContext(multiServiceContext);
        
        // Then: 验证多个服务都已注册
        @SuppressWarnings("unchecked")
        Map<String, Object> serviceRegistry = (Map<String, Object>) 
            ReflectionTestUtils.getField(multiServiceServer, "serviceRegistry");
        
        // 注意：因为两个服务实例实现同一个接口，所以实际只会注册一个服务名
        // （后注册的会覆盖先注册的，但这在实际使用中应避免）
        assertThat(serviceRegistry).containsKey(TestRpcService.class.getName());
        assertThat(serviceRegistry).hasSize(1); // 同一个接口名，只有一个entry
    }

    /**
     * 测试服务注册后的实际调用
     * 场景：通过注册的服务名调用方法
     * 验证：调用能正确路由到对应的服务实例
     */
    @Test
    void testServiceCallAfterRegistration() {
        // Given: 服务已注册（setUp中完成）
        
        // When: 通过服务名调用方法
        RpcRequest request = RpcRequest.newBuilder()
                .setRequestId("test-auto-registration")
                .setServiceName(TestRpcService.class.getName()) // 使用接口名
                .setMethodName("sayHello")
                .addParameterTypes(String.class.getName())
                .addParameters("\"AutoDiscovery\"")
                .setTimestamp(System.currentTimeMillis())
                .build();
        
        grpcRpcServer.call(request, mockResponseObserver);
        
        // Then: 验证调用成功
        ArgumentCaptor<RpcResponse> responseCaptor = ArgumentCaptor.forClass(RpcResponse.class);
        verify(mockResponseObserver).onNext(responseCaptor.capture());
        
        RpcResponse response = responseCaptor.getValue();
        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getResult()).contains("Hello, AutoDiscovery");
    }
}

