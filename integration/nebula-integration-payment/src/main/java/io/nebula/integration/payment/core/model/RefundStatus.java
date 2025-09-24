package io.nebula.integration.payment.core.model;

/**
 * 退款状态枚举
 *
 * @author nebula
 */
public enum RefundStatus {

    /**
     * 退款处理中
     */
    PROCESSING("processing", "退款处理中"),

    /**
     * 退款成功
     */
    SUCCESS("success", "退款成功"),

    /**
     * 退款失败
     */
    FAILED("failed", "退款失败"),

    /**
     * 未知状态
     */
    UNKNOWN("unknown", "未知状态");

    private final String code;
    private final String name;

    RefundStatus(String code, String name) {
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
     * 根据代码获取退款状态
     *
     * @param code 代码
     * @return 退款状态
     */
    public static RefundStatus fromCode(String code) {
        for (RefundStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return UNKNOWN;
    }

    @Override
    public String toString() {
        return code;
    }
}
