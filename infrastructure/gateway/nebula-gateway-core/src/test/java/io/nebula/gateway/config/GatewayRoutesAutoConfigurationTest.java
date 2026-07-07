package io.nebula.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.route.RouteDefinition;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GatewayRoutesAutoConfiguration 路由生成测试
 */
class GatewayRoutesAutoConfigurationTest {

    @Test
    void discoveryEnabled_generatesLbUri() {
        GatewayProperties nebulaProp = new GatewayProperties();
        GatewayProperties.HttpProxyConfig httpConfig = nebulaProp.getHttp();
        httpConfig.setEnabled(true);
        httpConfig.setUseDiscovery(true);

        GatewayProperties.HttpServiceConfig svc = new GatewayProperties.HttpServiceConfig();
        svc.setApiPaths(List.of("/api/users/**"));
        httpConfig.getServices().put("user-service", svc);

        org.springframework.cloud.gateway.config.GatewayProperties springProp =
                new org.springframework.cloud.gateway.config.GatewayProperties();

        GatewayRoutesAutoConfiguration config = new GatewayRoutesAutoConfiguration(nebulaProp, springProp);
        config.configureRoutes();

        assertEquals(1, springProp.getRoutes().size());
        RouteDefinition route = springProp.getRoutes().get(0);
        assertEquals("lb://user-service", route.getUri().toString());
        assertEquals("nebula-http-user-service", route.getId());
    }

    @Test
    void staticAddress_usedDirectly() {
        GatewayProperties nebulaProp = new GatewayProperties();
        GatewayProperties.HttpProxyConfig httpConfig = nebulaProp.getHttp();
        httpConfig.setEnabled(true);
        httpConfig.setUseDiscovery(true);

        GatewayProperties.HttpServiceConfig svc = new GatewayProperties.HttpServiceConfig();
        svc.setAddress("http://10.0.0.5:8080");
        svc.setApiPaths(List.of("/api/orders/**"));
        httpConfig.getServices().put("order-service", svc);

        org.springframework.cloud.gateway.config.GatewayProperties springProp =
                new org.springframework.cloud.gateway.config.GatewayProperties();

        GatewayRoutesAutoConfiguration config = new GatewayRoutesAutoConfiguration(nebulaProp, springProp);
        config.configureRoutes();

        assertEquals(1, springProp.getRoutes().size());
        assertEquals("http://10.0.0.5:8080", springProp.getRoutes().get(0).getUri().toString());
    }

    @Test
    void customServiceName_usedInLbUri() {
        GatewayProperties nebulaProp = new GatewayProperties();
        GatewayProperties.HttpProxyConfig httpConfig = nebulaProp.getHttp();
        httpConfig.setEnabled(true);
        httpConfig.setUseDiscovery(true);

        GatewayProperties.HttpServiceConfig svc = new GatewayProperties.HttpServiceConfig();
        svc.setServiceName("custom-svc-name");
        svc.setApiPaths(List.of("/api/v2/**"));
        httpConfig.getServices().put("shortkey", svc);

        org.springframework.cloud.gateway.config.GatewayProperties springProp =
                new org.springframework.cloud.gateway.config.GatewayProperties();

        GatewayRoutesAutoConfiguration config = new GatewayRoutesAutoConfiguration(nebulaProp, springProp);
        config.configureRoutes();

        assertEquals("lb://custom-svc-name", springProp.getRoutes().get(0).getUri().toString());
    }
}
