package io.nebula.rpc.http.processor;

import io.nebula.rpc.core.annotation.RpcClient;
import io.nebula.rpc.core.annotation.RpcService;
import io.nebula.rpc.http.server.HttpRpcServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

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
        
        // 获取RPC接口类（自动推导或手动指定）
        Class<?> serviceInterface = findServiceInterface(beanClass, rpcService);
        
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
    
    /**
     * 查找服务接口
     * 如果 @RpcService 没有指定接口，自动查找标注了 @RpcClient 的接口
     * 
     * @param beanClass 服务实现类
     * @param rpcService RpcService注解
     * @return 服务接口类
     */
    private Class<?> findServiceInterface(Class<?> beanClass, RpcService rpcService) {
        // 1. 如果手动指定了接口，直接使用
        Class<?> specifiedInterface = rpcService.value();
        if (specifiedInterface != null && specifiedInterface != void.class) {
            return specifiedInterface;
        }
        
        // 2. 自动查找标注了 @RpcClient 的接口
        Class<?>[] interfaces = beanClass.getInterfaces();
        List<Class<?>> rpcInterfaces = new ArrayList<>();
        
        for (Class<?> iface : interfaces) {
            if (iface.isAnnotationPresent(RpcClient.class)) {
                rpcInterfaces.add(iface);
            }
        }
        
        // 3. 验证结果
        if (rpcInterfaces.isEmpty()) {
            throw new IllegalStateException(String.format(
                "类 %s 没有实现任何标注了 @RpcClient 的接口，请在 @RpcService 中手动指定接口类",
                beanClass.getName()));
        }
        
        if (rpcInterfaces.size() > 1) {
            throw new IllegalStateException(String.format(
                "类 %s 实现了多个 @RpcClient 接口 %s，请在 @RpcService 中手动指定接口类",
                beanClass.getName(), rpcInterfaces));
        }
        
        log.info("自动推导 RPC 服务接口: {} -> {}", 
            beanClass.getSimpleName(), rpcInterfaces.get(0).getSimpleName());
        
        return rpcInterfaces.get(0);
    }
}

