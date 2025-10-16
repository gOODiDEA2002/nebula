package io.nebula.rpc.core.scan;

import io.nebula.rpc.core.annotation.EnableRpcClients;
import io.nebula.rpc.core.annotation.RpcClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * RPC客户端扫描注册器
 * 扫描带有 @RpcClient 注解的接口并注册为Spring Bean
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
public class RpcClientScannerRegistrar implements ImportBeanDefinitionRegistrar, 
        ResourceLoaderAware, EnvironmentAware {
    
    private ResourceLoader resourceLoader;
    private Environment environment;
    
    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
    
    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
    
    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        // 获取 @EnableRpcClients 注解的属性
        Map<String, Object> attrs = metadata.getAnnotationAttributes(
                EnableRpcClients.class.getName());
        
        if (attrs == null) {
            return;
        }
        
        // 获取默认服务名
        String defaultService = getDefaultService(attrs);
        
        // 获取要扫描的包路径
        Set<String> basePackages = getBasePackages(metadata, attrs);
        
        log.info("开始扫描RPC客户端，扫描包: {}, 默认服务: {}", basePackages, 
                StringUtils.hasText(defaultService) ? defaultService : "未指定");
        
        // 扫描并注册RPC客户端
        registerRpcClients(basePackages, registry, defaultService);
        
        // 注册指定的客户端
        registerSpecifiedClients(attrs, registry, defaultService);
    }
    
    /**
     * 获取默认服务名
     * 从 @EnableRpcClients 的 value 属性获取
     */
    private String getDefaultService(Map<String, Object> attrs) {
        String[] value = (String[]) attrs.get("value");
        if (value != null && value.length > 0) {
            // 如果value只有一个元素，且格式像服务名（不包含.号），则作为默认服务名
            if (value.length == 1 && !value[0].contains(".")) {
                return value[0];
            }
        }
        return "";
    }
    
    /**
     * 获取要扫描的基础包路径
     */
    private Set<String> getBasePackages(AnnotationMetadata metadata, Map<String, Object> attrs) {
        Set<String> basePackages = new HashSet<>();
        
        // 添加 value 属性指定的包（排除用作默认服务名的情况）
        String[] value = (String[]) attrs.get("value");
        if (value != null) {
            // 如果value看起来是服务名（单个元素且不包含.），则不作为包路径
            if (!(value.length == 1 && !value[0].contains("."))) {
                basePackages.addAll(Arrays.asList(value));
            }
        }
        
        // 添加 basePackages 属性指定的包
        String[] packages = (String[]) attrs.get("basePackages");
        if (packages != null) {
            basePackages.addAll(Arrays.asList(packages));
        }
        
        // 添加 basePackageClasses 属性指定的类所在的包
        Class<?>[] basePackageClasses = (Class<?>[]) attrs.get("basePackageClasses");
        if (basePackageClasses != null) {
            for (Class<?> clazz : basePackageClasses) {
                basePackages.add(ClassUtils.getPackageName(clazz));
            }
        }
        
        // 如果没有指定包，则使用标注类所在的包
        if (basePackages.isEmpty()) {
            basePackages.add(ClassUtils.getPackageName(metadata.getClassName()));
        }
        
        return basePackages;
    }
    
    /**
     * 扫描并注册RPC客户端
     */
    private void registerRpcClients(Set<String> basePackages, BeanDefinitionRegistry registry, String defaultService) {
        ClassPathScanningCandidateComponentProvider scanner = 
                new ClassPathScanningCandidateComponentProvider(false, environment) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                // 只扫描接口
                return beanDefinition.getMetadata().isInterface();
            }
        };
        
        if (resourceLoader != null) {
            scanner.setResourceLoader(resourceLoader);
        }
        
        // 添加 @RpcClient 注解过滤器
        scanner.addIncludeFilter(new AnnotationTypeFilter(RpcClient.class));
        
        for (String basePackage : basePackages) {
            Set<BeanDefinition> candidates = scanner.findCandidateComponents(basePackage);
            
            for (BeanDefinition candidate : candidates) {
                if (candidate instanceof AnnotatedBeanDefinition) {
                    registerRpcClient((AnnotatedBeanDefinition) candidate, registry, defaultService);
                }
            }
        }
    }
    
    /**
     * 注册指定的RPC客户端
     */
    private void registerSpecifiedClients(Map<String, Object> attrs, BeanDefinitionRegistry registry, String defaultService) {
        Class<?>[] clients = (Class<?>[]) attrs.get("clients");
        
        if (clients == null || clients.length == 0) {
            return;
        }
        
        for (Class<?> clientClass : clients) {
            // 验证是否为接口且带有 @RpcClient 注解
            if (!clientClass.isInterface()) {
                log.warn("类 {} 不是接口，跳过注册", clientClass.getName());
                continue;
            }
            
            if (!clientClass.isAnnotationPresent(RpcClient.class)) {
                log.warn("类 {} 没有 @RpcClient 注解，跳过注册", clientClass.getName());
                continue;
            }
            
            // 构建Bean定义
            BeanDefinitionBuilder builder = BeanDefinitionBuilder
                    .genericBeanDefinition(RpcClientFactoryBean.class);
            builder.addPropertyValue("type", clientClass);
            
            // 设置默认服务名
            if (StringUtils.hasText(defaultService)) {
                builder.addPropertyValue("name", defaultService);
            }
            
            String beanName = generateBeanName(clientClass);
            registry.registerBeanDefinition(beanName, builder.getBeanDefinition());
            
            log.info("注册RPC客户端: {} -> {}", clientClass.getName(), beanName);
        }
    }
    
    /**
     * 注册单个RPC客户端
     */
    private void registerRpcClient(AnnotatedBeanDefinition definition, BeanDefinitionRegistry registry, String defaultService) {
        String className = definition.getMetadata().getClassName();
        
        try {
            Class<?> clientClass = Class.forName(className);
            
            // 构建Bean定义
            BeanDefinitionBuilder builder = BeanDefinitionBuilder
                    .genericBeanDefinition(RpcClientFactoryBean.class);
            builder.addPropertyValue("type", clientClass);
            
            // 设置默认服务名
            if (StringUtils.hasText(defaultService)) {
                builder.addPropertyValue("name", defaultService);
            }
            
            String beanName = generateBeanName(clientClass);
            registry.registerBeanDefinition(beanName, builder.getBeanDefinition());
            
            log.info("注册RPC客户端: {} -> {} (默认服务: {})", className, beanName, 
                    StringUtils.hasText(defaultService) ? defaultService : "未指定");
            
        } catch (ClassNotFoundException e) {
            log.error("无法加载RPC客户端类: {}", className, e);
        }
    }
    
    /**
     * 生成Bean名称
     */
    private String generateBeanName(Class<?> clientClass) {
        RpcClient annotation = clientClass.getAnnotation(RpcClient.class);
        
        // 优先使用 contextId
        if (StringUtils.hasText(annotation.contextId())) {
            return annotation.contextId();
        }
        
        // 使用类名（首字母小写）
        String className = clientClass.getSimpleName();
        return Character.toLowerCase(className.charAt(0)) + className.substring(1);
    }
}

