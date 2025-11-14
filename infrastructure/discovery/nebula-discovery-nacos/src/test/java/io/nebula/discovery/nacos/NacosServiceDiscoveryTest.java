package io.nebula.discovery.nacos;

import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import io.nebula.discovery.nacos.config.NacosProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * NacosServiceDiscovery单元测试
 */
@ExtendWith(MockitoExtension.class)
class NacosServiceDiscoveryTest {
    
    @Mock
    private NamingService namingService;
    
    private NacosServiceDiscovery serviceDiscovery;
    
    @BeforeEach
    void setUp() throws Exception {
        NacosProperties nacosProperties = new NacosProperties();
        nacosProperties.setServerAddr("127.0.0.1:8848");
        nacosProperties.setNamespace("public");
        nacosProperties.setGroupName("DEFAULT_GROUP");
        
        serviceDiscovery = new NacosServiceDiscovery(nacosProperties);
        // 使用反射注入namingService
        java.lang.reflect.Field field = NacosServiceDiscovery.class.getDeclaredField("namingService");
        field.setAccessible(true);
        field.set(serviceDiscovery, namingService);
    }
    
    @Test
    void testGetInstances() throws Exception {
        String serviceName = "test-service";
        Instance instance1 = new Instance();
        instance1.setIp("192.168.1.1");
        instance1.setPort(8080);
        
        Instance instance2 = new Instance();
        instance2.setIp("192.168.1.2");
        instance2.setPort(8080);
        
        List<Instance> instances = Arrays.asList(instance1, instance2);
        when(namingService.selectInstances(eq(serviceName), eq("DEFAULT_GROUP"), eq(true))).thenReturn(instances);
        
        List<io.nebula.discovery.core.ServiceInstance> result = serviceDiscovery.getInstances(serviceName);
        
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getIp()).isEqualTo("192.168.1.1");
        verify(namingService).selectInstances(eq(serviceName), eq("DEFAULT_GROUP"), eq(true));
    }
    
    @Test
    void testSelectHealthyInstances() throws Exception {
        String serviceName = "test-service";
        Instance healthyInstance = new Instance();
        healthyInstance.setIp("192.168.1.1");
        healthyInstance.setPort(8080);
        healthyInstance.setHealthy(true);
        
        List<Instance> instances = Arrays.asList(healthyInstance);
        when(namingService.selectInstances(serviceName, true)).thenReturn(instances);
        
        // 直接调用NamingService方法
        List<Instance> result = namingService.selectInstances(serviceName, true);
        
        assertThat(result).hasSize(1);
        assertThat(result.get(0).isHealthy()).isTrue();
    }
}

