package io.nebula.rpc.async.execution;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 异步RPC调用结果
 * 
 * <p>包含执行ID，用于后续查询执行状态
 * 
 * @param <T> 实际结果类型
 * @author Nebula Framework
 * @since 2.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsyncRpcResult<T> implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 执行ID
     */
    private String executionId;
    
    /**
     * 执行状态
     */
    private ExecutionStatus status;
    
    /**
     * 创建时间
     */
    private String createTime;
    
    /**
     * 实际结果（仅当status=SUCCESS时有值）
     */
    private T result;
    
    /**
     * 错误信息（仅当status=FAILED时有值）
     */
    private String errorMessage;
    
    /**
     * 创建Pending状态的结果
     */
    public static <T> AsyncRpcResult<T> pending(String executionId) {
        return AsyncRpcResult.<T>builder()
                .executionId(executionId)
                .status(ExecutionStatus.PENDING)
                .createTime(java.time.LocalDateTime.now().toString())
                .build();
    }
    
    /**
     * 创建成功状态的结果
     */
    public static <T> AsyncRpcResult<T> success(String executionId, T result) {
        return AsyncRpcResult.<T>builder()
                .executionId(executionId)
                .status(ExecutionStatus.SUCCESS)
                .result(result)
                .build();
    }
    
    /**
     * 创建失败状态的结果
     */
    public static <T> AsyncRpcResult<T> failed(String executionId, String errorMessage) {
        return AsyncRpcResult.<T>builder()
                .executionId(executionId)
                .status(ExecutionStatus.FAILED)
                .errorMessage(errorMessage)
                .build();
    }
}
