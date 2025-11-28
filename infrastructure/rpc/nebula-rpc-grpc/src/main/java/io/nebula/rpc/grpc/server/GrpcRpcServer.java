package io.nebula.rpc.grpc.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.stub.StreamObserver;
import io.nebula.rpc.core.annotation.RpcClient;
import io.nebula.rpc.core.annotation.RpcService;
import io.nebula.rpc.core.context.RpcContext;
import io.nebula.rpc.grpc.proto.GenericRpcServiceGrpc;
import io.nebula.rpc.grpc.proto.RpcRequest;
import io.nebula.rpc.grpc.proto.RpcResponse;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
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
@GrpcService
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
        // 1. 如果手动指定了接口，直接使用
        Class<?> specifiedInterface = rpcService.value();
        if (specifiedInterface != null && specifiedInterface != void.class) {
            return specifiedInterface;
        }
        
        // 2. 自动查找标注了 @RpcClient 的接口
        Class<?>[] interfaces = beanClass.getInterfaces();
        List<Class<?>> rpcInterfaces = new ArrayList<>();
        
        for (Class<?> iface : interfaces) {
            if (iface.isAnnotationPresent(RpcClient.class)) {
                rpcInterfaces.add(iface);
            }
        }
        
        // 3. 验证结果
        if (rpcInterfaces.isEmpty()) {
            throw new IllegalStateException(String.format(
                "类 %s 没有实现任何标注了 @RpcClient 的接口，请在 @RpcService 中手动指定接口类",
                beanClass.getName()));
        }
        
        if (rpcInterfaces.size() > 1) {
            throw new IllegalStateException(String.format(
                "类 %s 实现了多个 @RpcClient 接口 %s，请在 @RpcService 中手动指定接口类",
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

            // 解析参数类型
            Class<?>[] parameterTypes = parseParameterTypes(request.getParameterTypesList());

            // 查找方法
            Method method = findMethod(serviceInstance.getClass(), request.getMethodName(), parameterTypes);
            if (method == null) {
                throw new NoSuchMethodException(
                        String.format("方法未找到: %s.%s", request.getServiceName(), request.getMethodName()));
            }

            // 解析参数值
            Object[] parameters = parseParameters(request.getParametersList(), parameterTypes);

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
     * 解析参数类型
     */
    private Class<?>[] parseParameterTypes(java.util.List<String> typeNames) throws ClassNotFoundException {
        Class<?>[] types = new Class<?>[typeNames.size()];
        for (int i = 0; i < typeNames.size(); i++) {
            types[i] = Class.forName(typeNames.get(i));
        }
        return types;
    }

    /**
     * 查找方法
     */
    private Method findMethod(Class<?> clazz, String methodName, Class<?>[] parameterTypes) {
        try {
            return clazz.getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            // 尝试在接口中查找
            for (Class<?> interfaceClass : clazz.getInterfaces()) {
                try {
                    return interfaceClass.getMethod(methodName, parameterTypes);
                } catch (NoSuchMethodException ignored) {
                }
            }
            return null;
        }
    }

    /**
     * 解析参数值
     */
    private Object[] parseParameters(java.util.List<String> parameterJsonList, Class<?>[] parameterTypes) 
            throws Exception {
        Object[] parameters = new Object[parameterJsonList.size()];
        for (int i = 0; i < parameterJsonList.size(); i++) {
            String parameterJson = parameterJsonList.get(i);
            parameters[i] = objectMapper.readValue(parameterJson, parameterTypes[i]);
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

