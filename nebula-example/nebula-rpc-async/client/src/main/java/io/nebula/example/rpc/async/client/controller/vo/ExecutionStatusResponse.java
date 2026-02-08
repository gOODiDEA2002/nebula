package io.nebula.example.rpc.async.client.controller.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 执行状态响应
 * 
 * @author Nebula Framework
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionStatusResponse {
    
    /**
     * 执行ID
     */
    private String executionId;
    
    /**
     * 执行状态: PENDING, RUNNING, SUCCESS, FAILED, CANCELLED, TIMEOUT
     */
    private String status;
    
    /**
     * 接口名称
     */
    private String interfaceName;
    
    /**
     * 方法名称
     */
    private String methodName;
    
    /**
     * 创建时间
     */
    private String createTime;
    
    /**
     * 开始时间
     */
    private String startTime;
    
    /**
     * 完成时间
     */
    private String finishTime;
    
    /**
     * 错误消息（仅当status=FAILED时有值）
     */
    private String errorMessage;
}
