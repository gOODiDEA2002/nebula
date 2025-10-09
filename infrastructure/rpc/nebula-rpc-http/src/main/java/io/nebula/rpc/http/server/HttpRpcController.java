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

            // 转换参数类型(Jackson反序列化可能导致类型不匹配)
            Object[] convertedParams = convertParameters(request.getParameters(), request.getParameterTypes());

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
     */
    private Method findMethod(Class<?> clazz, String methodName, Class<?>[] parameterTypes) {
        try {
            return clazz.getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            log.debug("方法未找到: {}", methodName, e);
            return null;
        }
    }

    /**
     * 转换参数类型
     * 解决 Jackson 反序列化导致的类型不匹配问题
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

