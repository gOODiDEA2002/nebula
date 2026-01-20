package io.nebula.rpc.async.execution;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nebula.rpc.async.storage.AsyncExecutionStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

/**
 * 异步RPC执行管理器
 * 
 * <p>负责提交异步执行、管理执行状态、查询执行结果
 * 
 * @author Nebula Framework
 * @since 2.1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncRpcExecutionManager {
    
    private final AsyncExecutionStorage storage;
    private final Executor rpcExecutor;
    private final ObjectMapper objectMapper;
    
    /**
     * 提交异步执行
     *
     * @param interfaceClass RPC接口类
     * @param method 方法
     * @param args 参数
     * @param callable 实际执行逻辑
     * @param <T> 返回类型
     * @return 执行记录
     */
    public <T> AsyncRpcExecution submitAsync(
            Class<?> interfaceClass,
            Method method,
            Object[] args,
            Callable<T> callable) {
        
        // 1. 创建执行记录
        AsyncRpcExecution execution = createExecution(interfaceClass, method, args);
        storage.save(execution);
        
        log.info("[AsyncRpc] 提交异步执行: executionId={}, interface={}, method={}", 
                execution.getExecutionId(), interfaceClass.getSimpleName(), method.getName());
        
        // 2. 异步执行
        rpcExecutor.execute(() -> executeAsync(execution.getExecutionId(), callable));
        
        return execution;
    }
    
    /**
     * 异步执行逻辑
     */
    private <T> void executeAsync(String executionId, Callable<T> callable) {
        try {
            log.info("[AsyncRpc] 开始执行: executionId={}", executionId);
            
            // 更新为运行中
            storage.updateStatus(executionId, ExecutionStatus.RUNNING);
            
            // 执行RPC调用
            T result = callable.call();
            
            // 保存结果并更新状态（合并操作避免覆盖问题）
            updateResultAndStatus(executionId, result, ExecutionStatus.SUCCESS);
            
            log.info("[AsyncRpc] 执行成功: executionId={}", executionId);
            
        } catch (Exception e) {
            log.error("[AsyncRpc] 执行失败: executionId={}", executionId, e);
            updateErrorAndStatus(executionId, e, ExecutionStatus.FAILED);
        }
    }
    
    /**
     * 更新结果并设置状态（合并为单次操作）
     */
    private void updateResultAndStatus(String executionId, Object result, ExecutionStatus status) {
        AsyncRpcExecution execution = storage.findById(executionId);
        if (execution != null) {
            try {
                execution.setResult(objectMapper.writeValueAsString(result));
                execution.setStatus(status);
                execution.setFinishTime(java.time.LocalDateTime.now());
                storage.save(execution);
            } catch (Exception e) {
                log.error("[AsyncRpc] 更新结果和状态失败: executionId={}", executionId, e);
                throw new RuntimeException("更新结果失败", e);
            }
        }
    }
    
    /**
     * 更新错误并设置状态（合并为单次操作）
     */
    private void updateErrorAndStatus(String executionId, Throwable error, ExecutionStatus status) {
        AsyncRpcExecution execution = storage.findById(executionId);
        if (execution != null) {
            execution.setErrorMessage(error.getMessage());
            execution.setErrorStack(getStackTrace(error));
            execution.setStatus(status);
            execution.setFinishTime(java.time.LocalDateTime.now());
            storage.save(execution);
        }
    }
    
    /**
     * 获取异常堆栈
     */
    private String getStackTrace(Throwable error) {
        java.io.StringWriter sw = new java.io.StringWriter();
        error.printStackTrace(new java.io.PrintWriter(sw));
        return sw.toString();
    }
    
    /**
     * 查询执行状态
     *
     * @param executionId 执行ID
     * @return 执行记录
     */
    public AsyncRpcExecution getExecution(String executionId) {
        return storage.findById(executionId);
    }
    
    /**
     * 取消执行
     *
     * @param executionId 执行ID
     * @return 是否成功取消
     */
    public boolean cancel(String executionId) {
        AsyncRpcExecution execution = storage.findById(executionId);
        if (execution != null && execution.getStatus() == ExecutionStatus.PENDING) {
            storage.updateStatus(executionId, ExecutionStatus.CANCELLED);
            log.info("[AsyncRpc] 取消执行: executionId={}", executionId);
            return true;
        }
        return false;
    }
    
    /**
     * 创建执行记录
     */
    private AsyncRpcExecution createExecution(Class<?> interfaceClass, Method method, Object[] args) {
        String executionId = UUID.randomUUID().toString().replace("-", "");
        
        // 序列化参数
        String argsJson = null;
        try {
            argsJson = objectMapper.writeValueAsString(args);
        } catch (Exception e) {
            log.warn("[AsyncRpc] 序列化参数失败", e);
            argsJson = "[]";
        }
        
        return AsyncRpcExecution.builder()
                .executionId(executionId)
                .interfaceName(interfaceClass.getName())
                .methodName(method.getName())
                .status(ExecutionStatus.PENDING)
                .arguments(argsJson)
                .createTime(LocalDateTime.now())
                .metadata(new HashMap<>())
                .build();
    }
}
