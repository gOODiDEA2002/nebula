package io.nebula.core.config;

/**
 * 配置属性接口
 * 所有配置类都应该实现此接口
 */
public interface ConfigurationProperties {
    
    /**
     * 获取配置前缀
     * 
     * @return 配置前缀
     */
    String prefix();
    
    /**
     * 验证配置
     * 在配置绑定后调用，用于进行自定义验证
     * 
     * @throws io.nebula.core.common.exception.ValidationException 验证失败时抛出
     */
    default void validate() {
        // 默认不进行额外验证
    }
    
    /**
     * 配置初始化后的回调
     * 在验证通过后调用，可用于初始化派生属性
     */
    default void afterPropertiesSet() {
        // 默认不进行额外处理
    }
    
    /**
     * 获取配置描述
     * 
     * @return 配置描述
     */
    default String description() {
        return "Configuration for " + prefix();
    }
    
    /**
     * 是否启用此配置
     * 
     * @return 是否启用
     */
    default boolean isEnabled() {
        return true;
    }
}
