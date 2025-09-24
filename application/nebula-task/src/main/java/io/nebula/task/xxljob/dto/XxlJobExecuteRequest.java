package io.nebula.task.xxljob.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * XXL-JOB 执行请求
 */
public class XxlJobExecuteRequest {
    
    private int jobId;
    private String executorHandler;
    private String executorParams;
    private String executorBlockStrategy;
    private int executorTimeout;
    private int logId;
    private long logDateTime;
    private String glueType;
    private String glueSource;
    private long glueUpdatetime;
    private int broadcastIndex;
    private int broadcastTotal;
    
    /**
     * 解析任务参数
     * 参数格式：key1:value1|key2:value2
     */
    public Map<String, Object> parseParameters() {
        Map<String, Object> parameters = new HashMap<>();
        
        if (executorParams != null && !executorParams.trim().isEmpty()) {
            String[] params = executorParams.split("\\|");
            for (String param : params) {
                if (param != null && !param.trim().isEmpty()) {
                    String[] keyValue = param.split(":", 2);
                    if (keyValue.length == 2) {
                        parameters.put(keyValue[0].trim(), keyValue[1].trim());
                    }
                }
            }
        }
        
        return parameters;
    }
    
    // Getters and Setters
    public int getJobId() {
        return jobId;
    }
    
    public void setJobId(int jobId) {
        this.jobId = jobId;
    }
    
    public String getExecutorHandler() {
        return executorHandler;
    }
    
    public void setExecutorHandler(String executorHandler) {
        this.executorHandler = executorHandler;
    }
    
    public String getExecutorParams() {
        return executorParams;
    }
    
    public void setExecutorParams(String executorParams) {
        this.executorParams = executorParams;
    }
    
    public String getExecutorBlockStrategy() {
        return executorBlockStrategy;
    }
    
    public void setExecutorBlockStrategy(String executorBlockStrategy) {
        this.executorBlockStrategy = executorBlockStrategy;
    }
    
    public int getExecutorTimeout() {
        return executorTimeout;
    }
    
    public void setExecutorTimeout(int executorTimeout) {
        this.executorTimeout = executorTimeout;
    }
    
    public int getLogId() {
        return logId;
    }
    
    public void setLogId(int logId) {
        this.logId = logId;
    }
    
    public long getLogDateTime() {
        return logDateTime;
    }
    
    public void setLogDateTime(long logDateTime) {
        this.logDateTime = logDateTime;
    }
    
    public String getGlueType() {
        return glueType;
    }
    
    public void setGlueType(String glueType) {
        this.glueType = glueType;
    }
    
    public String getGlueSource() {
        return glueSource;
    }
    
    public void setGlueSource(String glueSource) {
        this.glueSource = glueSource;
    }
    
    public long getGlueUpdatetime() {
        return glueUpdatetime;
    }
    
    public void setGlueUpdatetime(long glueUpdatetime) {
        this.glueUpdatetime = glueUpdatetime;
    }
    
    public int getBroadcastIndex() {
        return broadcastIndex;
    }
    
    public void setBroadcastIndex(int broadcastIndex) {
        this.broadcastIndex = broadcastIndex;
    }
    
    public int getBroadcastTotal() {
        return broadcastTotal;
    }
    
    public void setBroadcastTotal(int broadcastTotal) {
        this.broadcastTotal = broadcastTotal;
    }
    
    @Override
    public String toString() {
        return String.format("XxlJobExecuteRequest{jobId=%d, executorHandler='%s', executorParams='%s', logId=%d}", 
                jobId, executorHandler, executorParams, logId);
    }
}
