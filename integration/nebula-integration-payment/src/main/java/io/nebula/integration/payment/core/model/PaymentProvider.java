package io.nebula.integration.payment.core.model;

/**
 * 支付提供商枚举
 *
 * @author nebula
 */
public enum PaymentProvider {

    /**
     * 支付宝
     */
    ALIPAY("alipay", "支付宝"),

    /**
     * 微信支付
     */
    WECHAT_PAY("wechat_pay", "微信支付"),

    /**
     * 银联支付
     */
    UNION_PAY("union_pay", "银联支付"),

    /**
     * PayPal
     */
    PAYPAL("paypal", "PayPal"),

    /**
     * Stripe
     */
    STRIPE("stripe", "Stripe"),

    /**
     * 测试支付（用于开发和测试）
     */
    MOCK("mock", "测试支付");

    private final String code;
    private final String name;

    PaymentProvider(String code, String name) {
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
     * 根据代码获取支付提供商
     *
     * @param code 代码
     * @return 支付提供商
     */
    public static PaymentProvider fromCode(String code) {
        for (PaymentProvider provider : values()) {
            if (provider.code.equals(code)) {
                return provider;
            }
        }
        throw new IllegalArgumentException("Unknown payment provider code: " + code);
    }

    @Override
    public String toString() {
        return code;
    }
}
