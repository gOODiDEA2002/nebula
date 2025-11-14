package io.nebula.discovery.nacos;

import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import io.nebula.discovery.core.ServiceInstance;
import io.nebula.discovery.nacos.config.NacosProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Nacos服务注册功能测试
 * 
 * 测试目的: 验证服务注册到Nacos的功能
 */
@ExtendWith(MockitoExtension.class)
class NacosServiceRegistryTest {
    
    @Mock
    private NamingService namingService;
    
    private NacosProperties nacosProperties;
    private NacosServiceDiscovery serviceDiscovery;
    
    @BeforeEach
    void setUp() throws Exception {
        nacosProperties = new NacosProperties();
        nacosProperties.setServerAddr("127.0.0.1:8848");
        nacosProperties.setNamespace("public");
        nacosProperties.setGroupName("DEFAULT_GROUP");
        nacosProperties.setClusterName("DEFAULT");
        nacosProperties.setWeight(1.0);
        nacosProperties.setHealthy(true);
        nacosProperties.setInstanceEnabled(true);
        
        // 使用构造函数注入，避免调用afterPropertiesSet
        serviceDiscovery = new NacosServiceDiscovery(nacosProperties);
        // 通过反射设置namingService
        java.lang.reflect.Field field = NacosServiceDiscovery.class.getDeclaredField("namingService");
        field.setAccessible(true);
        field.set(serviceDiscovery, namingService);
    }
    
    @Test
    void testRegister() throws Exception {
        // 准备测试数据
        ServiceInstance instance = ServiceInstance.builder()
                .serviceName("test-service")
                .instanceId("test-instance-1")
                .ip("192.168.1.100")
                .port(8080)
                .weight(1.0)
                .healthy(true)
                .enabled(true)
                .clusterName("DEFAULT")
                .metadata(Map.of("version", "1.0.0"))
                .build();
        
        // 执行注册
        serviceDiscovery.register(instance);
        
        // 验证调用
        ArgumentCaptor<Instance> captor = ArgumentCaptor.forClass(Instance.class);
        verify(namingService).registerInstance(
                eq("test-service"),
                eq("DEFAULT_GROUP"),
                captor.capture()
        );
        
        // 验证Instance属性
        Instance capturedInstance = captor.getValue();
        assertThat(capturedInstance.getIp()).isEqualTo("192.168.1.100");
        assertThat(capturedInstance.getPort()).isEqualTo(8080);
        assertThat(capturedInstance.getWeight()).isEqualTo(1.0);
        assertThat(capturedInstance.isHealthy()).isTrue();
        assertThat(capturedInstance.isEnabled()).isTrue();
        assertThat(capturedInstance.getMetadata()).containsEntry("version", "1.0.0");
    }
    
    @Test
    void testRegisterWithMetadata() throws Exception {
        // 准备带元数据的服务实例
        Map<String, String> metadata = new HashMap<>();
        metadata.put("version", "1.0.0");
        metadata.put("region", "cn-east");
        metadata.put("zone", "zone-a");
        
        ServiceInstance instance = ServiceInstance.builder()
                .serviceName("test-service")
                .instanceId("test-instance-2")
                .ip("192.168.1.101")
                .port(8081)
                .metadata(metadata)
                .build();
        
        // 执行注册
        serviceDiscovery.register(instance);
        
        // 验证元数据正确传递
        ArgumentCaptor<Instance> captor = ArgumentCaptor.forClass(Instance.class);
        verify(namingService).registerInstance(anyString(), anyString(), captor.capture());
        
        Instance capturedInstance = captor.getValue();
        assertThat(capturedInstance.getMetadata())
                .containsEntry("version", "1.0.0")
                .containsEntry("region", "cn-east")
                .containsEntry("zone", "zone-a");
    }
    
    @Test
    void testRegisterWithGroupName() throws Exception {
        // 准备带自定义分组的服务实例
        ServiceInstance instance = ServiceInstance.builder()
                .serviceName("test-service")
                .instanceId("test-instance-3")
                .ip("192.168.1.102")
                .port(8082)
                .groupName("CUSTOM_GROUP")
                .build();
        
        // 执行注册
        serviceDiscovery.register(instance);
        
        // 验证使用自定义分组
        verify(namingService).registerInstance(
                eq("test-service"),
                eq("CUSTOM_GROUP"),
                any(Instance.class)
        );
    }
    
    @Test
    void testRegisterWithNamespace() throws Exception {
        // 修改namespace配置
        nacosProperties.setNamespace("dev");
        
        ServiceInstance instance = ServiceInstance.builder()
                .serviceName("test-service")
                .instanceId("test-instance-4")
                .ip("192.168.1.103")
                .port(8083)
                .build();
        
        // 执行注册
        serviceDiscovery.register(instance);
        
        // 验证注册调用
        verify(namingService).registerInstance(
                eq("test-service"),
                anyString(),
                any(Instance.class)
        );
    }
    
    @Test
    void testRegisterInstanceDirectly() throws Exception {
        // 测试直接注册实例方法（兼容方法）
        String serviceName = "direct-service";
        String ip = "192.168.1.104";
        int port = 8084;
        
        // 执行直接注册
        serviceDiscovery.registerInstance(serviceName, ip, port);
        
        // 验证调用
        ArgumentCaptor<Instance> captor = ArgumentCaptor.forClass(Instance.class);
        verify(namingService).registerInstance(
                eq(serviceName),
                eq("DEFAULT_GROUP"),
                captor.capture()
        );
        
        // 验证Instance属性
        Instance capturedInstance = captor.getValue();
        assertThat(capturedInstance.getIp()).isEqualTo(ip);
        assertThat(capturedInstance.getPort()).isEqualTo(port);
        assertThat(capturedInstance.getClusterName()).isEqualTo("DEFAULT");
    }
    
    @Test
    void testRegisterWithCluster() throws Exception {
        // 测试指定集群注册
        String serviceName = "cluster-service";
        String ip = "192.168.1.105";
        int port = 8085;
        String clusterName = "BEIJING";
        
        // 执行注册
        serviceDiscovery.registerInstance(serviceName, ip, port, clusterName);
        
        // 验证调用
        ArgumentCaptor<Instance> captor = ArgumentCaptor.forClass(Instance.class);
        verify(namingService).registerInstance(
                eq(serviceName),
                eq("DEFAULT_GROUP"),
                captor.capture()
        );
        
        // 验证集群名
        Instance capturedInstance = captor.getValue();
        assertThat(capturedInstance.getClusterName()).isEqualTo(clusterName);
    }
    
    @Test
    void testDeregister() throws Exception {
        // 先准备一个已注册的服务实例
        String serviceName = "test-service";
        String instanceId = "test-instance-1";
        String ip = "192.168.1.100";
        int port = 8080;
        
        // 模拟获取实例列表，返回包含要注销的实例
        com.alibaba.nacos.api.naming.pojo.Instance nacosInstance = new com.alibaba.nacos.api.naming.pojo.Instance();
        nacosInstance.setServiceName(serviceName);
        nacosInstance.setInstanceId(instanceId);
        nacosInstance.setIp(ip);
        nacosInstance.setPort(port);
        
        when(namingService.selectInstances(eq(serviceName), eq("DEFAULT_GROUP"), eq(true)))
                .thenReturn(java.util.Collections.singletonList(nacosInstance));
        
        // 先调用getInstances触发缓存
        serviceDiscovery.getInstances(serviceName);
        
        // 执行注销
        serviceDiscovery.deregister(serviceName, instanceId);
        
        // 验证注销调用
        verify(namingService).deregisterInstance(
                eq(serviceName),
                eq("DEFAULT_GROUP"),
                eq(ip),
                eq(port)
        );
    }
    
    @Test
    void testDeregisterInstanceDirectly() throws Exception {
        // 测试直接注销实例方法
        String serviceName = "test-service";
        String ip = "192.168.1.100";
        int port = 8080;
        
        // 执行注销
        serviceDiscovery.deregisterInstance(serviceName, ip, port);
        
        // 验证调用
        verify(namingService).deregisterInstance(
                eq(serviceName),
                eq("DEFAULT_GROUP"),
                eq(ip),
                eq(port)
        );
    }
}

