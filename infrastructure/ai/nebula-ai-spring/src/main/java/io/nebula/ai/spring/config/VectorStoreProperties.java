package io.nebula.ai.spring.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 向量存储配置属性
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Data
@ConfigurationProperties(prefix = "nebula.ai.vector-store")
public class VectorStoreProperties {
    
    /**
     * 批处理大小
     * 
     * 说明：每次向向量库添加的文档块数量
     * - 较小的值：减少内存占用，降低失败影响
     * - 较大的值：提高索引速度
     * 默认值：10
     */
    private int batchSize = 10;
    
    /**
     * 批次间延迟（毫秒）
     * 
     * 说明：批次之间的等待时间，给向量库/embedding服务恢复时间
     * 默认值：500ms
     */
    private long batchDelayMs = 500;
    
    /**
     * 最大重试次数
     * 
     * 说明：索引失败时的最大重试次数
     * 建议值：3-5 次
     * 默认值：5
     */
    private int maxRetryAttempts = 5;
    
    /**
     * 重试延迟（毫秒）
     * 
     * 说明：首次重试的延迟时间，后续重试采用指数退避策略
     * 例如：1000ms → 2000ms → 4000ms → 8000ms
     * 默认值：1000ms
     */
    private long retryDelayMs = 1000;
    
    /**
     * 是否启用批处理
     * 
     * 说明：如果为 false，将一次性添加所有文档（不推荐）
     * 默认值：true
     */
    private boolean batchingEnabled = true;
    
    /**
     * 是否启用重试
     * 
     * 说明：如果为 false，失败时不会重试
     * 默认值：true
     */
    private boolean retryEnabled = true;
}

