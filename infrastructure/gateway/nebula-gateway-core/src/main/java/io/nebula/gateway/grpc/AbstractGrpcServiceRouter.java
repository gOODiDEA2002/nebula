package io.nebula.gateway.grpc;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.server.ServerWebExchange;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * gRPC服务路由器抽象基类
 * <p>
 * 子类需要实现 {@link #registerRoutes()} 方法来注册具体的路由
 * <p>
 * 示例：
 * <pre>
 * @Component
 * public class MyGrpcServiceRouter extends AbstractGrpcServiceRouter {
 *     
 *     private final UserRpcClient userRpcClient;
 *     
 *     protected void registerRoutes() {
 *         registerRoute("POST", "/api/v1/users/login", "user", "login",
 *             (body, exchange) -> userRpcClient.login(parseBody(body, LoginDto.Request.class)));
 *     }
 * }
 * </pre>
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractGrpcServiceRouter {
    
    protected final ObjectMapper objectMapper;
    
    /**
     * 路由表：key -> RouteInfo
     */
    protected final Map<String, RouteInfo> routes = new HashMap<>();
    
    /**
     * 初始化路由表
     * 子类应该调用 {@link #registerRoute} 方法注册路由
     */
    @jakarta.annotation.PostConstruct
    public void init() {
        registerRoutes();
        log.info("gRPC路由表初始化完成，共{}个路由", routes.size());
    }
    
    /**
     * 注册路由
     * 子类实现此方法来注册具体的路由
     */
    protected abstract void registerRoutes();
    
    /**
     * 注册单个路由
     *
     * @param method      HTTP方法
     * @param pathPattern 路径模式（支持{id}等路径变量）
     * @param serviceName 服务名称
     * @param methodName  方法名称
     * @param invoker     调用器
     */
    protected void registerRoute(String method, String pathPattern, String serviceName,
                                 String methodName, GrpcInvoker invoker) {
        String key = method + ":" + pathPattern;
        routes.put(key, new RouteInfo(serviceName, methodName, pathPattern, invoker));
        log.debug("注册gRPC路由: {} {} -> {}.{}", method, pathPattern, serviceName, methodName);
    }
    
    /**
     * 获取路由数量
     */
    public int getRouteCount() {
        return routes.size();
    }
    
    /**
     * 路由匹配
     *
     * @param path   请求路径
     * @param method HTTP方法
     * @return 匹配的路由信息，未找到返回null
     */
    public RouteInfo route(String path, String method) {
        // 精确匹配
        String key = method + ":" + path;
        RouteInfo routeInfo = routes.get(key);
        if (routeInfo != null) {
            return routeInfo;
        }
        
        // 模式匹配（带路径变量）
        for (Map.Entry<String, RouteInfo> entry : routes.entrySet()) {
            if (!entry.getKey().startsWith(method + ":")) {
                continue;
            }
            
            String pattern = entry.getValue().getPathPattern();
            // 将{id}、{xxx}等转换为正则表达式
            String regex = pattern.replaceAll("\\{\\w+\\}", "[^/]+");
            if (path.matches(regex)) {
                return entry.getValue();
            }
        }
        
        return null;
    }
    
    /**
     * 解析请求体为指定类型
     *
     * @param body  请求体JSON字符串
     * @param clazz 目标类型
     * @return 解析后的对象
     */
    protected <T> T parseBody(String body, Class<T> clazz) {
        if (body == null || body.isEmpty()) {
            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new GrpcRouteException("无法创建空请求对象", e);
            }
        }
        try {
            return objectMapper.readValue(body, clazz);
        } catch (Exception e) {
            throw new GrpcRouteException("请求体解析失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 从请求头获取字符串值
     *
     * @param exchange   请求上下文
     * @param headerName Header 名称
     * @return Header 值，不存在则返回 null
     */
    protected String getHeader(ServerWebExchange exchange, String headerName) {
        return exchange.getRequest().getHeaders().getFirst(headerName);
    }
    
    /**
     * 从请求头获取 Long 值
     *
     * @param exchange   请求上下文
     * @param headerName Header 名称
     * @return Header 值解析为 Long，不存在或解析失败返回 null
     */
    protected Long getHeaderAsLong(ServerWebExchange exchange, String headerName) {
        String value = exchange.getRequest().getHeaders().getFirst(headerName);
        if (value != null && !value.isEmpty()) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                log.warn("无法解析Header {}为Long: {}", headerName, value);
            }
        }
        return null;
    }
    
    /**
     * 从路径中提取变量（数字类型）
     *
     * @param path    请求路径
     * @param pattern 正则模式，使用捕获组提取变量
     * @return 提取的数字
     */
    protected Long extractPathVariableLong(String path, String pattern) {
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(path);
        if (m.find()) {
            return Long.parseLong(m.group(1));
        }
        throw new GrpcRouteException("无法从路径提取变量: " + path);
    }
    
    /**
     * 从路径中提取变量（字符串类型）
     *
     * @param path    请求路径
     * @param pattern 正则模式，使用捕获组提取变量
     * @return 提取的字符串
     */
    protected String extractPathVariableString(String path, String pattern) {
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(path);
        if (m.find()) {
            return m.group(1);
        }
        throw new GrpcRouteException("无法从路径提取变量: " + path);
    }
    
    /**
     * 获取查询参数
     */
    protected String getQueryParam(ServerWebExchange exchange, String name) {
        return exchange.getRequest().getQueryParams().getFirst(name);
    }
    
    /**
     * 获取查询参数（Long类型）
     */
    protected Long getQueryParamLong(ServerWebExchange exchange, String name) {
        String value = getQueryParam(exchange, name);
        if (value != null && !value.isEmpty()) {
            return Long.parseLong(value);
        }
        return null;
    }
    
    /**
     * 获取查询参数（Integer类型）
     */
    protected Integer getQueryParamInt(ServerWebExchange exchange, String name) {
        String value = getQueryParam(exchange, name);
        if (value != null && !value.isEmpty()) {
            return Integer.parseInt(value);
        }
        return null;
    }
    
    /**
     * 获取客户端IP
     */
    protected String getClientIp(ServerWebExchange exchange) {
        // 从X-Forwarded-For获取
        String xff = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        
        // 从X-Real-IP获取
        String xri = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
        if (xri != null && !xri.isEmpty()) {
            return xri;
        }
        
        // 从远程地址获取
        if (exchange.getRequest().getRemoteAddress() != null) {
            return exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        }
        
        return "unknown";
    }
    
    /**
     * 路由信息
     */
    @Data
    public static class RouteInfo {
        private final String serviceName;
        private final String methodName;
        private final String pathPattern;
        private final GrpcInvoker invoker;
        
        public Object invoke(String body, ServerWebExchange exchange) {
            return invoker.invoke(body, exchange);
        }
    }
    
    /**
     * gRPC调用接口
     */
    @FunctionalInterface
    public interface GrpcInvoker {
        /**
         * 执行gRPC调用
         *
         * @param body     请求体JSON字符串
         * @param exchange Web请求上下文
         * @return 调用结果
         */
        Object invoke(String body, ServerWebExchange exchange);
    }
    
    /**
     * gRPC路由异常
     */
    public static class GrpcRouteException extends RuntimeException {
        public GrpcRouteException(String message) {
            super(message);
        }
        
        public GrpcRouteException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

