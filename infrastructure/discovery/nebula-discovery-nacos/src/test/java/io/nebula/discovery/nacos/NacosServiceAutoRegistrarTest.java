package io.nebula.discovery.nacos;

import io.nebula.discovery.core.ServiceDiscovery;
import io.nebula.discovery.core.ServiceInstance;
import io.nebula.discovery.nacos.config.NacosProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.servlet.context.ServletWebServerInitializedEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.env.Environment;
import org.springframework.mock.web.MockServletContext;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Nacos服务自动注册器测试
 * 
 * 测试目的: 验证服务自动注册和注销功能
 */
@ExtendWith(MockitoExtension.class)
class NacosServiceAutoRegistrarTest {
    
    @Mock
    private ServiceDiscovery serviceDiscovery;
    
    @Mock
    private Environment environment;
    
    private NacosProperties nacosProperties;
    private NacosServiceAutoRegistrar autoRegistrar;
    
    @BeforeEach
    void setUp() {
        nacosProperties = new NacosProperties();
        nacosProperties.setAutoRegister(true);
        nacosProperties.setGroupName("DEFAULT_GROUP");
        nacosProperties.setClusterName("DEFAULT");
        nacosProperties.setWeight(1.0);
        nacosProperties.setHealthy(true);
        nacosProperties.setInstanceEnabled(true);
        
        // 设置Environment的mock行为
        lenient().when(environment.getProperty("spring.application.name", "unknown-service"))
                .thenReturn("test-service");
        lenient().when(environment.getProperty("spring.application.version", "1.0.0"))
                .thenReturn("1.0.0");
        lenient().when(environment.getActiveProfiles())
                .thenReturn(new String[]{"dev"});
        lenient().when(environment.getProperty("grpc.server.port"))
                .thenReturn(null);
        
        autoRegistrar = new NacosServiceAutoRegistrar(serviceDiscovery, nacosProperties, environment);
    }
    
    @Test
    void testAutoRegistration() throws Exception {
        // 使用反射调用私有方法registerService
        int port = 8080;
        java.lang.reflect.Method method = NacosServiceAutoRegistrar.class.getDeclaredMethod("registerService", int.class);
        method.setAccessible(true);
        method.invoke(autoRegistrar, port);
        
        // 验证服务注册被调用
        ArgumentCaptor<ServiceInstance> captor = ArgumentCaptor.forClass(ServiceInstance.class);
        verify(serviceDiscovery).register(captor.capture());
        
        // 验证ServiceInstance属性
        ServiceInstance instance = captor.getValue();
        assertThat(instance.getServiceName()).isEqualTo("test-service");
        assertThat(instance.getPort()).isEqualTo(port);
        assertThat(instance.getGroupName()).isEqualTo("DEFAULT_GROUP");
        assertThat(instance.getClusterName()).isEqualTo("DEFAULT");
        assertThat(instance.getWeight()).isEqualTo(1.0);
        assertThat(instance.isHealthy()).isTrue();
        assertThat(instance.isEnabled()).isTrue();
        assertThat(instance.getProtocol()).isEqualTo("http");
        
        // 验证元数据
        assertThat(instance.getMetadata())
                .containsKey("startTime")
                .containsEntry("version", "1.0.0")
                .containsEntry("profile", "dev");
    }
    
    @Test
    void testAutoRegistrationDisabled() throws Exception {
        // 禁用自动注册
        nacosProperties.setAutoRegister(false);
        autoRegistrar = new NacosServiceAutoRegistrar(serviceDiscovery, nacosProperties, environment);
        
        // 使用反射调用registerService
        java.lang.reflect.Method method = NacosServiceAutoRegistrar.class.getDeclaredMethod("registerService", int.class);
        method.setAccessible(true);
        method.invoke(autoRegistrar, 8080);
        
        // 验证服务注册未被调用
        verify(serviceDiscovery, never()).register(any());
    }
    
    @Test
    void testAutoRegistrationWithGrpcPort() throws Exception {
        // 设置gRPC端口
        when(environment.getProperty("grpc.server.port")).thenReturn("9090");
        
        // 使用反射调用registerService
        java.lang.reflect.Method method = NacosServiceAutoRegistrar.class.getDeclaredMethod("registerService", int.class);
        method.setAccessible(true);
        method.invoke(autoRegistrar, 8080);
        
        // 验证服务注册被调用
        ArgumentCaptor<ServiceInstance> captor = ArgumentCaptor.forClass(ServiceInstance.class);
        verify(serviceDiscovery).register(captor.capture());
        
        // 验证元数据包含gRPC端口
        ServiceInstance instance = captor.getValue();
        assertThat(instance.getMetadata()).containsEntry("grpc.port", "9090");
    }
    
    @Test
    void testAutoRegistrationWithCustomMetadata() throws Exception {
        // 设置自定义元数据
        java.util.Map<String, String> customMetadata = new java.util.HashMap<>();
        customMetadata.put("region", "cn-east");
        customMetadata.put("zone", "zone-a");
        nacosProperties.setMetadata(customMetadata);
        
        autoRegistrar = new NacosServiceAutoRegistrar(serviceDiscovery, nacosProperties, environment);
        
        // 使用反射调用registerService
        java.lang.reflect.Method method = NacosServiceAutoRegistrar.class.getDeclaredMethod("registerService", int.class);
        method.setAccessible(true);
        method.invoke(autoRegistrar, 8080);
        
        // 验证服务注册被调用
        ArgumentCaptor<ServiceInstance> captor = ArgumentCaptor.forClass(ServiceInstance.class);
        verify(serviceDiscovery).register(captor.capture());
        
        // 验证元数据包含自定义字段
        ServiceInstance instance = captor.getValue();
        assertThat(instance.getMetadata())
                .containsEntry("region", "cn-east")
                .containsEntry("zone", "zone-a");
    }
    
    @Test
    void testAutoDeregistration() throws Exception {
        // 先调用注册
        java.lang.reflect.Method registerMethod = NacosServiceAutoRegistrar.class.getDeclaredMethod("registerService", int.class);
        registerMethod.setAccessible(true);
        registerMethod.invoke(autoRegistrar, 8080);
        
        // 验证注册成功
        verify(serviceDiscovery).register(any());
        
        // 重置mock以便验证注销调用
        reset(serviceDiscovery);
        
        // 调用注销方法
        autoRegistrar.deregisterService();
        
        // 验证服务注销被调用
        verify(serviceDiscovery).deregister(eq("test-service"), anyString());
    }
    
    @Test
    void testDeregistrationNotCalledWhenNotRegistered() throws Exception {
        // 不触发注册，直接调用注销
        autoRegistrar.deregisterService();
        
        // 验证服务注销未被调用（因为从未注册过）
        verify(serviceDiscovery, never()).deregister(anyString(), anyString());
    }
    
    @Test
    void testDeregistrationWithNacosServiceDiscovery() throws Exception {
        // 使用NacosServiceDiscovery实例
        NacosServiceDiscovery nacosServiceDiscovery = mock(NacosServiceDiscovery.class);
        autoRegistrar = new NacosServiceAutoRegistrar(nacosServiceDiscovery, nacosProperties, environment);
        
        // 先调用注册
        java.lang.reflect.Method registerMethod = NacosServiceAutoRegistrar.class.getDeclaredMethod("registerService", int.class);
        registerMethod.setAccessible(true);
        registerMethod.invoke(autoRegistrar, 8080);
        
        // 验证注册成功
        verify(nacosServiceDiscovery).register(any());
        
        // 调用注销方法
        autoRegistrar.deregisterService();
        
        // 验证使用了deregisterInstance方法（NacosServiceDiscovery特有的方法）
        verify(nacosServiceDiscovery).deregisterInstance(eq("test-service"), anyString(), eq(8080));
    }
    
    @Test
    void testPreDestroyDeregistration() throws Exception {
        // 先调用注册
        java.lang.reflect.Method registerMethod = NacosServiceAutoRegistrar.class.getDeclaredMethod("registerService", int.class);
        registerMethod.setAccessible(true);
        registerMethod.invoke(autoRegistrar, 8080);
        
        // 验证注册成功
        verify(serviceDiscovery).register(any());
        
        // 重置mock
        reset(serviceDiscovery);
        
        // 调用@PreDestroy方法
        autoRegistrar.deregisterService();
        
        // 验证服务注销被调用
        verify(serviceDiscovery).deregister(anyString(), anyString());
    }
    
    @Test
    void testRegistrationException() throws Exception {
        // 模拟注册抛出异常
        doThrow(new RuntimeException("Registration failed")).when(serviceDiscovery).register(any());
        
        // 使用反射调用registerService应该抛出异常
        java.lang.reflect.Method method = NacosServiceAutoRegistrar.class.getDeclaredMethod("registerService", int.class);
        method.setAccessible(true);
        
        assertThatThrownBy(() -> {
            try {
                method.invoke(autoRegistrar, 8080);
            } catch (java.lang.reflect.InvocationTargetException e) {
                throw e.getCause();
            }
        })
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("服务自动注册失败");
    }
    
    @Test
    void testInstanceIdGeneration() throws Exception {
        // 使用反射调用registerService
        java.lang.reflect.Method method = NacosServiceAutoRegistrar.class.getDeclaredMethod("registerService", int.class);
        method.setAccessible(true);
        method.invoke(autoRegistrar, 8080);
        
        // 验证服务注册被调用
        ArgumentCaptor<ServiceInstance> captor = ArgumentCaptor.forClass(ServiceInstance.class);
        verify(serviceDiscovery).register(captor.capture());
        
        // 验证instanceId格式：serviceName:ip:port
        ServiceInstance instance = captor.getValue();
        assertThat(instance.getInstanceId())
                .startsWith("test-service:")
                .contains(":")
                .endsWith(":8080");
    }
}

