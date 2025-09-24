package io.nebula.integration.payment.core.model;

import lombok.Builder;
import lombok.Data;

/**
 * 通知处理结果模型
 *
 * @author nebula
 */
@Data
@Builder
public class NotificationResult {

    /**
     * 是否成功处理
     */
    private boolean success;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 响应内容（返回给支付提供商）
     */
    private String response;

    /**
     * 创建成功结果
     *
     * @param response 响应内容
     * @return 成功结果
     */
    public static NotificationResult success(String response) {
        return NotificationResult.builder()
            .success(true)
            .response(response)
            .build();
    }

    /**
     * 创建失败结果
     *
     * @param errorMessage 错误信息
     * @return 失败结果
     */
    public static NotificationResult failure(String errorMessage) {
        return NotificationResult.builder()
            .success(false)
            .errorMessage(errorMessage)
            .build();
    }
}
