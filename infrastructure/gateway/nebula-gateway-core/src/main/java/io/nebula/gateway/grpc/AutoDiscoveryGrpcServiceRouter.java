package io.nebula.gateway.grpc;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nebula.gateway.config.GatewayProperties;
import io.nebula.rpc.core.annotation.RpcCall;
import io.nebula.rpc.core.annotation.RpcClient;
import io.nebula.rpc.core.context.RpcContext;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ServerWebExchange;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 自动发现gRPC服务路由器
 * <p>
 * 根据@RpcClient接口和@RpcCall注解自动注册HTTP到gRPC的路由映射
 * <p>
 * 特性：
 * - 路径映射：自动将 /api/v1/* 映射到 /rpc/*
 * - Header传递：自动将HTTP Header传递给RPC服务
 * - userId注入：自动将X-User-Id header注入到请求DTO
 */
@Slf4j
public class AutoDiscoveryGrpcServiceRouter extends AbstractGrpcServiceRouter {

    private final GatewayProperties gatewayProperties;
    private final GrpcClientAutoRegistrar clientRegistrar;
    
    /** 方法信息缓存 */
    private final Map<String, MethodInfo> methodInfoCache = new HashMap<>();
    
    /** RPC路径到API路径的映射 */
    private final Map<String, String> rpcToApiPathMapping = new HashMap<>();

    public AutoDiscoveryGrpcServiceRouter(ObjectMapper objectMapper,
                                           GatewayProperties gatewayProperties,
                                           GrpcClientAutoRegistrar clientRegistrar) {
        super(objectMapper);
        this.gatewayProperties = gatewayProperties;
        this.clientRegistrar = clientRegistrar;
    }

    @Override
    public void init() {
        log.info("开始自动发现并注册gRPC路由...");
        registerRoutes();
        log.info("gRPC路由表初始化完成，共{}个路由", routes.size());
    }

    @Override
    protected void registerRoutes() {
        GatewayProperties.GrpcConfig grpcConfig = gatewayProperties.getGrpc();
        if (!grpcConfig.isEnabled() || !grpcConfig.isAutoScan()) {
            log.info("gRPC自动扫描已禁用");
            return;
        }

        // 遍历所有配置的服务
        for (Map.Entry<String, GatewayProperties.ServiceConfig> entry : grpcConfig.getServices().entrySet()) {
            String serviceName = entry.getKey();
            GatewayProperties.ServiceConfig serviceConfig = entry.getValue();

            if (!serviceConfig.isEnabled() || serviceConfig.getApiPackages().isEmpty()) {
                continue;
            }

            // 扫描服务的API包
            scanAndRegisterRoutes(serviceName, serviceConfig.getApiPackages());
        }
    }

    /**
     * 扫描指定包下的@RpcClient接口并注册路由
     */
    private void scanAndRegisterRoutes(String serviceName, List<String> packages) {
        // 创建自定义扫描器，支持扫描接口
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(org.springframework.beans.factory.annotation.AnnotatedBeanDefinition beanDefinition) {
                // 允许接口作为候选组件
                return beanDefinition.getMetadata().isInterface() || super.isCandidateComponent(beanDefinition);
            }
        };
        scanner.addIncludeFilter(new AnnotationTypeFilter(RpcClient.class));

        for (String packageName : packages) {
            Set<BeanDefinition> candidates = scanner.findCandidateComponents(packageName);

            for (BeanDefinition candidate : candidates) {
                try {
                    Class<?> interfaceClass = Class.forName(candidate.getBeanClassName());
                    if (!interfaceClass.isInterface()) {
                        continue;
                    }

                    // 获取代理对象
                    Object proxy = clientRegistrar.getProxy(interfaceClass);
                    if (proxy == null) {
                        log.warn("未找到 {} 的代理对象，跳过路由注册", interfaceClass.getSimpleName());
                        continue;
                    }

                    // 解析并注册接口方法的路由
                    registerInterfaceRoutes(serviceName, interfaceClass, proxy);

                } catch (ClassNotFoundException e) {
                    log.warn("无法加载类: {}", candidate.getBeanClassName(), e);
                }
            }
        }
    }

    /** 支持的 HTTP 方法列表 */
    private static final List<String> ALL_HTTP_METHODS = Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH");
    
    /**
     * 注册接口的所有方法路由
     */
    private void registerInterfaceRoutes(String serviceName, Class<?> interfaceClass, Object proxy) {
        GatewayProperties.PathMapping pathMapping = gatewayProperties.getGrpc().getPathMapping();
        
        for (Method method : interfaceClass.getDeclaredMethods()) {
            RpcCall rpcCall = method.getAnnotation(RpcCall.class);
            if (rpcCall == null) {
                continue;
            }

            String rpcPath = rpcCall.value();
            String httpMethodDef = rpcCall.method().toUpperCase();
            String methodName = method.getName();

            // 路径映射：将 RPC 路径映射到 API 路径
            String apiPath = rpcPath;
            if (pathMapping.isEnabled()) {
                apiPath = mapRpcPathToApiPath(rpcPath, pathMapping);
            }
            
            // 保存路径映射关系
            rpcToApiPathMapping.put(rpcPath, apiPath);

            // 缓存方法信息
            MethodInfo methodInfo = new MethodInfo(method, proxy, interfaceClass);
            
            // 确定要注册的 HTTP 方法列表
            List<String> httpMethods;
            if ("*".equals(httpMethodDef) || "ANY".equals(httpMethodDef)) {
                // 接受所有 HTTP 方法
                httpMethods = ALL_HTTP_METHODS;
            } else {
                // 只接受指定的 HTTP 方法
                httpMethods = Collections.singletonList(httpMethodDef);
            }
            
            // 为每个 HTTP 方法注册路由
            for (String httpMethod : httpMethods) {
                String cacheKey = httpMethod + ":" + apiPath;
                methodInfoCache.put(cacheKey, methodInfo);

                // 注册路由（使用 API 路径）
                final String finalApiPath = apiPath;
                registerRoute(httpMethod, apiPath, serviceName, methodName, (body, exchange) -> {
                    try {
                        return invokeMethod(methodInfo, body, exchange);
                    } catch (Exception e) {
                        log.error("调用RPC方法失败: {}.{}", interfaceClass.getSimpleName(), methodName, e);
                        throw new RuntimeException("RPC调用失败: " + e.getMessage(), e);
                    }
                });
            }

            // 日志输出
            String methodsStr = httpMethods.size() > 1 ? "[" + String.join(",", httpMethods) + "]" : httpMethods.get(0);
            log.info("注册路由: {} {} -> {}.{} (RPC: {})", 
                    methodsStr, apiPath, interfaceClass.getSimpleName(), methodName, rpcPath);
        }
    }
    
    /**
     * 将 RPC 路径映射到 API 路径
     * 例如: /rpc/users/login -> /api/v1/users/login
     */
    private String mapRpcPathToApiPath(String rpcPath, GatewayProperties.PathMapping pathMapping) {
        String rpcPrefix = pathMapping.getRpcPrefix();
        String apiPrefix = pathMapping.getApiPrefix();
        
        if (rpcPath.startsWith(rpcPrefix)) {
            return apiPrefix + rpcPath.substring(rpcPrefix.length());
        }
        
        return rpcPath;
    }

    /**
     * 调用RPC方法
     */
    private Object invokeMethod(MethodInfo methodInfo, String body, ServerWebExchange exchange) throws Exception {
        Method method = methodInfo.getMethod();
        Object proxy = methodInfo.getProxy();
        Parameter[] parameters = method.getParameters();

        // 获取 HTTP Headers
        HttpHeaders headers = exchange.getRequest().getHeaders();
        
        // 设置 RpcContext（传递用户信息到后端服务）
        setupRpcContext(headers);
        
        try {
            // 准备方法参数
            Object[] args = new Object[parameters.length];

            for (int i = 0; i < parameters.length; i++) {
                Parameter param = parameters[i];
                args[i] = resolveParameter(param, body, exchange, headers);
            }

            // 调用方法
            return method.invoke(proxy, args);
        } finally {
            // 清除 RpcContext
            RpcContext.clear();
        }
    }
    
    /**
     * 设置 RpcContext（从 HTTP Headers 传递到后端服务）
     * <p>
     * 根据配置的 header-propagation.includes 规则，
     * 将匹配的 Headers 设置到 RpcContext，
     * 框架只负责传递，不关心业务含义。
     */
    private void setupRpcContext(HttpHeaders headers) {
        GatewayProperties.HeaderPropagation config = gatewayProperties.getGrpc().getHeaderPropagation();
        if (!config.isEnabled()) {
            return;
        }
        
        // 根据配置传递 Headers
        for (String include : config.getIncludes()) {
            if (include.endsWith("*")) {
                // 通配符匹配（如 X-*）
                String prefix = include.substring(0, include.length() - 1);
                headers.forEach((name, values) -> {
                    if (name.toLowerCase().startsWith(prefix.toLowerCase()) && !values.isEmpty()) {
                        RpcContext.set(name, values.get(0));
                    }
                });
            } else {
                // 精确匹配
                String value = headers.getFirst(include);
                if (value != null && !value.isEmpty()) {
                    RpcContext.set(include, value);
                }
            }
        }
        
        if (log.isDebugEnabled()) {
            log.debug("RpcContext 已设置 {} 个 header", RpcContext.getAll().size());
        }
    }

    /**
     * 解析方法参数
     */
    private Object resolveParameter(Parameter param, String body, ServerWebExchange exchange, 
                                     HttpHeaders headers) throws Exception {
        Class<?> paramType = param.getType();
        GatewayProperties.HeaderPropagation headerPropagation = gatewayProperties.getGrpc().getHeaderPropagation();

        // @RequestBody 注解的参数
        if (param.isAnnotationPresent(RequestBody.class)) {
            Object dto;
            if (body == null || body.isEmpty()) {
                try {
                    dto = paramType.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    dto = null;
                }
            } else {
                dto = objectMapper.readValue(body, paramType);
            }
            
            // 注意：不再自动注入 userId 到 DTO
            // 后端服务应通过 CurrentUserContext.getUserId() 获取当前用户
            
            return dto;
        }

        // @PathVariable 注解的参数
        PathVariable pathVariable = param.getAnnotation(PathVariable.class);
        if (pathVariable != null) {
            String varName = pathVariable.value().isEmpty() ? param.getName() : pathVariable.value();
            String varValue = extractPathVariable(exchange.getRequest().getPath().value(), varName);
            return convertValue(varValue, paramType);
        }

        // @RequestParam 注解的参数
        RequestParam requestParam = param.getAnnotation(RequestParam.class);
        if (requestParam != null) {
            String paramName = requestParam.value().isEmpty() ? param.getName() : requestParam.value();
            String paramValue = exchange.getRequest().getQueryParams().getFirst(paramName);
            if (paramValue == null && requestParam.required()) {
                throw new IllegalArgumentException("缺少必需的请求参数: " + paramName);
            }
            return convertValue(paramValue, paramType);
        }

        // ServerWebExchange 类型参数
        if (ServerWebExchange.class.isAssignableFrom(paramType)) {
            return exchange;
        }
        
        // HttpHeaders 类型参数
        if (HttpHeaders.class.isAssignableFrom(paramType)) {
            return filterHeaders(headers, headerPropagation);
        }

        // 默认尝试从请求体解析（POST 请求）
        if (body != null && !body.isEmpty() && !paramType.isPrimitive() && paramType != String.class) {
            try {
                Object dto = objectMapper.readValue(body, paramType);
                return dto;
            } catch (Exception e) {
                log.debug("无法将请求体解析为 {}: {}", paramType.getSimpleName(), e.getMessage());
            }
        }
        
        // GET 请求时，尝试从查询参数构建 DTO
        if (!paramType.isPrimitive() && paramType != String.class) {
            try {
                Object dto = buildDtoFromQueryParams(exchange, paramType);
                return dto;
            } catch (Exception e) {
                log.debug("无法从查询参数构建 {}: {}", paramType.getSimpleName(), e.getMessage());
            }
        }

        return null;
    }
    
    /**
     * 从查询参数构建 DTO 对象
     * 支持 GET 请求将查询参数映射到 DTO 字段
     */
    private Object buildDtoFromQueryParams(ServerWebExchange exchange, Class<?> dtoType) {
        var queryParams = exchange.getRequest().getQueryParams();
        if (queryParams.isEmpty()) {
            // 没有查询参数时，尝试创建空对象（使用默认值）
            try {
                return dtoType.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                return null;
            }
        }
        
        try {
            // 将查询参数转换为 Map，过滤空值
            Map<String, Object> paramMap = new HashMap<>();
            queryParams.forEach((key, values) -> {
                if (values != null && !values.isEmpty()) {
                    // 过滤空字符串
                    List<String> nonEmptyValues = values.stream()
                            .filter(v -> v != null && !v.isEmpty())
                            .toList();
                    if (!nonEmptyValues.isEmpty()) {
                        // 单值参数直接使用，多值参数保持为列表
                        paramMap.put(key, nonEmptyValues.size() == 1 ? nonEmptyValues.get(0) : nonEmptyValues);
                    }
                }
            });
            
            // 如果过滤后没有有效参数，创建默认对象
            if (paramMap.isEmpty()) {
                return dtoType.getDeclaredConstructor().newInstance();
            }
            
            // 使用 ObjectMapper 将 Map 转换为 DTO
            return objectMapper.convertValue(paramMap, dtoType);
        } catch (Exception e) {
            log.debug("从查询参数构建 DTO 失败: {}", e.getMessage());
            return null;
        }
    }
    
    
    /**
     * 过滤 Headers
     */
    private HttpHeaders filterHeaders(HttpHeaders original, GatewayProperties.HeaderPropagation config) {
        if (!config.isEnabled()) {
            return new HttpHeaders();
        }
        
        HttpHeaders filtered = new HttpHeaders();
        List<String> includes = config.getIncludes();
        List<String> excludes = config.getExcludes();
        
        original.forEach((name, values) -> {
            if (shouldIncludeHeader(name, includes, excludes)) {
                filtered.addAll(name, values);
            }
        });
        
        return filtered;
    }
    
    /**
     * 判断是否应该包含 Header
     */
    private boolean shouldIncludeHeader(String headerName, List<String> includes, List<String> excludes) {
        // 检查排除列表
        for (String exclude : excludes) {
            if (matchesPattern(headerName, exclude)) {
                return false;
            }
        }
        
        // 检查包含列表
        for (String include : includes) {
            if (matchesPattern(headerName, include)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 匹配模式（支持 * 通配符）
     */
    private boolean matchesPattern(String value, String pattern) {
        if (pattern.equals("*")) {
            return true;
        }
        if (pattern.endsWith("*")) {
            String prefix = pattern.substring(0, pattern.length() - 1);
            return value.toLowerCase().startsWith(prefix.toLowerCase());
        }
        return value.equalsIgnoreCase(pattern);
    }

    /**
     * 从路径中提取变量值
     */
    private String extractPathVariable(String actualPath, String varName) {
        // 遍历路由表查找匹配的模式
        for (String routeKey : routes.keySet()) {
            RouteInfo routeInfo = routes.get(routeKey);
            String pattern = routeInfo.getPathPattern();
            
            // 将模式转换为正则表达式
            String regex = pattern.replaceAll("\\{([^}]+)\\}", "(?<$1>[^/]+)");
            Pattern p = Pattern.compile(regex);
            Matcher m = p.matcher(actualPath);
            
            if (m.matches()) {
                try {
                    return m.group(varName);
                } catch (IllegalArgumentException e) {
                    // 变量名不匹配，继续尝试
                }
            }
        }
        
        // 简单的数字提取作为后备方案
        Pattern numPattern = Pattern.compile("/(\\d+)(?:/|$)");
        Matcher numMatcher = numPattern.matcher(actualPath);
        if (numMatcher.find()) {
            return numMatcher.group(1);
        }
        
        return null;
    }

    /**
     * 值类型转换
     */
    private Object convertValue(String value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        
        if (targetType == String.class) {
            return value;
        } else if (targetType == Long.class || targetType == long.class) {
            return Long.parseLong(value);
        } else if (targetType == Integer.class || targetType == int.class) {
            return Integer.parseInt(value);
        } else if (targetType == Double.class || targetType == double.class) {
            return Double.parseDouble(value);
        } else if (targetType == Boolean.class || targetType == boolean.class) {
            return Boolean.parseBoolean(value);
        }
        
        return value;
    }

    /**
     * 方法信息缓存类
     */
    @Data
    private static class MethodInfo {
        private final Method method;
        private final Object proxy;
        private final Class<?> interfaceClass;

        public MethodInfo(Method method, Object proxy, Class<?> interfaceClass) {
            this.method = method;
            this.proxy = proxy;
            this.interfaceClass = interfaceClass;
        }
    }
}
