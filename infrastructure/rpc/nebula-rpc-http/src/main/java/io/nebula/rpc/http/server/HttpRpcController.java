package io.nebula.rpc.http.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nebula.rpc.core.message.RpcRequest;
import io.nebula.rpc.core.message.RpcResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;

/**
 * HTTP RPC 控制器
 * 处理 HTTP RPC 请求,通过反射调用注册的服务
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
@RestController
@RequestMapping("/rpc")
public class HttpRpcController {

    private final HttpRpcServer rpcServer;
    private final ObjectMapper objectMapper;

    public HttpRpcController(HttpRpcServer rpcServer, ObjectMapper objectMapper) {
        this.rpcServer = rpcServer;
        this.objectMapper = objectMapper;
    }

    /**
     * 处理RPC请求
     */
    @PostMapping
    public ResponseEntity<RpcResponse> handleRpcRequest(@RequestBody RpcRequest request) {
        log.debug("收到RPC请求: requestId={}, service={}, method={}",
                request.getRequestId(), request.getServiceName(), request.getMethodName());

        try {
            // 查找服务实现
            Object serviceImpl = rpcServer.getServiceRegistry().get(request.getServiceName());
            if (serviceImpl == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(RpcResponse.error(request.getRequestId(), "服务未找到: " + request.getServiceName()));
            }

            // 通过反射调用方法
            Method method = findMethod(serviceImpl.getClass(), request.getMethodName(), request.getParameterTypes());
            if (method == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(RpcResponse.error(request.getRequestId(), "方法未找到: " + request.getMethodName()));
            }

            // 使用方法声明的参数类型做转换（而非请求中传来的类型，避免接口/实现类型不匹配）
            Object[] convertedParams = convertParameters(request.getParameters(), method.getParameterTypes());

            // 执行方法
            Object result = method.invoke(serviceImpl, convertedParams);

            // 返回响应
            return ResponseEntity.ok(RpcResponse.success(request.getRequestId(), result));

        } catch (Exception e) {
            log.error("RPC调用失败: requestId={}, service={}, method={}",
                    request.getRequestId(), request.getServiceName(), request.getMethodName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(RpcResponse.error(request.getRequestId(), "RPC调用失败: " + e.getMessage()));
        }
    }

    /**
     * 查找方法
     * <p>
     * 策略：
     * 1. 精确匹配 - 使用 getMethod(name, parameterTypes) 查找
     * 2. 兼容匹配 - 当客户端传来的参数类型是实现类（如 ArrayList）而方法声明使用接口（如 List）时，
     *    通过方法名 + 参数数量 + isAssignableFrom 做 fallback 匹配
     * 3. 名称匹配 - 当 parameterTypes 为 null 时，仅按方法名匹配（取唯一同名方法）
     */
    private Method findMethod(Class<?> clazz, String methodName, Class<?>[] parameterTypes) {
        // 策略1: 精确匹配
        if (parameterTypes != null) {
            try {
                return clazz.getMethod(methodName, parameterTypes);
            } catch (NoSuchMethodException e) {
                log.debug("精确匹配失败，尝试兼容匹配: method={}, parameterTypes={}", methodName, parameterTypes);
            }
        }

        // 策略2/3: 遍历所有公开方法做兼容匹配
        Method candidate = null;
        int candidateCount = 0;
        for (Method m : clazz.getMethods()) {
            if (!m.getName().equals(methodName)) {
                continue;
            }
            Class<?>[] declaredTypes = m.getParameterTypes();

            // 如果请求中没有 parameterTypes，按名称匹配
            if (parameterTypes == null) {
                candidate = m;
                candidateCount++;
                continue;
            }

            // 参数数量不同，跳过
            if (declaredTypes.length != parameterTypes.length) {
                continue;
            }

            // 检查每个参数类型是否兼容（请求类型 isAssignableFrom 声明类型，或反向兼容）
            boolean compatible = true;
            for (int i = 0; i < declaredTypes.length; i++) {
                if (!declaredTypes[i].isAssignableFrom(parameterTypes[i])
                        && !parameterTypes[i].isAssignableFrom(declaredTypes[i])) {
                    compatible = false;
                    break;
                }
            }
            if (compatible) {
                candidate = m;
                candidateCount++;
            }
        }

        if (candidateCount == 1) {
            log.debug("兼容匹配成功: method={}", methodName);
            return candidate;
        } else if (candidateCount > 1) {
            log.warn("兼容匹配到多个方法，返回最后一个: method={}, count={}", methodName, candidateCount);
            return candidate;
        }

        log.debug("方法未找到: {}", methodName);
        return null;
    }

    /**
     * 转换参数类型
     * 解决 Jackson 反序列化导致的类型不匹配问题
     * <p>
     * 使用方法声明的参数类型（而非请求中客户端推断的类型），
     * 确保 Jackson convertValue 能正确处理泛型信息
     */
    private Object[] convertParameters(Object[] parameters, Class<?>[] parameterTypes) {
        if (parameters == null || parameterTypes == null) {
            return parameters;
        }
        
        if (parameters.length != parameterTypes.length) {
            return parameters;
        }
        
        Object[] convertedParams = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i] == null) {
                convertedParams[i] = null;
            } else if (parameterTypes[i].isInstance(parameters[i])) {
                // 类型匹配,直接使用
                convertedParams[i] = parameters[i];
            } else {
                // 类型不匹配,使用 ObjectMapper 转换
                convertedParams[i] = objectMapper.convertValue(parameters[i], parameterTypes[i]);
            }
        }
        
        return convertedParams;
    }
}

