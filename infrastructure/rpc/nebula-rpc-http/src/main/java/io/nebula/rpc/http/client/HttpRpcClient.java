package io.nebula.rpc.http.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nebula.rpc.core.client.RpcClient;
import io.nebula.rpc.core.discovery.ServiceDiscoveryRpcClient;
import io.nebula.rpc.core.message.RpcRequest;
import io.nebula.rpc.core.message.RpcResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * HTTP RPC 客户端实现
 * 支持服务发现集成
 */
@Slf4j
public class HttpRpcClient implements ServiceDiscoveryRpcClient.ConfigurableRpcClient {
    
    private final RestTemplate restTemplate;
    private volatile String baseUrl;
    private final Executor executor;
    private final ObjectMapper objectMapper;
    
    public HttpRpcClient(RestTemplate restTemplate, String baseUrl, Executor executor, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.executor = executor;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public <T> T call(Class<T> serviceClass, String methodName, Object... args) {
        try {
            RpcRequest request = buildRequest(serviceClass.getName(), methodName, args);
            RpcResponse response = sendRequest(request);
            
            if (response.isSuccess()) {
                @SuppressWarnings("unchecked")
                T result = (T) response.getResult();
                return result;
            } else {
                throw new RuntimeException("RPC调用失败: " + response.getMessage());
            }
        } catch (Exception e) {
            log.error("RPC调用异常: serviceName={}, methodName={}", serviceClass.getName(), methodName, e);
            throw new RuntimeException("RPC调用异常", e);
        }
    }
    
    @Override
    public <T> CompletableFuture<T> callAsync(Class<T> serviceClass, String methodName, Object... args) {
        return CompletableFuture.supplyAsync(() -> call(serviceClass, methodName, args), executor);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> T createProxy(Class<T> serviceClass) {
        return (T) Proxy.newProxyInstance(
                serviceClass.getClassLoader(),
                new Class<?>[]{serviceClass},
                (proxy, method, args) -> {
                    // 对于Object类的方法,直接调用
                    if (method.getDeclaringClass() == Object.class) {
                        return method.invoke(this, args);
                    }
                    
                    // 使用实际方法的参数类型构建 RPC 请求,而不是根据参数值推断
                    RpcRequest request = buildRequestWithMethodInfo(serviceClass.getName(), method, args);
                    RpcResponse response = sendRequest(request);
                    
                    if (response.isSuccess()) {
                        Object result = response.getResult();
                        
                        // 类型转换(解决 Jackson 反序列化导致的类型不匹配)
                        Class<?> returnType = method.getReturnType();
                        if (result != null && !returnType.isInstance(result)) {
                            result = objectMapper.convertValue(result, returnType);
                        }
                        
                        return result;
                    } else {
                        throw new RuntimeException("RPC调用失败: " + response.getMessage());
                    }
                }
        );
    }
    
    @Override
    public String getServiceAddress(String serviceName) {
        // 简单实现：假设服务名就是路径
        return baseUrl + "/" + serviceName;
    }
    
    @Override
    public void close() {
        // HTTP客户端通常不需要显式关闭
        log.info("HTTP RPC客户端已关闭");
    }
    
    @Override
    public void setTargetAddress(String address) {
        // 确保地址格式正确
        if (address != null && !address.startsWith("http://") && !address.startsWith("https://")) {
            address = "http://" + address;
        }
        this.baseUrl = address;
        log.debug("设置目标地址: {}", address);
    }
    
    private RpcRequest buildRequest(String serviceName, String methodName, Object[] args) {
        return RpcRequest.builder()
                .requestId(UUID.randomUUID().toString())
                .serviceName(serviceName)
                .methodName(methodName)
                .parameters(args)
                .parameterTypes(getParameterTypes(args))
                .timestamp(System.currentTimeMillis())
                .timeout(30000) // 默认30秒超时
                .version("1.0")
                .build();
    }
    
    /**
     * 使用方法信息构建 RPC 请求
     * 直接使用方法声明的参数类型,避免通过参数值推断导致的类型不匹配
     */
    private RpcRequest buildRequestWithMethodInfo(String serviceName, java.lang.reflect.Method method, Object[] args) {
        return RpcRequest.builder()
                .requestId(UUID.randomUUID().toString())
                .serviceName(serviceName)
                .methodName(method.getName())
                .parameters(args)
                .parameterTypes(method.getParameterTypes())
                .timestamp(System.currentTimeMillis())
                .timeout(30000) // 默认30秒超时
                .version("1.0")
                .build();
    }
    
    private Class<?>[] getParameterTypes(Object[] args) {
        if (args == null || args.length == 0) {
            return new Class<?>[0];
        }
        
        Class<?>[] types = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            types[i] = args[i] != null ? args[i].getClass() : Object.class;
        }
        return types;
    }
    
    private RpcResponse sendRequest(RpcRequest request) {
        try {
            String url = baseUrl + "/rpc";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Request-ID", request.getRequestId());
            
            HttpEntity<RpcRequest> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<RpcResponse> responseEntity = restTemplate.exchange(
                    url, 
                    HttpMethod.POST, 
                    entity, 
                    RpcResponse.class
            );
            
            return responseEntity.getBody();
        } catch (Exception e) {
            log.error("发送RPC请求失败: requestId={}", request.getRequestId(), e);
            return RpcResponse.exception(request.getRequestId(), e);
        }
    }
}
