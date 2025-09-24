package io.nebula.task.core;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 任务定义接口
 * 统一调度任务和批处理任务的定义
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
     * 获取任务类型
     */
    TaskType getTaskType();
    
    /**
     * 获取任务分组
     */
    default String getTaskGroup() {
        return "default";
    }
    
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
    
    // ========== 调度相关 ==========
    
    /**
     * 获取Cron表达式
     */
    default String getCronExpression() {
        return null;
    }
    
    /**
     * 是否为调度任务
     */
    default boolean isScheduledTask() {
        return getTaskType() == TaskType.SCHEDULED && getCronExpression() != null;
    }
    
    
    /**
     * 获取批处理页大小
     */
    default int getPageSize() {
        return 1000;
    }
    
    /**
     * 是否支持并行处理
     */
    default boolean isParallelEnabled() {
        return false;
    }
    
    /**
     * 获取并行度
     */
    default int getParallelism() {
        return 1;
    }
    
    // ========== 执行控制 ==========
    
    /**
     * 是否可重启
     */
    default boolean isRestartable() {
        return true;
    }
    
    /**
     * 获取超时时间（秒）
     */
    default int getTimeoutSeconds() {
        return 3600; // 默认1小时
    }
    
    /**
     * 获取重试次数
     */
    default int getRetryCount() {
        return 0;
    }
    
    /**
     * 获取重试间隔（秒）
     */
    default int getRetryIntervalSeconds() {
        return 60;
    }
}
