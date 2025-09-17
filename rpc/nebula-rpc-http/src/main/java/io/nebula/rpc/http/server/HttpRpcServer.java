package io.nebula.rpc.http.server;

import io.nebula.rpc.core.message.RpcRequest;
import io.nebula.rpc.core.message.RpcResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HTTP RPC 服务器实现
 */
@Slf4j
@RestController
@RequestMapping("/rpc")
public class HttpRpcServer {
    
    private final ConcurrentHashMap<String, Object> serviceRegistry = new ConcurrentHashMap<>();
    
    /**
     * 注册服务
     */
    public void registerService(String serviceName, Object serviceImpl) {
        serviceRegistry.put(serviceName, serviceImpl);
        log.info("注册服务: {}", serviceName);
    }
    
    /**
     * 处理RPC请求
     */
    @PostMapping
    public ResponseEntity<RpcResponse> handleRpcRequest(@RequestBody RpcRequest request) {
        log.debug("收到RPC请求: requestId={}, service={}, method={}", 
                 request.getRequestId(), request.getServiceName(), request.getMethodName());
        
        try {
            // 查找服务实现
            Object serviceImpl = serviceRegistry.get(request.getServiceName());
            if (serviceImpl == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(RpcResponse.error(request.getRequestId(), 
                                "服务未找到: " + request.getServiceName()));
            }
            
            // 反射调用方法
            Object result = invokeMethod(serviceImpl, request);
            
            // 返回成功响应
            RpcResponse response = RpcResponse.success(request.getRequestId(), result);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("RPC调用异常: requestId={}", request.getRequestId(), e);
            RpcResponse response = RpcResponse.exception(request.getRequestId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 获取服务列表
     */
    @GetMapping("/services")
    public ResponseEntity<String[]> getServices() {
        String[] serviceNames = serviceRegistry.keySet().toArray(new String[0]);
        return ResponseEntity.ok(serviceNames);
    }
    
    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("RPC服务器运行正常");
    }
    
    private Object invokeMethod(Object serviceImpl, RpcRequest request) throws Exception {
        Class<?> serviceClass = serviceImpl.getClass();
        String methodName = request.getMethodName();
        Class<?>[] parameterTypes = request.getParameterTypes();
        Object[] parameters = request.getParameters();
        
        // 查找方法
        Method method = findMethod(serviceClass, methodName, parameterTypes);
        if (method == null) {
            throw new NoSuchMethodException("方法未找到: " + methodName);
        }
        
        // 设置方法可访问
        method.setAccessible(true);
        
        // 调用方法
        return method.invoke(serviceImpl, parameters);
    }
    
    private Method findMethod(Class<?> serviceClass, String methodName, Class<?>[] parameterTypes) {
        try {
            // 首先尝试精确匹配
            return serviceClass.getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            // 如果精确匹配失败，尝试兼容性匹配
            Method[] methods = serviceClass.getMethods();
            for (Method method : methods) {
                if (method.getName().equals(methodName) && 
                    isParameterTypesCompatible(method.getParameterTypes(), parameterTypes)) {
                    return method;
                }
            }
            return null;
        }
    }
    
    private boolean isParameterTypesCompatible(Class<?>[] declared, Class<?>[] actual) {
        if (declared.length != actual.length) {
            return false;
        }
        
        for (int i = 0; i < declared.length; i++) {
            if (!declared[i].isAssignableFrom(actual[i])) {
                return false;
            }
        }
        
        return true;
    }
}
