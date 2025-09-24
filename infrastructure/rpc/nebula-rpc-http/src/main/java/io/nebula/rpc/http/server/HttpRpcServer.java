package io.nebula.rpc.http.server;

import io.nebula.rpc.core.server.RpcServer;
import io.nebula.rpc.core.message.RpcRequest;
import io.nebula.rpc.core.message.RpcResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * HTTP RPC 服务器实现
 * 实现 RpcServer 接口，提供标准的服务注册和生命周期管理
 */
@Slf4j
@Component
public class HttpRpcServer implements RpcServer, InitializingBean, DisposableBean {
    
    private final ConcurrentHashMap<String, Object> serviceRegistry = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private int port = 8080; // 默认端口
    
    @Override
    public void afterPropertiesSet() throws Exception {
        // Spring Boot 环境下自动启动
        log.info("HTTP RPC 服务器初始化完成");
        running.set(true);
    }
    
    @Override
    public void destroy() throws Exception {
        shutdown();
    }
    
    @Override
    public <T> void registerService(Class<T> serviceClass, T serviceImpl) {
        registerService(serviceClass.getName(), serviceClass, serviceImpl);
    }
    
    @Override
    public <T> void registerService(String serviceName, Class<T> serviceClass, T serviceImpl) {
        serviceRegistry.put(serviceName, serviceImpl);
        log.info("注册服务: serviceName={}, serviceClass={}", serviceName, serviceClass.getName());
    }
    
    /**
     * 注册服务（兼容方法）
     */
    public void registerService(String serviceName, Object serviceImpl) {
        serviceRegistry.put(serviceName, serviceImpl);
        log.info("注册服务: {}", serviceName);
    }
    
    @Override
    public void start(int port) {
        this.port = port;
        if (running.compareAndSet(false, true)) {
            log.info("HTTP RPC 服务器启动，端口: {}", port);
        }
    }
    
    @Override
    public void shutdown() {
        if (running.compareAndSet(true, false)) {
            serviceRegistry.clear();
            log.info("HTTP RPC 服务器已关闭");
        }
    }
    
    @Override
    public boolean isRunning() {
        return running.get();
    }
    
    @Override
    public int getPort() {
        return port;
    }
    
    /**
     * 获取服务注册表
     */
    public ConcurrentHashMap<String, Object> getServiceRegistry() {
        return serviceRegistry;
    }
    
    /**
     * HTTP RPC 控制器
     * 作为内部类处理 HTTP 请求
     */
    @RestController
    @RequestMapping("/rpc")
    public static class HttpRpcController {
        
        private final HttpRpcServer rpcServer;
        
        public HttpRpcController(HttpRpcServer rpcServer) {
            this.rpcServer = rpcServer;
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
                Object serviceImpl = rpcServer.getServiceRegistry().get(request.getServiceName());
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
            String[] serviceNames = rpcServer.getServiceRegistry().keySet().toArray(new String[0]);
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
    
    /**
     * 自动配置 HTTP RPC 控制器
     */
    @Bean
    public HttpRpcController httpRpcController() {
        return new HttpRpcController(this);
    }
}
