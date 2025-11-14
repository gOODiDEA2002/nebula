package io.nebula.discovery.nacos;

import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import io.nebula.discovery.core.ServiceInstance;
import io.nebula.discovery.nacos.config.NacosProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Nacos服务订阅功能测试
 * 
 * 测试目的: 验证服务订阅（监听服务变化）功能
 */
@ExtendWith(MockitoExtension.class)
class NacosServiceSubscriptionTest {
    
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
        
        // 创建ServiceDiscovery并注入namingService
        serviceDiscovery = new NacosServiceDiscovery(nacosProperties);
        java.lang.reflect.Field field = NacosServiceDiscovery.class.getDeclaredField("namingService");
        field.setAccessible(true);
        field.set(serviceDiscovery, namingService);
    }
    
    @Test
    void testSubscribe() throws Exception {
        String serviceName = "test-service";
        
        // 执行订阅
        serviceDiscovery.subscribe(serviceName, (name, instances) -> {
            // 监听器回调
        });
        
        // 验证订阅方法被调用
        verify(namingService).subscribe(
                eq(serviceName),
                eq("DEFAULT_GROUP"),
                any(EventListener.class)
        );
    }
    
    @Test
    void testSubscribeWithCustomGroup() throws Exception {
        String serviceName = "test-service";
        String groupName = "CUSTOM_GROUP";
        
        // 执行订阅
        serviceDiscovery.subscribe(serviceName, groupName, (name, instances) -> {
            // 监听器回调
        });
        
        // 验证订阅方法被调用
        verify(namingService).subscribe(
                eq(serviceName),
                eq(groupName),
                any(EventListener.class)
        );
    }
    
    @Test
    void testServiceChangeNotification() throws Exception {
        String serviceName = "test-service";
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> notifiedServiceName = new AtomicReference<>();
        AtomicReference<List<ServiceInstance>> notifiedInstances = new AtomicReference<>();
        
        // 捕获EventListener
        ArgumentCaptor<EventListener> listenerCaptor = ArgumentCaptor.forClass(EventListener.class);
        
        // 执行订阅
        serviceDiscovery.subscribe(serviceName, (name, instances) -> {
            notifiedServiceName.set(name);
            notifiedInstances.set(instances);
            latch.countDown();
        });
        
        // 获取注册的EventListener
        verify(namingService).subscribe(anyString(), anyString(), listenerCaptor.capture());
        EventListener eventListener = listenerCaptor.getValue();
        
        // 创建模拟的NamingEvent
        Instance instance1 = new Instance();
        instance1.setServiceName(serviceName);
        instance1.setIp("192.168.1.100");
        instance1.setPort(8080);
        instance1.setHealthy(true);
        
        Instance instance2 = new Instance();
        instance2.setServiceName(serviceName);
        instance2.setIp("192.168.1.101");
        instance2.setPort(8080);
        instance2.setHealthy(true);
        
        List<Instance> instances = Arrays.asList(instance1, instance2);
        NamingEvent event = new NamingEvent(serviceName, instances);
        
        // 触发事件
        eventListener.onEvent(event);
        
        // 等待通知
        boolean received = latch.await(1, TimeUnit.SECONDS);
        
        // 验证通知
        assertThat(received).isTrue();
        assertThat(notifiedServiceName.get()).isEqualTo(serviceName);
        assertThat(notifiedInstances.get()).hasSize(2);
        assertThat(notifiedInstances.get().get(0).getIp()).isEqualTo("192.168.1.100");
        assertThat(notifiedInstances.get().get(1).getIp()).isEqualTo("192.168.1.101");
    }
    
    @Test
    void testUnsubscribe() throws Exception {
        String serviceName = "test-service";
        
        // 先订阅
        serviceDiscovery.subscribe(serviceName, (name, instances) -> {});
        
        // 捕获EventListener
        ArgumentCaptor<EventListener> listenerCaptor = ArgumentCaptor.forClass(EventListener.class);
        verify(namingService).subscribe(anyString(), anyString(), listenerCaptor.capture());
        EventListener registeredListener = listenerCaptor.getValue();
        
        // 执行取消订阅
        serviceDiscovery.unsubscribe(serviceName);
        
        // 验证取消订阅方法被调用
        verify(namingService).unsubscribe(
                eq(serviceName),
                eq("DEFAULT_GROUP"),
                eq(registeredListener)
        );
    }
    
    @Test
    void testUnsubscribeWithCustomGroup() throws Exception {
        String serviceName = "test-service";
        String groupName = "CUSTOM_GROUP";
        
        // 先订阅
        serviceDiscovery.subscribe(serviceName, groupName, (name, instances) -> {});
        
        // 捕获EventListener
        ArgumentCaptor<EventListener> listenerCaptor = ArgumentCaptor.forClass(EventListener.class);
        verify(namingService).subscribe(anyString(), anyString(), listenerCaptor.capture());
        EventListener registeredListener = listenerCaptor.getValue();
        
        // 执行取消订阅
        serviceDiscovery.unsubscribe(serviceName, groupName);
        
        // 验证取消订阅方法被调用
        verify(namingService).unsubscribe(
                eq(serviceName),
                eq(groupName),
                eq(registeredListener)
        );
    }
    
    @Test
    void testMultipleSubscriptions() throws Exception {
        String serviceName1 = "service-1";
        String serviceName2 = "service-2";
        
        // 订阅多个服务
        serviceDiscovery.subscribe(serviceName1, (name, instances) -> {});
        serviceDiscovery.subscribe(serviceName2, (name, instances) -> {});
        
        // 验证两个订阅都被调用
        verify(namingService).subscribe(eq(serviceName1), anyString(), any(EventListener.class));
        verify(namingService).subscribe(eq(serviceName2), anyString(), any(EventListener.class));
    }
    
    @Test
    void testSubscriptionCacheUpdate() throws Exception {
        String serviceName = "test-service";
        
        // 捕获EventListener
        ArgumentCaptor<EventListener> listenerCaptor = ArgumentCaptor.forClass(EventListener.class);
        
        // 执行订阅
        serviceDiscovery.subscribe(serviceName, (name, instances) -> {});
        
        // 获取注册的EventListener
        verify(namingService).subscribe(anyString(), anyString(), listenerCaptor.capture());
        EventListener eventListener = listenerCaptor.getValue();
        
        // 创建模拟的NamingEvent
        Instance instance1 = new Instance();
        instance1.setServiceName(serviceName);
        instance1.setIp("192.168.1.100");
        instance1.setPort(8080);
        
        NamingEvent event = new NamingEvent(serviceName, Arrays.asList(instance1));
        
        // 触发事件（应该更新缓存）
        eventListener.onEvent(event);
        
        // 验证缓存被更新（通过获取实例列表间接验证）
        // 这里主要验证事件处理不会抛出异常
        assertThatCode(() -> eventListener.onEvent(event)).doesNotThrowAnyException();
    }
    
    @Test
    void testSubscribeWithEmptyServiceList() throws Exception {
        String serviceName = "empty-service";
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<List<ServiceInstance>> notifiedInstances = new AtomicReference<>();
        
        // 捕获EventListener
        ArgumentCaptor<EventListener> listenerCaptor = ArgumentCaptor.forClass(EventListener.class);
        
        // 执行订阅
        serviceDiscovery.subscribe(serviceName, (name, instances) -> {
            notifiedInstances.set(instances);
            latch.countDown();
        });
        
        // 获取注册的EventListener
        verify(namingService).subscribe(anyString(), anyString(), listenerCaptor.capture());
        EventListener eventListener = listenerCaptor.getValue();
        
        // 创建空的NamingEvent
        NamingEvent event = new NamingEvent(serviceName, Arrays.asList());
        
        // 触发事件
        eventListener.onEvent(event);
        
        // 等待通知
        boolean received = latch.await(1, TimeUnit.SECONDS);
        
        // 验证收到空列表通知
        assertThat(received).isTrue();
        assertThat(notifiedInstances.get()).isEmpty();
    }
}

