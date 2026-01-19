package io.nebula.rpc.async.execution;

/**
 * RPC执行状态枚举
 * 
 * @author Nebula Framework
 * @since 2.1.0
 */
public enum ExecutionStatus {
    
    /**
     * 等待执行
     */
    PENDING,
    
    /**
     * 执行中
     */
    RUNNING,
    
    /**
     * 成功
     */
    SUCCESS,
    
    /**
     * 失败
     */
    FAILED,
    
    /**
     * 已取消
     */
    CANCELLED,
    
    /**
     * 超时
     */
    TIMEOUT
}
