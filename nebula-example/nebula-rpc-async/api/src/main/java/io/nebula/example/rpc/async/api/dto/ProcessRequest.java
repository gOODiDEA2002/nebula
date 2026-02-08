package io.nebula.example.rpc.async.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * 数据处理请求
 * 
 * @author Nebula Framework
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 任务ID（客户端生成）
     */
    private String taskId;
    
    /**
     * 数据源标识
     */
    private String dataSource;
    
    /**
     * 处理类型
     */
    private ProcessType type;
    
    /**
     * 处理参数
     */
    private Map<String, Object> params;
    
    /**
     * 模拟延迟秒数（用于测试异步效果）
     */
    private int delaySeconds;
    
    /**
     * 备注说明
     */
    private String remark;
}
