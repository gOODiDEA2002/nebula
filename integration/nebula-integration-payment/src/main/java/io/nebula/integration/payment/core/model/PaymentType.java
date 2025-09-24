package io.nebula.integration.payment.core.model;

/**
 * 支付类型枚举
 *
 * @author nebula
 */
public enum PaymentType {

    /**
     * 网站支付
     */
    WEB("web", "网站支付"),

    /**
     * 手机网站支付
     */
    WAP("wap", "手机网站支付"),

    /**
     * APP支付
     */
    APP("app", "APP支付"),

    /**
     * 扫码支付
     */
    QR_CODE("qr_code", "扫码支付"),

    /**
     * 刷卡支付（被扫支付）
     */
    MICRO_PAY("micro_pay", "刷卡支付"),

    /**
     * 小程序支付
     */
    MINI_PROGRAM("mini_program", "小程序支付"),

    /**
     * 公众号支付
     */
    JSAPI("jsapi", "公众号支付"),

    /**
     * H5支付
     */
    H5("h5", "H5支付"),

    /**
     * 快捷支付
     */
    QUICK_PAY("quick_pay", "快捷支付");

    private final String code;
    private final String name;

    PaymentType(String code, String name) {
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
     * 根据代码获取支付类型
     *
     * @param code 代码
     * @return 支付类型
     */
    public static PaymentType fromCode(String code) {
        for (PaymentType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown payment type code: " + code);
    }

    @Override
    public String toString() {
        return code;
    }
}
