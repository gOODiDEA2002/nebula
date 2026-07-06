package io.nebula.discovery.nacos;

import io.nebula.discovery.core.ServiceDiscovery;
import io.nebula.discovery.core.ServiceInstance;
import io.nebula.discovery.nacos.config.NacosProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.boot.web.server.WebServer;
import org.springframework.core.env.Environment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证自动注册监听父类 {@link WebServerInitializedEvent}，从而同时覆盖 Servlet 与 Reactive(如网关)应用。
 * 修复前只判 ServletWebServerInitializedEvent，Reactive 应用永远不触发注册。
 */
@ExtendWith(MockitoExtension.class)
class NacosServiceAutoRegistrarReactiveTest {

    @Mock
    private ServiceDiscovery serviceDiscovery;

    @Mock
    private Environment environment;

    @Mock
    private WebServerInitializedEvent event;

    @Mock
    private WebServer webServer;

    @Test
    void webServerInitializedEventTriggersRegistration() throws Exception {
        NacosProperties props = new NacosProperties();
        props.setIp("127.0.0.1"); // 避免真实网卡探测
        NacosServiceAutoRegistrar registrar =
                new NacosServiceAutoRegistrar(serviceDiscovery, props, environment);

        when(event.getWebServer()).thenReturn(webServer);
        when(webServer.getPort()).thenReturn(8080);
        when(environment.getProperty("spring.application.name", "unknown-service")).thenReturn("svc");
        when(environment.getProperty("spring.application.version", "1.0.0")).thenReturn("1.0.0");
        when(environment.getActiveProfiles()).thenReturn(new String[0]);
        when(environment.getProperty("grpc.server.port")).thenReturn(null);

        // 任意 WebServerInitializedEvent(含 Reactive 的子类)都应触发注册
        registrar.onApplicationEvent(event);

        verify(serviceDiscovery).register(any(ServiceInstance.class));
    }
}
