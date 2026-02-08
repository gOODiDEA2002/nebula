package io.nebula.example.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API 网关启动类
 * 
 * 基于 Nebula Gateway 框架的 API 网关，特性：
 * - HTTP 反向代理 - 将请求转发到后端服务的 Controller
 * - JWT 认证 - 统一的用户身份验证
 * - 请求限流 - 基于 IP 的限流
 * - 熔断降级 - 基于 Resilience4j 的熔断保护
 * - 请求日志 - 请求追踪与慢请求标记
 * - CORS 处理 - 跨域请求支持
 * 
 * 架构原则：
 * - 前端接口通过 Controller 暴露（HTTP 代理转发）
 * - 服务间调用通过 RpcClient（不经过 Gateway）
 * 
 * @author Nebula Framework Team
 * @since 1.0.0
 */
@Slf4j
@SpringBootApplication
public class GatewayExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayExampleApplication.class, args);
        log.info("========================================");
        log.info("  Nebula Gateway Example 启动成功");
        log.info("  端口: 8000");
        log.info("  路由:");
        log.info("    /api/users/** -> user-service");
        log.info("    /api/orders/** -> order-service");
        log.info("========================================");
    }
}
