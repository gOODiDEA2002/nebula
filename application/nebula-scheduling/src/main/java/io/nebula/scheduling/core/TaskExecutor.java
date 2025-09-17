package io.nebula.scheduling.core;

import java.util.Map;

/**
 * 任务执行器接口
 */
public interface TaskExecutor {
    
    /**
     * 执行任务
     * 
     * @param taskId 任务ID
     * @param parameters 任务参数
     * @return 执行结果
     */
    TaskResult execute(String taskId, Map<String, Object> parameters);
    
    /**
     * 获取执行器类型
     */
    String getExecutorType();
    
    /**
     * 是否支持指定的任务类型
     */
    boolean supports(String taskType);
}
