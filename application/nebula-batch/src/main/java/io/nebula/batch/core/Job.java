package io.nebula.batch.core;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 批处理作业接口
 */
public interface Job {
    
    /**
     * 获取作业ID
     */
    String getJobId();
    
    /**
     * 获取作业名称
     */
    String getJobName();
    
    /**
     * 获取作业描述
     */
    String getDescription();
    
    /**
     * 获取作业类型
     */
    String getJobType();
    
    /**
     * 获取作业参数
     */
    Map<String, Object> getParameters();
    
    /**
     * 是否可重启
     */
    boolean isRestartable();
    
    /**
     * 获取创建时间
     */
    LocalDateTime getCreateTime();
    
    /**
     * 获取更新时间
     */
    LocalDateTime getUpdateTime();
}
