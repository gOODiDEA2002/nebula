package io.nebula.example.modules.messaging.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 获取消息统计信息接口DTO
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Data
@Schema(description = "获取消息统计信息接口DTO")
public class GetMessageStatsDto {
    
    /**
     * 获取消息统计信息请求
     */
    @Data
    @Schema(description = "获取消息统计信息请求")
    public static class Request {
        
        @Schema(description = "统计类型", example = "PRODUCER",
                allowableValues = {"PRODUCER", "CONSUMER", "ALL"})
        @NotBlank(message = "统计类型不能为空")
        private String statsType;
    }
    
    /**
     * 获取消息统计信息响应
     */
    @Data
    @Schema(description = "获取消息统计信息响应")
    public static class Response {
        
        @Schema(description = "生产者统计信息")
        private ProducerStatsVo producerStats;
        
        @Schema(description = "消费者统计信息")
        private ConsumerStatsVo consumerStats;
    }
    
    /**
     * 生产者统计信息VO
     */
    @Data
    @Schema(description = "生产者统计信息")
    public static class ProducerStatsVo {
        
        @Schema(description = "发送总数", example = "1000")
        private Long sentCount;
        
        @Schema(description = "发送成功数", example = "995")
        private Long successCount;
        
        @Schema(description = "发送失败数", example = "5")
        private Long failedCount;
        
        @Schema(description = "成功率", example = "0.995")
        private Double successRate;
        
        @Schema(description = "平均发送耗时（毫秒）", example = "15.5")
        private Double averageElapsedTime;
        
        @Schema(description = "统计开始时间", example = "1704672000000")
        private Long startTime;
    }
    
    /**
     * 消费者统计信息VO
     */
    @Data
    @Schema(description = "消费者统计信息")
    public static class ConsumerStatsVo {
        
        @Schema(description = "消费总数", example = "950")
        private Long consumedCount;
        
        @Schema(description = "消费成功数", example = "940")
        private Long successCount;
        
        @Schema(description = "消费失败数", example = "10")
        private Long failedCount;
        
        @Schema(description = "成功率", example = "0.989")
        private Double successRate;
        
        @Schema(description = "平均消费耗时（毫秒）", example = "25.3")
        private Double averageElapsedTime;
        
        @Schema(description = "正在处理的消息数", example = "5")
        private Integer processingCount;
        
        @Schema(description = "统计开始时间", example = "1704672000000")
        private Long startTime;
    }
}

