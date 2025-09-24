package io.nebula.task.core;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 任务执行上下文
 * 封装任务执行时的相关信息
 */
public class TaskContext {
    
    private final String taskId;
    private final String taskName;
    private final String taskAlias;
    private final TaskType taskType;
    private final ExecutionMode executionMode;
    private final Map<String, Object> parameters;
    private final TaskLogger logger;
    private final LocalDateTime startTime;
    private final int logId;
    private final long logDateTime;
    
    private TaskContext(Builder builder) {
        this.taskId = builder.taskId;
        this.taskName = builder.taskName;
        this.taskAlias = builder.taskAlias;
        this.taskType = builder.taskType;
        this.executionMode = builder.executionMode;
        this.parameters = builder.parameters;
        this.logger = builder.logger;
        this.startTime = builder.startTime;
        this.logId = builder.logId;
        this.logDateTime = builder.logDateTime;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    // Getters
    public String getTaskId() { return taskId; }
    public String getTaskName() { return taskName; }
    public String getTaskAlias() { return taskAlias; }
    public TaskType getTaskType() { return taskType; }
    public ExecutionMode getExecutionMode() { return executionMode; }
    public Map<String, Object> getParameters() { return parameters; }
    public TaskLogger getLogger() { return logger; }
    public LocalDateTime getStartTime() { return startTime; }
    public int getLogId() { return logId; }
    public long getLogDateTime() { return logDateTime; }
    
    /**
     * 获取参数值
     * 
     * @param key 参数名
     * @return 参数值
     */
    public Object getParameter(String key) {
        return parameters != null ? parameters.get(key) : null;
    }
    
    /**
     * 获取参数值（带默认值）
     * 
     * @param key 参数名
     * @param defaultValue 默认值
     * @return 参数值
     */
    @SuppressWarnings("unchecked")
    public <T> T getParameter(String key, T defaultValue) {
        Object value = getParameter(key);
        return value != null ? (T) value : defaultValue;
    }
    
    /**
     * 获取字符串参数
     */
    public String getStringParameter(String key) {
        return getStringParameter(key, null);
    }
    
    /**
     * 获取字符串参数（带默认值）
     */
    public String getStringParameter(String key, String defaultValue) {
        Object value = getParameter(key);
        return value != null ? value.toString() : defaultValue;
    }
    
    /**
     * 获取整型参数
     */
    public Integer getIntParameter(String key) {
        return getIntParameter(key, null);
    }
    
    /**
     * 获取整型参数（带默认值）
     */
    public Integer getIntParameter(String key, Integer defaultValue) {
        Object value = getParameter(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
    
    /**
     * 获取布尔参数
     */
    public Boolean getBooleanParameter(String key) {
        return getBooleanParameter(key, null);
    }
    
    /**
     * 获取布尔参数（带默认值）
     */
    public Boolean getBooleanParameter(String key, Boolean defaultValue) {
        Object value = getParameter(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return defaultValue;
    }
    
    public static class Builder {
        private String taskId;
        private String taskName;
        private String taskAlias;
        private TaskType taskType;
        private ExecutionMode executionMode;
        private Map<String, Object> parameters;
        private TaskLogger logger;
        private LocalDateTime startTime;
        private int logId;
        private long logDateTime;
        
        public Builder taskId(String taskId) {
            this.taskId = taskId;
            return this;
        }
        
        public Builder taskName(String taskName) {
            this.taskName = taskName;
            return this;
        }
        
        public Builder taskAlias(String taskAlias) {
            this.taskAlias = taskAlias;
            return this;
        }
        
        public Builder taskType(TaskType taskType) {
            this.taskType = taskType;
            return this;
        }
        
        public Builder executionMode(ExecutionMode executionMode) {
            this.executionMode = executionMode;
            return this;
        }
        
        public Builder parameters(Map<String, Object> parameters) {
            this.parameters = parameters;
            return this;
        }
        
        public Builder logger(TaskLogger logger) {
            this.logger = logger;
            return this;
        }
        
        public Builder startTime(LocalDateTime startTime) {
            this.startTime = startTime;
            return this;
        }
        
        public Builder logId(int logId) {
            this.logId = logId;
            return this;
        }
        
        public Builder logDateTime(long logDateTime) {
            this.logDateTime = logDateTime;
            return this;
        }
        
        public TaskContext build() {
            if (startTime == null) {
                startTime = LocalDateTime.now();
            }
            return new TaskContext(this);
        }
    }
}
