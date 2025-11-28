package io.nebula.gateway.grpc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nebula.core.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;

/**
 * gRPC网关过滤器
 * <p>
 * 将HTTP请求转换为gRPC调用，并将响应转换回HTTP响应
 */
@Slf4j
@RequiredArgsConstructor
public class GrpcGatewayFilter implements GatewayFilter {
    
    private final ObjectMapper objectMapper;
    private final AbstractGrpcServiceRouter serviceRouter;
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        String method = request.getMethod().name();
        
        log.info("gRPC Gateway Filter: {} {}, router: {}, routes: {}", 
                method, path, serviceRouter.getClass().getSimpleName(), serviceRouter.getRouteCount());
        
        // 尝试路由到gRPC服务
        AbstractGrpcServiceRouter.RouteInfo routeInfo = serviceRouter.route(path, method);
        
        if (routeInfo == null) {
            // 没有匹配的gRPC路由，继续使用HTTP代理
            log.warn("No gRPC route found for: {} {}, fallback to HTTP", method, path);
            return chain.filter(exchange);
        }
        
        log.info("Routing to gRPC: {} {} -> {}.{}", method, path,
                routeInfo.getServiceName(), routeInfo.getMethodName());
        
        // 读取请求体并执行gRPC调用
        if ("GET".equals(method) || "DELETE".equals(method)) {
            // GET/DELETE请求没有请求体
            return executeGrpcCall(exchange, routeInfo, null);
        } else {
            // POST/PUT请求读取请求体
            return DataBufferUtils.join(request.getBody())
                    .defaultIfEmpty(exchange.getResponse().bufferFactory().wrap(new byte[0]))
                    .flatMap(dataBuffer -> {
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bytes);
                        DataBufferUtils.release(dataBuffer);
                        String body = new String(bytes, StandardCharsets.UTF_8);
                        return executeGrpcCall(exchange, routeInfo, body);
                    });
        }
    }
    
    /**
     * 执行gRPC调用
     */
    private Mono<Void> executeGrpcCall(ServerWebExchange exchange,
                                        AbstractGrpcServiceRouter.RouteInfo routeInfo,
                                        String requestBody) {
        return Mono.fromCallable(() -> {
                    try {
                        // 调用gRPC服务
                        Object result = routeInfo.invoke(requestBody, exchange);
                        
                        // 包装为统一响应格式
                        if (result instanceof Result) {
                            return result;
                        }
                        return Result.success(result);
                    } catch (Exception e) {
                        log.error("gRPC调用失败: {}", e.getMessage(), e);
                        return Result.error("GRPC_ERROR", "服务调用失败: " + e.getMessage());
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())  // 在弹性线程池中执行阻塞调用
                .flatMap(result -> writeResponse(exchange, result));
    }
    
    /**
     * 写入响应
     */
    private Mono<Void> writeResponse(ServerWebExchange exchange, Object result) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.OK);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        
        try {
            String json = objectMapper.writeValueAsString(result);
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("响应序列化失败", e);
            return Mono.error(e);
        }
    }
}

