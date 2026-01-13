package io.nebula.crawler.core.proxy;

/**
 * 代理验证器接口
 * <p>
 * 验证代理的可用性
 * </p>
 *
 * @author Nebula Team
 * @since 2.0.1
 */
public interface ProxyValidator {

    /**
     * 验证代理是否可用
     *
     * @param proxy 待验证代理
     * @return 验证结果
     */
    ValidationResult validate(Proxy proxy);
    
    /**
     * 验证结果
     */
    record ValidationResult(boolean valid, long responseTime, String message) {
        
        /**
         * 验证成功
         */
        public static ValidationResult success(long responseTime) {
            return new ValidationResult(true, responseTime, null);
        }
        
        /**
         * 验证失败
         */
        public static ValidationResult failure(String message) {
            return new ValidationResult(false, -1, message);
        }
        
        /**
         * 是否有效
         */
        public boolean isValid() {
            return valid;
        }
    }
}
