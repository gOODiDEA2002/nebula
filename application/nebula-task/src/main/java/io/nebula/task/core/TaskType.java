package io.nebula.task.core;

/**
 * 任务类型枚举
 */
public enum TaskType {
    
    /**
     * 调度任务 - 基于 Cron 表达式定时执行
     */
    SCHEDULED,
    
    /**
     * 手动任务 - 仅支持手动触发执行
     */
    MANUAL
}
