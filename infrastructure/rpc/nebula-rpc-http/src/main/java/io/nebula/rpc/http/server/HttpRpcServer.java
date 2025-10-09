package io.nebula.rpc.http.server;

import io.nebula.rpc.core.server.RpcServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * HTTP RPC 服务器实现
 * 维护服务注册表,通过反射调用 RPC 服务
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
public class HttpRpcServer implements RpcServer, InitializingBean, DisposableBean {
    
    /**
     * 服务注册表: serviceName -> service instance
     */
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
        log.info("注册RPC服务: serviceName={}, serviceClass={}", serviceName, serviceClass.getName());
    }
    
    /**
     * 获取服务注册表
     */
    public ConcurrentHashMap<String, Object> getServiceRegistry() {
        return serviceRegistry;
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
    
}
