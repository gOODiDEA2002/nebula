package io.nebula.gateway.grpc;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;

/**
 * gRPC网关过滤器工厂
 * <p>
 * 用于在路由配置中使用：
 * <pre>
 * spring:
 *   cloud:
 *     gateway:
 *       routes:
 *         - id: grpc-route
 *           uri: no://op
 *           predicates:
 *             - Path=/api/**
 *           filters:
 *             - Grpc
 * </pre>
 */
public class GrpcGatewayFilterFactory extends AbstractGatewayFilterFactory<GrpcGatewayFilterFactory.Config> {
    
    private final ObjectMapper objectMapper;
    private final AbstractGrpcServiceRouter serviceRouter;
    
    /**
     * 构造函数
     * 
     * @param objectMapper JSON序列化器
     * @param serviceRouter gRPC服务路由器
     */
    public GrpcGatewayFilterFactory(ObjectMapper objectMapper, AbstractGrpcServiceRouter serviceRouter) {
        super(Config.class);
        this.objectMapper = objectMapper;
        this.serviceRouter = serviceRouter;
    }
    
    @Override
    public GatewayFilter apply(Config config) {
        return new GrpcGatewayFilter(objectMapper, serviceRouter);
    }
    
    @Override
    public String name() {
        return "Grpc";
    }
    
    /**
     * 配置类
     */
    public static class Config {
        // 可以添加配置项，如超时时间等
        private int timeout = 30000;
        
        public int getTimeout() {
            return timeout;
        }
        
        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }
    }
}

