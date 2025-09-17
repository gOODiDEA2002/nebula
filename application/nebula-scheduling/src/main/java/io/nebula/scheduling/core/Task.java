package io.nebula.scheduling.core;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 任务定义接口
 */
public interface Task {
    
    /**
     * 获取任务ID
     */
    String getTaskId();
    
    /**
     * 获取任务名称
     */
    String getTaskName();
    
    /**
     * 获取任务描述
     */
    String getDescription();
    
    /**
     * 获取任务组
     */
    String getTaskGroup();
    
    /**
     * 获取Cron表达式
     */
    String getCronExpression();
    
    /**
     * 获取任务参数
     */
    Map<String, Object> getParameters();
    
    /**
     * 是否启用
     */
    boolean isEnabled();
    
    /**
     * 获取创建时间
     */
    LocalDateTime getCreateTime();
    
    /**
     * 获取更新时间
     */
    LocalDateTime getUpdateTime();
}
