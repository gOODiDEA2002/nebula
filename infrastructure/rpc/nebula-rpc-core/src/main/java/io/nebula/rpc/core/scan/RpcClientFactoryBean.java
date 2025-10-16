package io.nebula.rpc.core.scan;

import io.nebula.rpc.core.annotation.RpcCall;
import io.nebula.rpc.core.annotation.RpcClient;
import io.nebula.rpc.core.context.RpcContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.StringUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * RPC客户端工厂Bean
 * 创建RPC客户端的代理对象
 * 
 * 采用延迟加载策略，在第一次实际调用时才查找RpcClient Bean，
 * 避免Bean初始化顺序问题
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
public class RpcClientFactoryBean implements FactoryBean<Object>, ApplicationContextAware {
    
    private Class<?> type;
    private String name;  // 外部注入的服务名
    private ApplicationContext applicationContext;
    
    /**
     * RPC客户端实例，采用延迟初始化
     * volatile 保证多线程可见性
     */
    private volatile io.nebula.rpc.core.client.RpcClient rpcClient;
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
    
    /**
     * 延迟查找RpcClient实例
     * 使用双重检查锁定（DCL）确保线程安全
     * 
     * @return RpcClient实例，如果未找到返回null
     */
    private io.nebula.rpc.core.client.RpcClient getRpcClient() {
        if (rpcClient == null) {
            synchronized (this) {
                if (rpcClient == null) {
                    rpcClient = findRpcClientBean();
                }
            }
        }
        return rpcClient;
    }
    
    /**
     * 从ApplicationContext中查找RpcClient Bean
     * 优先级：ServiceDiscoveryRpcClient > HttpRpcClient > 按类型查找
     * 
     * @return RpcClient实例，如果未找到返回null
     */
    private io.nebula.rpc.core.client.RpcClient findRpcClientBean() {
        try {
            // 优先尝试获取 ServiceDiscoveryRpcClient
            try {
                io.nebula.rpc.core.client.RpcClient client = (io.nebula.rpc.core.client.RpcClient) 
                        applicationContext.getBean("serviceDiscoveryRpcClient");
                log.debug("使用 ServiceDiscoveryRpcClient 创建 RPC 客户端代理");
                return client;
            } catch (BeansException e) {
                log.debug("ServiceDiscoveryRpcClient 不可用: {}", e.getMessage());
            }
            
            // 其次尝试获取 HttpRpcClient
            try {
                io.nebula.rpc.core.client.RpcClient client = (io.nebula.rpc.core.client.RpcClient) 
                        applicationContext.getBean("httpRpcClient");
                log.debug("使用 HttpRpcClient 创建 RPC 客户端代理");
                return client;
            } catch (BeansException e) {
                log.debug("HttpRpcClient 不可用: {}", e.getMessage());
            }
            
            // 最后尝试按类型获取任意 RpcClient
            try {
                io.nebula.rpc.core.client.RpcClient client = 
                        applicationContext.getBean(io.nebula.rpc.core.client.RpcClient.class);
                log.debug("使用默认 RpcClient 创建 RPC 客户端代理: {}", 
                        client.getClass().getSimpleName());
                return client;
            } catch (BeansException e) {
                log.debug("未找到任何 RpcClient Bean: {}", e.getMessage());
            }
            
            log.warn("未找到任何可用的 RpcClient 实例，RPC 调用将使用简单代理（调用时会失败）");
            return null;
        } catch (Exception e) {
            log.error("查找 RpcClient 时发生未预期的异常", e);
            return null;
        }
    }
    
    @Override
    public Object getObject() throws Exception {
        return createProxy();
    }
    
    @Override
    public Class<?> getObjectType() {
        return type;
    }
    
    @Override
    public boolean isSingleton() {
        return true;
    }
    
    public void setType(Class<?> type) {
        this.type = type;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * 创建RPC客户端代理
     * 始终创建动态代理，延迟到实际调用时才查找RpcClient实例
     */
    private Object createProxy() {
        if (type == null) {
            throw new IllegalStateException("RPC客户端类型不能为null");
        }
        
        RpcClient annotation = type.getAnnotation(RpcClient.class);
        if (annotation == null) {
            throw new IllegalStateException("类 " + type.getName() + " 缺少 @RpcClient 注解");
        }
        
        // 创建动态代理，延迟查找RpcClient实例
        log.debug("创建 RPC 客户端代理: {}", type.getName());
        return Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                new RpcInvocationHandler(type, annotation)
        );
    }
    
    /**
     * RPC调用处理器
     * 在实际调用时才获取RpcClient实例，实现真正的延迟加载
     */
    private class RpcInvocationHandler implements InvocationHandler {
        
        private final Class<?> interfaceClass;
        private final RpcClient clientAnnotation;
        
        public RpcInvocationHandler(Class<?> interfaceClass, RpcClient clientAnnotation) {
            this.interfaceClass = interfaceClass;
            this.clientAnnotation = clientAnnotation;
        }
        
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // 处理Object类的方法
            if (method.getDeclaringClass() == Object.class) {
                return method.invoke(this, args);
            }
            
            // 延迟获取RpcClient实例（此时所有Bean都已初始化）
            io.nebula.rpc.core.client.RpcClient client = getRpcClient();
            
            // 如果没有RpcClient实例，抛出异常
            if (client == null) {
                throw new IllegalStateException(
                        "未找到RpcClient实例，请确保已正确配置RPC客户端（nebula.rpc.http.enabled 或 nebula.rpc.grpc.enabled）");
            }
            
            // 构建服务名称
            String serviceName = getServiceName();
            
            // 执行RPC调用
            log.debug("执行RPC调用: service={}, method={}", serviceName, method.getName());
            
            // 设置服务名到 ThreadLocal，供 ServiceDiscoveryRpcClient 使用
            try {
                if (StringUtils.hasText(serviceName)) {
                    RpcContextHolder.setServiceName(serviceName);
                }
                return client.call(interfaceClass, method.getName(), args);
            } finally {
                // 清理 ThreadLocal
                RpcContextHolder.clear();
            }
        }
        
        /**
         * 获取服务名称
         */
        private String getServiceName() {
            // 优先使用外部注入的服务名
            if (StringUtils.hasText(RpcClientFactoryBean.this.name)) {
                return RpcClientFactoryBean.this.name;
            }
            // 其次使用注解中的服务名
            if (StringUtils.hasText(clientAnnotation.name())) {
                return clientAnnotation.name();
            }
            if (StringUtils.hasText(clientAnnotation.value())) {
                return clientAnnotation.value();
            }
            // 最后使用接口全限定名作为服务名
            return interfaceClass.getName();
        }
    }
}

