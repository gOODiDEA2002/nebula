# Nebula Starter Gateway - 测试指南

本文档说明网关的基础测试策略。

## 测试前提

- 本地启动网关应用
- 至少有一个后端服务可被发现或手动配置路由

## 单元测试建议

### 1. 路由配置校验

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRouteTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldRouteToUserService() {
        webTestClient.get()
            .uri("/api/v1/users/health")
            .exchange()
            .expectStatus().is2xxSuccessful();
    }
}
```

### 2. 限流验证

模拟高频请求，确认返回 `429` 或自定义限流响应。

## 集成测试建议

1. 使用 Docker Compose 启动 Nacos + 用户服务
2. 网关开启 `use-discovery=true`
3. 验证服务注册后路由是否自动生效
