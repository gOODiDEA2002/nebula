package io.nebula.rpc.grpc.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.stub.StreamObserver;
import io.nebula.rpc.core.annotation.RpcClient;
import io.nebula.rpc.core.annotation.RemoteService;
import io.nebula.rpc.core.annotation.RpcService;
import io.nebula.rpc.core.context.RpcContext;
import io.nebula.rpc.grpc.proto.GenericRpcServiceGrpc;
import io.nebula.rpc.grpc.proto.RpcRequest;
import io.nebula.rpc.grpc.proto.RpcResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * gRPC RPC 服务器
 * 处理通用RPC调用
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
public class GrpcRpcServer extends GenericRpcServiceGrpc.GenericRpcServiceImplBase 
        implements ApplicationContextAware {

    private ApplicationContext applicationContext;
    private final ObjectMapper objectMapper;
    private final Map<String, Object> serviceRegistry = new ConcurrentHashMap<>();

    public GrpcRpcServer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        // 扫描并注册所有 @RpcService 标注的服务
        registerRpcServices();
    }

    /**
     * 注册RPC服务
     */
    private void registerRpcServices() {
        Map<String, Object> rpcServices = applicationContext.getBeansWithAnnotation(RpcService.class);
        for (Map.Entry<String, Object> entry : rpcServices.entrySet()) {
            Object serviceBean = entry.getValue();
            Class<?> beanClass = serviceBean.getClass();
            RpcService rpcService = beanClass.getAnnotation(RpcService.class);
            
            // 获取RPC接口类（自动推导或手动指定）
            Class<?> serviceInterface = findServiceInterface(beanClass, rpcService);
            
            // 使用接口全限定名作为服务名
            String serviceName = serviceInterface.getName();
            serviceRegistry.put(serviceName, serviceBean);
            log.info("注册 gRPC RPC 服务: {} -> {}", serviceName, beanClass.getSimpleName());
        }
    }
    
    /**
     * 查找服务接口
     * 如果 @RpcService 没有指定接口，自动查找标注了 @RpcClient 的接口
     * 
     * @param beanClass 服务实现类
     * @param rpcService RpcService注解
     * @return 服务接口类
     */
    private Class<?> findServiceInterface(Class<?> beanClass, RpcService rpcService) {
        Class<?> specifiedInterface = rpcService.value();
        if (specifiedInterface != null && specifiedInterface != void.class) {
            return specifiedInterface;
        }
        
        Class<?>[] interfaces = beanClass.getInterfaces();
        List<Class<?>> rpcInterfaces = new ArrayList<>();
        
        for (Class<?> iface : interfaces) {
            if (iface.isAnnotationPresent(RpcClient.class) 
                    || iface.isAnnotationPresent(RemoteService.class)) {
                rpcInterfaces.add(iface);
            }
        }
        
        if (rpcInterfaces.isEmpty()) {
            throw new IllegalStateException(String.format(
                "类 %s 没有实现任何标注了 @RpcClient 或 @RemoteService 的接口，请在 @RpcService 中手动指定接口类",
                beanClass.getName()));
        }
        
        if (rpcInterfaces.size() > 1) {
            throw new IllegalStateException(String.format(
                "类 %s 实现了多个 RPC 接口 %s，请在 @RpcService 中手动指定接口类",
                beanClass.getName(), rpcInterfaces));
        }
        
        log.info("自动推导 gRPC RPC 服务接口: {} -> {}", 
            beanClass.getSimpleName(), rpcInterfaces.get(0).getSimpleName());
        
        return rpcInterfaces.get(0);
    }

    @Override
    public void call(RpcRequest request, StreamObserver<RpcResponse> responseObserver) {
        String requestId = request.getRequestId();
        if (requestId == null || requestId.isEmpty()) {
            requestId = UUID.randomUUID().toString();
        }

        log.debug("收到 gRPC RPC 请求: requestId={}, service={}, method={}", 
                requestId, request.getServiceName(), request.getMethodName());

        RpcResponse.Builder responseBuilder = RpcResponse.newBuilder()
                .setRequestId(requestId)
                .setTimestamp(System.currentTimeMillis());

        try {
            // 将请求中的 metadata 设置到 RpcContext，供业务层使用
            RpcContext.setAll(request.getMetadataMap());
            
            // 查找服务实例
            Object serviceInstance = serviceRegistry.get(request.getServiceName());
            if (serviceInstance == null) {
                throw new IllegalStateException("服务未找到: " + request.getServiceName());
            }

            // 查找方法（参数类型仅以类型名字符串参与匹配，不做 Class.forName，杜绝任意类加载）
            Method method = findMethod(serviceInstance.getClass(), request.getMethodName(),
                    request.getParameterTypesList());
            if (method == null) {
                throw new NoSuchMethodException(
                        String.format("方法未找到: %s.%s", request.getServiceName(), request.getMethodName()));
            }

            // 解析参数值（使用方法的泛型参数类型以支持 List<Long> 等泛型类型）
            Object[] parameters = parseParametersWithGenericTypes(request.getParametersList(), method.getGenericParameterTypes());

            // 执行方法
            Object result = method.invoke(serviceInstance, parameters);

            // 序列化结果
            String resultJson = objectMapper.writeValueAsString(result);

            responseBuilder
                    .setSuccess(true)
                    .setResult(resultJson);

            log.debug("gRPC RPC 调用成功: requestId={}, service={}, method={}", 
                    requestId, request.getServiceName(), request.getMethodName());

        } catch (Exception e) {
            // 获取根本原因（处理 InvocationTargetException 等包装异常）
            Throwable rootCause = getRootCause(e);
            log.error("gRPC RPC 调用失败: requestId={}, service={}, method={}", 
                    requestId, request.getServiceName(), request.getMethodName(), rootCause);

            // 确保 errorMessage 不为 null（protobuf 不允许 null 值）
            String errorMessage = rootCause.getMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = rootCause.getClass().getName();
            }

            responseBuilder
                    .setSuccess(false)
                    .setErrorCode("RPC_CALL_ERROR")
                    .setErrorMessage(errorMessage)
                    .setStackTrace(getStackTrace(e));
        } finally {
            // 清除 RpcContext，防止内存泄漏
            RpcContext.clear();
        }

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }
    
    /**
     * 查找方法。
     * <p>
     * 参数类型仅以"全限定类名字符串"参与匹配（比对目标类已声明方法的参数类型名），
     * 不对请求携带的类名做 {@code Class.forName}，杜绝任意类加载（与 HTTP 侧 HttpRpcController 策略一致）。
     * <p>
     * 1. 名称 + 参数数量 + 声明参数类型名精确匹配；
     * 2. 名称 + 参数数量匹配（客户端发送运行时具体类型如 ArrayList 而声明为 List、null 参数占位 java.lang.Object 等场景兜底）。
     */
    Method findMethod(Class<?> clazz, String methodName, java.util.List<String> parameterTypeNames) {
        int paramCount = parameterTypeNames != null ? parameterTypeNames.size() : -1;

        // 策略1: 方法名 + 参数数量 + 声明参数类型名逐一精确匹配（仅按名字比对，不加载类）
        if (parameterTypeNames != null) {
            for (Method m : clazz.getMethods()) {
                if (!m.getName().equals(methodName)) {
                    continue;
                }
                Class<?>[] declaredTypes = m.getParameterTypes();
                if (declaredTypes.length != paramCount) {
                    continue;
                }
                boolean allMatch = true;
                for (int i = 0; i < declaredTypes.length; i++) {
                    if (!declaredTypes[i].getName().equals(parameterTypeNames.get(i))) {
                        allMatch = false;
                        break;
                    }
                }
                if (allMatch) {
                    log.debug("类型名精确匹配: method={}", methodName);
                    return m;
                }
            }
        }

        // 策略2: 名称 + 参数数量匹配（类型名对不上或缺失时兜底）
        Method candidate = null;
        int count = 0;
        for (Method m : clazz.getMethods()) {
            if (!m.getName().equals(methodName)) {
                continue;
            }
            if (paramCount >= 0 && m.getParameterCount() != paramCount) {
                continue;
            }
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
     * 使用泛型类型解析参数值
     * <p>
     * 相比 parseParameters，此方法支持泛型类型（如 List&lt;Long&gt;），
     * 能正确将 JSON 数字反序列化为指定的泛型元素类型。
     * </p>
     * 
     * @param parameterJsonList 参数 JSON 列表
     * @param genericParameterTypes 方法的泛型参数类型（来自 Method.getGenericParameterTypes()）
     * @return 解析后的参数数组
     */
    private Object[] parseParametersWithGenericTypes(java.util.List<String> parameterJsonList, 
                                                     java.lang.reflect.Type[] genericParameterTypes) 
            throws Exception {
        Object[] parameters = new Object[parameterJsonList.size()];
        for (int i = 0; i < parameterJsonList.size(); i++) {
            String parameterJson = parameterJsonList.get(i);
            // 使用 Jackson 的 TypeFactory 构建完整的类型信息，支持泛型
            com.fasterxml.jackson.databind.JavaType javaType = 
                    objectMapper.getTypeFactory().constructType(genericParameterTypes[i]);
            parameters[i] = objectMapper.readValue(parameterJson, javaType);
        }
        return parameters;
    }

    /**
     * 获取异常堆栈信息
     */
    private String getStackTrace(Exception e) {
        StringBuilder sb = new StringBuilder();
        Throwable rootCause = getRootCause(e);
        sb.append(rootCause.getClass().getName()).append(": ").append(rootCause.getMessage()).append("\n");
        for (StackTraceElement element : rootCause.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
            if (sb.length() > 2000) { // 限制堆栈长度
                sb.append("\t...");
                break;
            }
        }
        return sb.toString();
    }

    /**
     * 获取根本异常（解包 InvocationTargetException 等包装异常）
     */
    private Throwable getRootCause(Throwable e) {
        Throwable cause = e;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }
}

