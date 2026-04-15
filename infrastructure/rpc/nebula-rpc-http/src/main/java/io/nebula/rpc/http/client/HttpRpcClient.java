package io.nebula.rpc.http.client;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nebula.rpc.core.client.RpcClient;
import io.nebula.rpc.core.discovery.ServiceDiscoveryRpcClient;
import io.nebula.rpc.core.message.RpcRequest;
import io.nebula.rpc.core.message.RpcResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.RestClient;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * HTTP RPC 客户端实现
 * 基于 Spring 6.1+ RestClient，支持服务发现集成
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
public class HttpRpcClient implements ServiceDiscoveryRpcClient.ConfigurableRpcClient {
    
    private final RestClient restClient;
    private volatile String baseUrl;
    private final Executor executor;
    private final ObjectMapper objectMapper;
    
    private final ConcurrentHashMap<MethodCacheKey, Method> methodCache = new ConcurrentHashMap<>();
    
    public HttpRpcClient(RestClient restClient, String baseUrl, Executor executor, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.baseUrl = baseUrl;
        this.executor = executor;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public <T> T call(Class<T> serviceClass, String methodName, Object... args) {
        String serviceName = serviceClass.getName();
        
        try {
            // 从服务接口解析方法声明，获取准确的参数类型和泛型返回类型
            Method serviceMethod = resolveMethod(serviceClass, methodName, args);
            
            // 使用方法声明的参数类型构建请求（避免 ArrayList vs List 不匹配）
            RpcRequest request;
            if (serviceMethod != null) {
                request = buildRequestWithMethodInfo(serviceName, serviceMethod, args);
            } else {
                request = buildRequest(serviceName, methodName, args);
            }
            
            RpcResponse response = sendRequest(request);
            
            if (response.isSuccess()) {
                Object result = response.getResult();
                
                if (result != null) {
                    result = convertResult(result, serviceMethod, serviceClass, methodName);
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
     * 转换 RPC 响应结果到目标类型
     * <p>
     * 使用方法的泛型返回类型（GenericReturnType）构建 JavaType，
     * 确保 List&lt;SearchResult&gt; 等嵌套泛型能被正确反序列化。
     * <p>
     * 典型场景: RpcResponse.result 被 Jackson 反序列化为 LinkedHashMap，
     * 需要转换为 SearchResponse（内含 List&lt;SearchResult&gt;）。
     */
    private Object convertResult(Object result, Method serviceMethod,
                                  Class<?> serviceClass, String methodName) {
        try {
            if (serviceMethod != null) {
                // 使用泛型返回类型，确保嵌套泛型正确处理
                Type genericReturnType = serviceMethod.getGenericReturnType();
                JavaType javaType = objectMapper.getTypeFactory().constructType(genericReturnType);
                // 对参数化类型（如 List<Dto>、Map<K,V>）始终执行 convertValue，
                // 因为 rawClass.isInstance 无法检测泛型元素类型不匹配
                boolean needsConversion = !javaType.getRawClass().isInstance(result)
                        || javaType.hasGenericTypes();
                if (needsConversion) {
                    result = objectMapper.convertValue(result, javaType);
                    log.debug("RPC响应类型转换成功: to={}, method={}.{}()",
                            javaType, serviceClass.getSimpleName(), methodName);
                }
                return result;
            }
            
            // fallback: 方法未解析成功，跳过转换直接返回
            log.debug("未解析到方法声明，跳过类型转换: {}.{}", serviceClass.getSimpleName(), methodName);
            return result;
            
        } catch (IllegalArgumentException e) {
            String errorMsg = String.format(
                "RPC响应类型转换失败: 服务=%s, 方法=%s, 实际类型=%s, 错误=%s",
                serviceClass.getName(), methodName, result.getClass().getName(), e.getMessage()
            );
            log.error(errorMsg, e);
            throw new RuntimeException(errorMsg, e);
        }
    }
    
    /**
     * 从服务接口解析方法声明（带缓存）
     * <p>
     * 先精确匹配（推断的参数类型），再按名称+参数数量做兼容匹配。
     * 解决客户端参数类型为实现类（ArrayList）而接口声明为接口类型（List）的问题。
     */
    private Method resolveMethod(Class<?> serviceClass, String methodName, Object[] args) {
        int paramCount = args != null ? args.length : 0;
        MethodCacheKey cacheKey = new MethodCacheKey(serviceClass, methodName, paramCount);
        
        Method cached = methodCache.get(cacheKey);
        if (cached != null) return cached;
        
        // 精确匹配
        Class<?>[] inferredTypes = getParameterTypes(args);
        try {
            Method method = serviceClass.getMethod(methodName, inferredTypes);
            methodCache.put(cacheKey, method);
            return method;
        } catch (NoSuchMethodException ignored) {
        }
        
        // 兼容匹配: 名称 + 参数数量
        Method candidate = null;
        for (Method m : serviceClass.getMethods()) {
            if (m.getName().equals(methodName) && m.getParameterCount() == paramCount) {
                candidate = m;
            }
        }
        if (candidate != null) {
            methodCache.put(cacheKey, candidate);
        } else {
            log.warn("无法解析方法: serviceClass={}, methodName={}, paramCount={}",
                    serviceClass.getName(), methodName, paramCount);
        }
        return candidate;
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
                    if (method.getDeclaringClass() == Object.class) {
                        return method.invoke(this, args);
                    }
                    
                    RpcRequest request = buildRequestWithMethodInfo(serviceClass.getName(), method, args);
                    RpcResponse response = sendRequest(request);
                    
                    if (response.isSuccess()) {
                        Object result = response.getResult();
                        
                        // 使用泛型返回类型做转换
                        if (result != null) {
                            Type genericReturnType = method.getGenericReturnType();
                            JavaType javaType = objectMapper.getTypeFactory().constructType(genericReturnType);
                            // 对参数化类型（如 List<Dto>）始终 convertValue，
                            // rawClass.isInstance 无法检测泛型元素类型不匹配
                            boolean needsConversion = !javaType.getRawClass().isInstance(result)
                                    || javaType.hasGenericTypes();
                            if (needsConversion) {
                                result = objectMapper.convertValue(result, javaType);
                            }
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
        return baseUrl + "/" + serviceName;
    }
    
    @Override
    public void close() {
        log.info("HTTP RPC客户端已关闭");
    }
    
    @Override
    public void setTargetAddress(String address) {
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
                .timeout(30000)
                .version("1.0")
                .build();
    }
    
    /**
     * 使用方法声明信息构建 RPC 请求
     * 直接使用方法声明的参数类型，避免参数值推断导致的类型不匹配
     */
    private RpcRequest buildRequestWithMethodInfo(String serviceName, Method method, Object[] args) {
        return RpcRequest.builder()
                .requestId(UUID.randomUUID().toString())
                .serviceName(serviceName)
                .methodName(method.getName())
                .parameters(args)
                .parameterTypes(method.getParameterTypes())
                .timestamp(System.currentTimeMillis())
                .timeout(30000)
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
            
            String jsonBody = objectMapper.writeValueAsString(request);
            log.debug("RPC请求序列化: url={}, bodyLength={}, service={}, method={}",
                    url, jsonBody.length(), request.getServiceName(), request.getMethodName());
            
            return restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("X-Request-ID", request.getRequestId())
                    .body(jsonBody)
                    .retrieve()
                    .body(RpcResponse.class);
        } catch (Exception e) {
            log.error("发送RPC请求失败: requestId={}", request.getRequestId(), e);
            return RpcResponse.exception(request.getRequestId(), e);
        }
    }
    
    /**
     * 方法缓存 Key（serviceClass + methodName + paramCount）
     */
    private static class MethodCacheKey {
        private final Class<?> serviceClass;
        private final String methodName;
        private final int paramCount;
        private final int hashCode;
        
        public MethodCacheKey(Class<?> serviceClass, String methodName, int paramCount) {
            this.serviceClass = serviceClass;
            this.methodName = methodName;
            this.paramCount = paramCount;
            this.hashCode = Objects.hash(serviceClass, methodName, paramCount);
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MethodCacheKey that = (MethodCacheKey) o;
            return paramCount == that.paramCount
                    && Objects.equals(serviceClass, that.serviceClass)
                    && Objects.equals(methodName, that.methodName);
        }
        
        @Override
        public int hashCode() {
            return hashCode;
        }
    }
}
