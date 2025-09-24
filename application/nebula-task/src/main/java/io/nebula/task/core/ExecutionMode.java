package io.nebula.task.core;

/**
 * 任务执行模式
 */
public enum ExecutionMode {
    
    /**
     * 自动执行 - 由调度器自动触发
     */
    SCHEDULED,
    
    /**
     * 手动执行 - 手动触发执行
     */
    MANUAL
}
