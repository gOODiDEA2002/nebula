package io.nebula.rpc.async.execution;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * RPC异步执行记录
 * 
 * @author Nebula Framework
 * @since 2.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsyncRpcExecution implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 执行ID（UUID）
     */
    private String executionId;
    
    /**
     * RPC接口类名
     */
    private String interfaceName;
    
    /**
     * RPC方法名
     */
    private String methodName;
    
    /**
     * 执行状态
     */
    private ExecutionStatus status;
    
    /**
     * 方法参数（JSON序列化）
     */
    private String arguments;
    
    /**
     * 执行结果（JSON序列化）
     */
    private String result;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 完成时间
     */
    private LocalDateTime finishTime;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 错误堆栈
     */
    private String errorStack;
    
    /**
     * 元数据
     */
    private Map<String, String> metadata;
}
