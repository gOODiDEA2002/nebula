package io.nebula.rpc.async.storage;

import io.nebula.rpc.async.execution.AsyncRpcExecution;
import io.nebula.rpc.async.execution.ExecutionStatus;

/**
 * 异步执行存储接口
 * 
 * <p>支持多种存储实现：Nacos、Redis、Database等
 * 
 * @author Nebula Framework
 * @since 2.1.0
 */
public interface AsyncExecutionStorage {
    
    /**
     * 保存执行记录
     *
     * @param execution 执行记录
     */
    void save(AsyncRpcExecution execution);
    
    /**
     * 根据ID查询
     *
     * @param executionId 执行ID
     * @return 执行记录，不存在返回null
     */
    AsyncRpcExecution findById(String executionId);
    
    /**
     * 更新状态
     *
     * @param executionId 执行ID
     * @param status 新状态
     */
    void updateStatus(String executionId, ExecutionStatus status);
    
    /**
     * 更新结果
     *
     * @param executionId 执行ID
     * @param result 执行结果
     */
    void updateResult(String executionId, Object result);
    
    /**
     * 更新错误信息
     *
     * @param executionId 执行ID
     * @param error 错误
     */
    void updateError(String executionId, Throwable error);
}
