package io.nebula.task.core;

/**
 * 任务执行器接口
 * 定义任务的具体执行逻辑
 */
public interface TaskExecutor {
    
    /**
     * 执行任务
     * 
     * @param context 任务执行上下文
     * @return 执行结果
     */
    TaskResult execute(TaskContext context);
    
    /**
     * 获取执行器名称
     * 用于标识执行器，也用作任务处理器的名称
     */
    String getExecutorName();
    
    /**
     * 是否支持指定的任务类型
     * 
     * @param taskType 任务类型
     * @return 是否支持
     */
    default boolean supports(TaskType taskType) {
        return true;
    }
    
    /**
     * 是否支持指定的任务名称
     * 
     * @param taskName 任务名称
     * @return 是否支持
     */
    default boolean supports(String taskName) {
        return getExecutorName().equals(taskName);
    }
    
    /**
     * 获取执行器描述
     */
    default String getDescription() {
        return "任务执行器: " + getExecutorName();
    }
    
    /**
     * 执行前的初始化操作
     * 
     * @param context 任务上下文
     */
    default void beforeExecute(TaskContext context) {
        // 默认不做任何操作
    }
    
    /**
     * 执行后的清理操作
     * 
     * @param context 任务上下文
     * @param result 执行结果
     */
    default void afterExecute(TaskContext context, TaskResult result) {
        // 默认不做任何操作
    }
    
    /**
     * 异常处理
     * 
     * @param context 任务上下文
     * @param exception 异常
     * @return 处理后的结果，如果返回null则使用默认的失败结果
     */
    default TaskResult handleException(TaskContext context, Exception exception) {
        return TaskResult.failure(context, exception);
    }
}
