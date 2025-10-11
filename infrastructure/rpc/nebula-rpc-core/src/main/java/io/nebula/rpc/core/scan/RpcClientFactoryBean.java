package io.nebula.rpc.core.scan;

import io.nebula.rpc.core.annotation.RpcCall;
import io.nebula.rpc.core.annotation.RpcClient;
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
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
public class RpcClientFactoryBean implements FactoryBean<Object>, 
        InitializingBean, ApplicationContextAware {
    
    private Class<?> type;
    private ApplicationContext applicationContext;
    private io.nebula.rpc.core.client.RpcClient rpcClient;
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
    
    @Override
    public void afterPropertiesSet() throws Exception {
        // 从容器中获取RpcClient实例
        // 优先使用 ServiceDiscoveryRpcClient, 如果不存在则使用 HttpRpcClient
        try {
            // 优先尝试获取 ServiceDiscoveryRpcClient
            try {
                this.rpcClient = (io.nebula.rpc.core.client.RpcClient) 
                        applicationContext.getBean("serviceDiscoveryRpcClient");
                log.debug("使用 ServiceDiscoveryRpcClient 创建 RPC 客户端代理");
                return;
            } catch (BeansException e) {
                log.debug("ServiceDiscoveryRpcClient 不可用: {}", e.getMessage());
            }
            
            // 其次尝试获取 HttpRpcClient
            try {
                this.rpcClient = (io.nebula.rpc.core.client.RpcClient) 
                        applicationContext.getBean("httpRpcClient");
                log.debug("使用 HttpRpcClient 创建 RPC 客户端代理");
                return;
            } catch (BeansException e) {
                log.debug("HttpRpcClient 不可用: {}", e.getMessage());
            }
            
            // 最后尝试按类型获取任意 RpcClient
            try {
                this.rpcClient = applicationContext.getBean(io.nebula.rpc.core.client.RpcClient.class);
                log.debug("使用默认 RpcClient 创建 RPC 客户端代理: {}", 
                        this.rpcClient.getClass().getSimpleName());
                return;
            } catch (BeansException e) {
                log.debug("未找到任何 RpcClient Bean: {}", e.getMessage());
            }
            
            log.warn("未找到任何可用的 RpcClient 实例，RPC 调用将失败");
        } catch (Exception e) {
            log.error("初始化 RpcClient 时发生未预期的异常", e);
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
    
    /**
     * 创建RPC客户端代理
     */
    private Object createProxy() {
        if (type == null) {
            throw new IllegalStateException("RPC客户端类型不能为null");
        }
        
        RpcClient annotation = type.getAnnotation(RpcClient.class);
        if (annotation == null) {
            throw new IllegalStateException("类 " + type.getName() + " 缺少 @RpcClient 注解");
        }
        
        // 如果存在RpcClient实例，使用其createProxy方法
        if (rpcClient != null) {
            log.info("使用RpcClient创建代理: {}", type.getName());
            return rpcClient.createProxy(type);
        }
        
        // 否则创建简单的动态代理
        log.info("创建简单动态代理: {}", type.getName());
        return Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                new RpcInvocationHandler(type, annotation)
        );
    }
    
    /**
     * RPC调用处理器
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
            
            // 获取方法上的 @RpcCall 注解
            RpcCall callAnnotation = method.getAnnotation(RpcCall.class);
            
            // 如果没有RpcClient实例，抛出异常
            if (rpcClient == null) {
                throw new IllegalStateException(
                        "未找到RpcClient实例，请确保已正确配置RPC客户端");
            }
            
            // 构建服务名称
            String serviceName = getServiceName();
            
            // 执行RPC调用
            log.debug("执行RPC调用: service={}, method={}", serviceName, method.getName());
            return rpcClient.call(interfaceClass, method.getName(), args);
        }
        
        /**
         * 获取服务名称
         */
        private String getServiceName() {
            if (StringUtils.hasText(clientAnnotation.name())) {
                return clientAnnotation.name();
            }
            if (StringUtils.hasText(clientAnnotation.value())) {
                return clientAnnotation.value();
            }
            // 使用接口全限定名作为服务名
            return interfaceClass.getName();
        }
    }
}

