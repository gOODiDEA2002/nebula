package io.nebula.integration.payment.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 支付配置属性
 *
 * @author nebula
 */
@ConfigurationProperties(prefix = "nebula.payment")
public class PaymentProperties {

    /**
     * Mock 支付配置
     */
    private MockConfig mock = new MockConfig();

    /**
     * 支付宝配置
     */
    private AlipayConfig alipay = new AlipayConfig();

    /**
     * 微信支付配置
     */
    private WechatPayConfig wechatPay = new WechatPayConfig();

    public MockConfig getMock() {
        return mock;
    }

    public void setMock(MockConfig mock) {
        this.mock = mock;
    }

    public AlipayConfig getAlipay() {
        return alipay;
    }

    public void setAlipay(AlipayConfig alipay) {
        this.alipay = alipay;
    }

    public WechatPayConfig getWechatPay() {
        return wechatPay;
    }

    public void setWechatPay(WechatPayConfig wechatPay) {
        this.wechatPay = wechatPay;
    }

    /**
     * Mock 支付配置
     */
    public static class MockConfig {
        /**
         * 是否启用 Mock 支付
         */
        private boolean enabled = true;

        /**
         * 自动支付成功的延迟时间（秒）
         */
        private int autoSuccessDelay = 60;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getAutoSuccessDelay() {
            return autoSuccessDelay;
        }

        public void setAutoSuccessDelay(int autoSuccessDelay) {
            this.autoSuccessDelay = autoSuccessDelay;
        }
    }

    /**
     * 支付宝配置
     */
    public static class AlipayConfig {
        /**
         * 是否启用支付宝
         */
        private boolean enabled = false;

        /**
         * 应用ID
         */
        private String appId;

        /**
         * 商户私钥
         */
        private String privateKey;

        /**
         * 支付宝公钥
         */
        private String publicKey;

        /**
         * 服务器地址
         */
        private String serverUrl = "https://openapi.alipay.com/gateway.do";

        /**
         * 签名类型
         */
        private String signType = "RSA2";

        /**
         * 字符编码
         */
        private String charset = "UTF-8";

        /**
         * 数据格式
         */
        private String format = "json";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }

        public String getPrivateKey() {
            return privateKey;
        }

        public void setPrivateKey(String privateKey) {
            this.privateKey = privateKey;
        }

        public String getPublicKey() {
            return publicKey;
        }

        public void setPublicKey(String publicKey) {
            this.publicKey = publicKey;
        }

        public String getServerUrl() {
            return serverUrl;
        }

        public void setServerUrl(String serverUrl) {
            this.serverUrl = serverUrl;
        }

        public String getSignType() {
            return signType;
        }

        public void setSignType(String signType) {
            this.signType = signType;
        }

        public String getCharset() {
            return charset;
        }

        public void setCharset(String charset) {
            this.charset = charset;
        }

        public String getFormat() {
            return format;
        }

        public void setFormat(String format) {
            this.format = format;
        }
    }

    /**
     * 微信支付配置
     */
    public static class WechatPayConfig {
        /**
         * 是否启用微信支付
         */
        private boolean enabled = false;

        /**
         * 应用ID
         */
        private String appId;

        /**
         * 商户号
         */
        private String mchId;

        /**
         * 商户密钥
         */
        private String mchKey;

        /**
         * 证书路径
         */
        private String certPath;

        /**
         * 服务器地址
         */
        private String serverUrl = "https://api.mch.weixin.qq.com";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }

        public String getMchId() {
            return mchId;
        }

        public void setMchId(String mchId) {
            this.mchId = mchId;
        }

        public String getMchKey() {
            return mchKey;
        }

        public void setMchKey(String mchKey) {
            this.mchKey = mchKey;
        }

        public String getCertPath() {
            return certPath;
        }

        public void setCertPath(String certPath) {
            this.certPath = certPath;
        }

        public String getServerUrl() {
            return serverUrl;
        }

        public void setServerUrl(String serverUrl) {
            this.serverUrl = serverUrl;
        }
    }
}
