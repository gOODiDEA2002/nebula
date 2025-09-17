package io.nebula.scheduling.core;

import java.time.LocalDateTime;

/**
 * 任务执行结果
 */
public class TaskResult {
    
    private String taskId;
    private boolean success;
    private String message;
    private Object data;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private long duration;
    private Exception exception;
    
    private TaskResult(Builder builder) {
        this.taskId = builder.taskId;
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
    
    public static TaskResult success(String taskId) {
        return builder()
                .taskId(taskId)
                .success(true)
                .message("任务执行成功")
                .build();
    }
    
    public static TaskResult success(String taskId, String message, Object data) {
        return builder()
                .taskId(taskId)
                .success(true)
                .message(message)
                .data(data)
                .build();
    }
    
    public static TaskResult failure(String taskId, String message) {
        return builder()
                .taskId(taskId)
                .success(false)
                .message(message)
                .build();
    }
    
    public static TaskResult failure(String taskId, Exception exception) {
        return builder()
                .taskId(taskId)
                .success(false)
                .message(exception.getMessage())
                .exception(exception)
                .build();
    }
    
    // Getters
    public String getTaskId() { return taskId; }
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public Object getData() { return data; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public long getDuration() { return duration; }
    public Exception getException() { return exception; }
    
    public static class Builder {
        private String taskId;
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
            return new TaskResult(this);
        }
    }
}
