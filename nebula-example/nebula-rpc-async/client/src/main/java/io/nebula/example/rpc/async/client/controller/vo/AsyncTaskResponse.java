package io.nebula.example.rpc.async.client.controller.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 异步任务响应
 * 
 * @author Nebula Framework
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsyncTaskResponse {
    
    /**
     * 执行ID（用于查询状态和结果）
     */
    private String executionId;
    
    /**
     * 任务ID
     */
    private String taskId;
    
    /**
     * 当前状态
     */
    private String status;
    
    /**
     * 消息
     */
    private String message;
    
    /**
     * 状态查询URL
     */
    private String queryUrl;
    
    /**
     * 结果查询URL
     */
    private String resultUrl;
}
