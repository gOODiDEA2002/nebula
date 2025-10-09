package io.nebula.rpc.http.processor;

import io.nebula.rpc.core.annotation.RpcService;
import io.nebula.rpc.http.server.HttpRpcServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.util.StringUtils;

/**
 * RPC服务注册处理器
 * 自动扫描并注册所有带有@RpcService注解的Bean到HttpRpcServer
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
public class RpcServiceRegistrationProcessor implements BeanPostProcessor {
    
    private final HttpRpcServer rpcServer;
    
    public RpcServiceRegistrationProcessor(HttpRpcServer rpcServer) {
        this.rpcServer = rpcServer;
    }
    
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> beanClass = bean.getClass();
        
        // 检查是否有 @RpcService 注解
        RpcService rpcService = beanClass.getAnnotation(RpcService.class);
        if (rpcService == null) {
            return bean;
        }
        
        // 获取RPC接口类
        Class<?> serviceInterface = rpcService.value();
        
        // 确定服务名称
        String serviceName;
        if (StringUtils.hasText(rpcService.serviceName())) {
            serviceName = rpcService.serviceName();
        } else {
            serviceName = serviceInterface.getName();
        }
        
        // 注册服务 - 使用强制类型转换避免泛型问题
        @SuppressWarnings("unchecked")
        Class<Object> interfaceClass = (Class<Object>) serviceInterface;
        rpcServer.registerService(serviceName, interfaceClass, bean);
        log.info("自动注册RPC服务: serviceName={}, interface={}, implementation={}", 
                serviceName, serviceInterface.getSimpleName(), beanClass.getSimpleName());
        
        return bean;
    }
}

