package io.nebula.rpc.http.server;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nebula.rpc.core.message.RpcRequest;
import io.nebula.rpc.core.message.RpcResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

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
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
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

            // 通过反射调用方法（支持接口/实现类参数类型兼容匹配）
            Method method = findMethod(serviceImpl.getClass(), request.getMethodName(), request.getParameterTypes());
            if (method == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(RpcResponse.error(request.getRequestId(), "方法未找到: " + request.getMethodName()));
            }

            // 使用方法的泛型参数类型做深度转换（处理 List<Dto> 等嵌套泛型场景）
            Object[] convertedParams = convertParameters(request.getParameters(), method);

            // 执行方法
            Object result = method.invoke(serviceImpl, convertedParams);

            // 返回响应
            return ResponseEntity.ok(RpcResponse.success(request.getRequestId(), result));

        } catch (Exception e) {
            log.error("RPC调用失败: requestId={}, service={}, method={}",
                    request.getRequestId(), request.getServiceName(), request.getMethodName(), e);
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(RpcResponse.error(request.getRequestId(), "RPC调用失败: " + cause.getMessage()));
        }
    }

    /**
     * 查找方法（三级 fallback 策略）
     * <p>
     * 1. 精确匹配 - 使用 getMethod(name, parameterTypes)
     * 2. 兼容匹配 - 方法名 + 参数数量 + isAssignableFrom（处理 ArrayList vs List 等）
     * 3. 名称匹配 - 仅方法名 + 参数数量（parameterTypes 为 null 或反序列化丢失时）
     */
    private Method findMethod(Class<?> clazz, String methodName, Class<?>[] parameterTypes) {
        // 策略1: 精确匹配
        if (parameterTypes != null) {
            try {
                return clazz.getMethod(methodName, parameterTypes);
            } catch (NoSuchMethodException e) {
                log.debug("精确匹配失败，尝试兼容匹配: method={}", methodName);
            }
        }

        int paramCount = parameterTypes != null ? parameterTypes.length : -1;

        // 策略2: 兼容匹配（isAssignableFrom）
        if (parameterTypes != null) {
            for (Method m : clazz.getMethods()) {
                if (!m.getName().equals(methodName)) continue;
                Class<?>[] declaredTypes = m.getParameterTypes();
                if (declaredTypes.length != paramCount) continue;

                boolean compatible = true;
                for (int i = 0; i < declaredTypes.length; i++) {
                    if (!declaredTypes[i].isAssignableFrom(parameterTypes[i])
                            && !parameterTypes[i].isAssignableFrom(declaredTypes[i])) {
                        compatible = false;
                        break;
                    }
                }
                if (compatible) {
                    log.debug("兼容匹配成功: method={}", methodName);
                    return m;
                }
            }
        }

        // 策略3: 名称 + 参数数量匹配（最宽松，处理 parameterTypes 为 null 的情况）
        Method candidate = null;
        int count = 0;
        for (Method m : clazz.getMethods()) {
            if (!m.getName().equals(methodName)) continue;
            if (paramCount >= 0 && m.getParameterCount() != paramCount) continue;
            candidate = m;
            count++;
        }

        if (count == 1) {
            log.debug("名称匹配成功: method={}", methodName);
            return candidate;
        } else if (count > 1) {
            log.warn("名称匹配到多个同名方法，返回最后一个: method={}, count={}", methodName, count);
            return candidate;
        }

        log.warn("方法未找到: class={}, method={}", clazz.getSimpleName(), methodName);
        return null;
    }

    /**
     * 转换参数类型（使用方法的泛型参数类型）
     * <p>
     * 核心改进: 使用 Method.getGenericParameterTypes() 获取完整泛型信息，
     * 通过 JavaType 做深度转换，解决 List&lt;LinkedHashMap&gt; -> List&lt;Dto&gt; 的嵌套泛型问题。
     * <p>
     * 场景: 客户端发送 indexDocuments(List&lt;IndexRequest&gt;)，
     * Jackson 将 JSON 反序列化为 ArrayList&lt;LinkedHashMap&gt;，
     * 虽然 List.isInstance(ArrayList) 为 true，但内部元素类型不对。
     * 使用 JavaType 可以正确转换嵌套的泛型元素。
     */
    private Object[] convertParameters(Object[] parameters, Method method) {
        if (parameters == null) return parameters;

        Class<?>[] rawTypes = method.getParameterTypes();
        Type[] genericTypes = method.getGenericParameterTypes();

        if (parameters.length != rawTypes.length) {
            log.warn("参数数量不匹配: expected={}, actual={}", rawTypes.length, parameters.length);
            return parameters;
        }

        Object[] converted = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i] == null) {
                converted[i] = null;
                continue;
            }

            // 基础类型、String、枚举: 直接用 raw class 转换
            if (rawTypes[i].isPrimitive() || rawTypes[i] == String.class || rawTypes[i].isEnum()) {
                converted[i] = objectMapper.convertValue(parameters[i], rawTypes[i]);
                continue;
            }

            // 复杂类型: 统一用 JavaType（含泛型信息）做深度转换
            // 即使容器类型匹配（如 ArrayList），内部元素也可能是 LinkedHashMap 需要转换
            try {
                JavaType javaType = objectMapper.getTypeFactory().constructType(genericTypes[i]);
                converted[i] = objectMapper.convertValue(parameters[i], javaType);
            } catch (Exception e) {
                log.warn("参数转换失败，使用原始值: index={}, type={}, error={}",
                        i, rawTypes[i].getSimpleName(), e.getMessage());
                converted[i] = parameters[i];
            }
        }
        return converted;
    }
}

