package io.nebula.task.core;

import java.time.LocalDateTime;

/**
 * 任务执行结果
 */
public class TaskResult {
    
    private final String taskId;
    private final String taskName;
    private final boolean success;
    private final String message;
    private final Object data;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final long duration;
    private final Exception exception;
    
    private TaskResult(Builder builder) {
        this.taskId = builder.taskId;
        this.taskName = builder.taskName;
        this.success = builder.success;
        this.message = builder.message;
        this.data = builder.data;
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
        this.duration = builder.duration;
        this.exception = builder.exception;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * 创建成功结果
     */
    public static TaskResult success(String taskId, String taskName) {
        return builder()
                .taskId(taskId)
                .taskName(taskName)
                .success(true)
                .message("任务执行成功")
                .build();
    }
    
    /**
     * 创建成功结果（带数据）
     */
    public static TaskResult success(String taskId, String taskName, String message, Object data) {
        return builder()
                .taskId(taskId)
                .taskName(taskName)
                .success(true)
                .message(message)
                .data(data)
                .build();
    }
    
    /**
     * 创建失败结果
     */
    public static TaskResult failure(String taskId, String taskName, String message) {
        return builder()
                .taskId(taskId)
                .taskName(taskName)
                .success(false)
                .message(message)
                .build();
    }
    
    /**
     * 创建失败结果（带异常）
     */
    public static TaskResult failure(String taskId, String taskName, Exception exception) {
        return builder()
                .taskId(taskId)
                .taskName(taskName)
                .success(false)
                .message(exception.getMessage())
                .exception(exception)
                .build();
    }
    
    /**
     * 基于上下文创建成功结果
     */
    public static TaskResult success(TaskContext context) {
        return success(context.getTaskId(), context.getTaskName());
    }
    
    /**
     * 基于上下文创建成功结果（带数据）
     */
    public static TaskResult success(TaskContext context, String message, Object data) {
        return success(context.getTaskId(), context.getTaskName(), message, data);
    }
    
    /**
     * 基于上下文创建失败结果
     */
    public static TaskResult failure(TaskContext context, String message) {
        return failure(context.getTaskId(), context.getTaskName(), message);
    }
    
    /**
     * 基于上下文创建失败结果（带异常）
     */
    public static TaskResult failure(TaskContext context, Exception exception) {
        return failure(context.getTaskId(), context.getTaskName(), exception);
    }
    
    // Getters
    public String getTaskId() { return taskId; }
    public String getTaskName() { return taskName; }
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public Object getData() { return data; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public long getDuration() { return duration; }
    public Exception getException() { return exception; }
    
    @Override
    public String toString() {
        return String.format("TaskResult{taskId='%s', taskName='%s', success=%s, message='%s', duration=%dms}", 
                taskId, taskName, success, message, duration);
    }
    
    public static class Builder {
        private String taskId;
        private String taskName;
        private boolean success;
        private String message;
        private Object data;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private long duration;
        private Exception exception;
        
        public Builder taskId(String taskId) {
            this.taskId = taskId;
            return this;
        }
        
        public Builder taskName(String taskName) {
            this.taskName = taskName;
            return this;
        }
        
        public Builder success(boolean success) {
            this.success = success;
            return this;
        }
        
        public Builder message(String message) {
            this.message = message;
            return this;
        }
        
        public Builder data(Object data) {
            this.data = data;
            return this;
        }
        
        public Builder startTime(LocalDateTime startTime) {
            this.startTime = startTime;
            return this;
        }
        
        public Builder endTime(LocalDateTime endTime) {
            this.endTime = endTime;
            return this;
        }
        
        public Builder duration(long duration) {
            this.duration = duration;
            return this;
        }
        
        public Builder exception(Exception exception) {
            this.exception = exception;
            return this;
        }
        
        public TaskResult build() {
            // 计算执行时间
            if (startTime != null && endTime != null && duration == 0) {
                duration = java.time.Duration.between(startTime, endTime).toMillis();
            }
            return new TaskResult(this);
        }
    }
}
