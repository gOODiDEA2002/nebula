package io.nebula.messaging.rabbitmq.delay;

import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 批量延时消息发送结果
 *
 * @author Nebula Framework
 * @since 2.0.0
 */
@Data
public class BatchDelayMessageResult {
    
    /**
     * 所有发送结果
     */
    private final List<DelayMessageResult> results;
    
    /**
     * 总耗时（毫秒）
     */
    private final long elapsedTime;
    
    public BatchDelayMessageResult(List<DelayMessageResult> results, long elapsedTime) {
        this.results = results;
        this.elapsedTime = elapsedTime;
    }
    
    /**
     * 是否全部发送成功
     */
    public boolean isAllSuccess() {
        return results.stream().allMatch(DelayMessageResult::isSuccess);
    }
    
    /**
     * 获取成功数量
     */
    public int getSuccessCount() {
        return (int) results.stream().filter(DelayMessageResult::isSuccess).count();
    }
    
    /**
     * 获取失败数量
     */
    public int getFailedCount() {
        return (int) results.stream().filter(r -> !r.isSuccess()).count();
    }
    
    /**
     * 获取总数量
     */
    public int getTotalCount() {
        return results.size();
    }
    
    /**
     * 获取失败的发送结果
     */
    public List<DelayMessageResult> getFailedResults() {
        return results.stream()
                .filter(r -> !r.isSuccess())
                .collect(Collectors.toList());
    }
    
    /**
     * 获取成功率
     */
    public double getSuccessRate() {
        if (results.isEmpty()) {
            return 0.0;
        }
        return (double) getSuccessCount() / getTotalCount();
    }
}

