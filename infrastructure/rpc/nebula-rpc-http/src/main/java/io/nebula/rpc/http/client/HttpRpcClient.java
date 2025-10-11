package io.nebula.rpc.http.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nebula.rpc.core.client.RpcClient;
import io.nebula.rpc.core.discovery.ServiceDiscoveryRpcClient;
import io.nebula.rpc.core.message.RpcRequest;
import io.nebula.rpc.core.message.RpcResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
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
    
    // 方法缓存：避免重复反射查找
    private final ConcurrentHashMap<MethodCacheKey, Method> methodCache = new ConcurrentHashMap<>();
    
    public HttpRpcClient(RestTemplate restTemplate, String baseUrl, Executor executor, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.executor = executor;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public <T> T call(Class<T> serviceClass, String methodName, Object... args) {
        String serviceName = serviceClass.getName();
        
        try {
            RpcRequest request = buildRequest(serviceName, methodName, args);
            RpcResponse response = sendRequest(request);
            
            if (response.isSuccess()) {
                Object result = response.getResult();
                
                // 类型转换(解决 Jackson 反序列化导致的类型不匹配)
                // 通过反射获取方法的返回类型
                Class<?> returnType = getMethodReturnType(serviceClass, methodName, args);
                if (result != null && returnType != null && !returnType.isInstance(result)) {
                    try {
                        // 尝试使用 ObjectMapper 进行类型转换
                        result = objectMapper.convertValue(result, returnType);
                        log.debug("RPC响应类型转换成功: from={} to={}, method={}.{}()", 
                                result.getClass().getSimpleName(), 
                                returnType.getSimpleName(),
                                serviceClass.getSimpleName(),
                                methodName);
                    } catch (IllegalArgumentException e) {
                        // 类型转换失败，提供详细的错误信息
                        String errorMsg = String.format(
                            "RPC响应类型转换失败: " +
                            "服务=%s, 方法=%s, " +
                            "期望类型=%s, 实际类型=%s, " +
                            "错误=%s",
                            serviceName,
                            methodName,
                            returnType.getName(),
                            result.getClass().getName(),
                            e.getMessage()
                        );
                        log.error(errorMsg, e);
                        throw new RuntimeException(errorMsg, e);
                    }
                }
                
                @SuppressWarnings("unchecked")
                T typedResult = (T) result;
                return typedResult;
            } else {
                String errorMsg = String.format(
                    "RPC调用失败: 服务=%s, 方法=%s, 错误=%s",
                    serviceName, methodName, response.getMessage()
                );
                log.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }
        } catch (RuntimeException e) {
            // 直接抛出 RuntimeException（包括类型转换异常）
            throw e;
        } catch (Exception e) {
            String errorMsg = String.format(
                "RPC调用异常: 服务=%s, 方法=%s, 异常类型=%s, 错误=%s",
                serviceName, methodName, e.getClass().getSimpleName(), e.getMessage()
            );
            log.error(errorMsg, e);
            throw new RuntimeException(errorMsg, e);
        }
    }
    
    /**
     * 获取方法的返回类型（带缓存）
     * 
     * @param serviceClass 服务接口类
     * @param methodName 方法名
     * @param args 参数
     * @return 返回类型，如果找不到则返回 null
     */
    private Class<?> getMethodReturnType(Class<?> serviceClass, String methodName, Object[] args) {
        // 获取参数类型
        Class<?>[] parameterTypes = getParameterTypes(args);
        
        // 创建缓存 key
        MethodCacheKey cacheKey = new MethodCacheKey(serviceClass, methodName, parameterTypes);
        
        // 尝试从缓存获取
        Method cachedMethod = methodCache.get(cacheKey);
        if (cachedMethod != null) {
            return cachedMethod.getReturnType();
        }
        
        // 缓存未命中，执行反射查找
        try {
            Method method = serviceClass.getMethod(methodName, parameterTypes);
            // 放入缓存
            methodCache.put(cacheKey, method);
            return method.getReturnType();
        } catch (NoSuchMethodException e) {
            log.warn("无法找到方法: serviceClass={}, methodName={}, 将尝试遍历所有方法", 
                    serviceClass.getName(), methodName);
            
            // 如果精确匹配失败，尝试通过方法名匹配
            for (Method method : serviceClass.getMethods()) {
                if (method.getName().equals(methodName)) {
                    // 放入缓存（使用实际的参数类型）
                    methodCache.put(cacheKey, method);
                    return method.getReturnType();
                }
            }
            
            log.error("无法找到方法的返回类型: serviceClass={}, methodName={}", 
                    serviceClass.getName(), methodName);
            return null;
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
    
    /**
     * 方法缓存 Key
     * 用于缓存反射查找的方法
     */
    private static class MethodCacheKey {
        private final Class<?> serviceClass;
        private final String methodName;
        private final Class<?>[] parameterTypes;
        private final int hashCode;
        
        public MethodCacheKey(Class<?> serviceClass, String methodName, Class<?>[] parameterTypes) {
            this.serviceClass = serviceClass;
            this.methodName = methodName;
            this.parameterTypes = parameterTypes;
            // 预计算 hashCode 以提高性能
            this.hashCode = Objects.hash(serviceClass, methodName, Arrays.hashCode(parameterTypes));
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MethodCacheKey that = (MethodCacheKey) o;
            return Objects.equals(serviceClass, that.serviceClass) &&
                   Objects.equals(methodName, that.methodName) &&
                   Arrays.equals(parameterTypes, that.parameterTypes);
        }
        
        @Override
        public int hashCode() {
            return hashCode;
        }
    }
}
