package io.nebula.rpc.grpc.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.stub.StreamObserver;
import io.nebula.rpc.core.annotation.RpcService;
import io.nebula.rpc.grpc.proto.GenericRpcServiceGrpc;
import io.nebula.rpc.grpc.proto.RpcRequest;
import io.nebula.rpc.grpc.proto.RpcResponse;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.Method;
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
            Class<?>[] interfaces = serviceBean.getClass().getInterfaces();
            
            for (Class<?> interfaceClass : interfaces) {
                String serviceName = interfaceClass.getName();
                serviceRegistry.put(serviceName, serviceBean);
                log.info("注册 gRPC RPC 服务: {}", serviceName);
            }
        }
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
            log.error("gRPC RPC 调用失败: requestId={}, service={}, method={}", 
                    requestId, request.getServiceName(), request.getMethodName(), e);

            responseBuilder
                    .setSuccess(false)
                    .setErrorCode("RPC_CALL_ERROR")
                    .setErrorMessage(e.getMessage())
                    .setStackTrace(getStackTrace(e));
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
        sb.append(e.getClass().getName()).append(": ").append(e.getMessage()).append("\n");
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
            if (sb.length() > 2000) { // 限制堆栈长度
                sb.append("\t...");
                break;
            }
        }
        return sb.toString();
    }
}

