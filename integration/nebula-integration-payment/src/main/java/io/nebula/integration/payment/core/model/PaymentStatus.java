package io.nebula.integration.payment.core.model;

/**
 * 支付状态枚举
 *
 * @author nebula
 */
public enum PaymentStatus {

    /**
     * 等待支付
     */
    PENDING("pending", "等待支付"),

    /**
     * 支付中
     */
    PROCESSING("processing", "支付中"),

    /**
     * 支付成功
     */
    SUCCESS("success", "支付成功"),

    /**
     * 支付失败
     */
    FAILED("failed", "支付失败"),

    /**
     * 支付取消
     */
    CANCELLED("cancelled", "支付取消"),

    /**
     * 支付超时
     */
    TIMEOUT("timeout", "支付超时"),

    /**
     * 已关闭
     */
    CLOSED("closed", "已关闭"),

    /**
     * 已退款
     */
    REFUNDED("refunded", "已退款"),

    /**
     * 部分退款
     */
    PARTIAL_REFUNDED("partial_refunded", "部分退款"),

    /**
     * 未知状态
     */
    UNKNOWN("unknown", "未知状态");

    private final String code;
    private final String name;

    PaymentStatus(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    /**
     * 根据代码获取支付状态
     *
     * @param code 代码
     * @return 支付状态
     */
    public static PaymentStatus fromCode(String code) {
        for (PaymentStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return UNKNOWN;
    }

    /**
     * 是否为最终状态（不会再变更的状态）
     *
     * @return 是否为最终状态
     */
    public boolean isFinal() {
        return this == SUCCESS || this == FAILED || this == CANCELLED || 
               this == TIMEOUT || this == CLOSED;
    }

    /**
     * 是否为成功状态
     *
     * @return 是否为成功状态
     */
    public boolean isSuccess() {
        return this == SUCCESS;
    }

    /**
     * 是否为失败状态
     *
     * @return 是否为失败状态
     */
    public boolean isFailed() {
        return this == FAILED || this == CANCELLED || this == TIMEOUT;
    }

    @Override
    public String toString() {
        return code;
    }
}
