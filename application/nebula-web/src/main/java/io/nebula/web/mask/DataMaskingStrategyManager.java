package io.nebula.web.mask;

import io.nebula.web.mask.strategies.*;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据脱敏策略管理器
 * 
 * @author nebula
 */
public class DataMaskingStrategyManager {
    
    private final Map<String, DataMaskingStrategy> strategies = new ConcurrentHashMap<>();
    
    public DataMaskingStrategyManager() {
        // 注册默认策略
        registerDefaultStrategies();
    }
    
    private void registerDefaultStrategies() {
        strategies.put(MaskType.PHONE.name(), new PhoneMaskingStrategy());
        strategies.put(MaskType.EMAIL.name(), new EmailMaskingStrategy());
        strategies.put(MaskType.ID_CARD.name(), new IdCardMaskingStrategy());
        strategies.put(MaskType.NAME.name(), new NameMaskingStrategy());
        strategies.put(MaskType.BANK_CARD.name(), new BankCardMaskingStrategy());
        strategies.put(MaskType.ADDRESS.name(), new AddressMaskingStrategy());
        strategies.put(MaskType.PASSWORD.name(), new PasswordMaskingStrategy());
        strategies.put(MaskType.IP_ADDRESS.name(), new IpAddressMaskingStrategy());
    }
    
    /**
     * 注册自定义脱敏策略
     * 
     * @param name 策略名称
     * @param strategy 策略实现
     */
    public void registerStrategy(String name, DataMaskingStrategy strategy) {
        strategies.put(name, strategy);
    }
    
    /**
     * 获取脱敏策略
     * 
     * @param type 脱敏类型
     * @param customStrategy 自定义策略名称
     * @return 脱敏策略
     */
    public DataMaskingStrategy getStrategy(MaskType type, String customStrategy) {
        if (type == MaskType.CUSTOM && StringUtils.hasText(customStrategy)) {
            return strategies.get(customStrategy);
        }
        return strategies.get(type.name());
    }
    
    /**
     * 执行数据脱敏
     * 
     * @param original 原始数据
     * @param type 脱敏类型
     * @param customStrategy 自定义策略名称
     * @return 脱敏后的数据
     */
    public String mask(String original, MaskType type, String customStrategy) {
        DataMaskingStrategy strategy = getStrategy(type, customStrategy);
        if (strategy == null) {
            return original;
        }
        return strategy.mask(original);
    }
}
