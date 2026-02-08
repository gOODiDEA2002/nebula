package io.nebula.example.rpc.async.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 数据处理结果
 * 
 * @author Nebula Framework
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessResult implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 任务ID
     */
    private String taskId;
    
    /**
     * 是否成功
     */
    private boolean success;
    
    /**
     * 结果消息
     */
    private String message;
    
    /**
     * 处理结果数据
     */
    private Map<String, Object> data;
    
    /**
     * 处理完成时间
     */
    private LocalDateTime processTime;
    
    /**
     * 处理耗时（毫秒）
     */
    private long durationMs;
    
    /**
     * 处理的数据条数
     */
    private int processedCount;
    
    /**
     * 创建成功结果
     */
    public static ProcessResult success(String taskId, String message) {
        return ProcessResult.builder()
                .taskId(taskId)
                .success(true)
                .message(message)
                .processTime(LocalDateTime.now())
                .build();
    }
    
    /**
     * 创建失败结果
     */
    public static ProcessResult failure(String taskId, String message) {
        return ProcessResult.builder()
                .taskId(taskId)
                .success(false)
                .message(message)
                .processTime(LocalDateTime.now())
                .build();
    }
}
